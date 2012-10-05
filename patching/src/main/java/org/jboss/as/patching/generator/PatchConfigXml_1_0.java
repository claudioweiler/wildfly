/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.patching.generator;

import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.patching.metadata.Patch;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for the 1.0 version of the patch-config xsd
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class PatchConfigXml_1_0 implements XMLStreamConstants, XMLElementReader<PatchConfigBuilder> {

    enum Element {

        ADDED_BUNDLE("added-bundle"),
        ADDED_MISC_CONTENT("added-misc-content"),
        ADDED_MODULE("added-module"),
        BUNDLES("bundles"),
        CUMULATIVE("cumulative"),
        DESCRIPTION("description"),
        GENERATE_BY_DIFF("generate-by-diff"),
        IN_RUNTIME_USE("in-runtime-use"),
        MISC_FILES("misc-files"),
        MODULES("modules"),
        NAME("name"),
        ONE_OFF("one-off"),
        PATCH_CONFIG("patch-config"),
        REMOVED_BUNDLE("removed-bundle"),
        REMOVED_MISC_CONTENT("removed-misc-content"),
        REMOVED_MODULE("removed-module"),
        SPECIFIED_CONTENT("specified-content"),
        UPDATED_BUNDLE("updated-bundle"),
        UPDATED_MISC_CONTENT("updated-misc-content"),
        UPDATED_MODULE("updated-module"),

        // default unknown element
        UNKNOWN(null),
        ;

        final String name;
        Element(String name) {
            this.name = name;
        }

        static Map<String, Element> elements = new HashMap<String, Element>();
        static {
            for(Element element : Element.values()) {
                if(element != UNKNOWN) {
                    elements.put(element.name, element);
                }
            }
        }

        static Element forName(String name) {
            final Element element = elements.get(name);
            return element == null ? UNKNOWN : element;
        }

    }

    enum Attribute {

        APPLIES_TO_VERSION("applies-to-version"),
        DIRECTORY("directory"),
        EXISTING_PATH("existing-path"),
        IN_RUNTIME_USE("in-runtime-use"),
        NAME("name"),
        PATH("path"),
        RESULTING_VERSION("resulting-version"),
        SLOT("slot"),

        // default unknown attribute
        UNKNOWN(null),
        ;

        private final String name;
        Attribute(String name) {
            this.name = name;
        }

        static Map<String, Attribute> attributes = new HashMap<String, Attribute>();
        static {
            for(Attribute attribute : Attribute.values()) {
                if(attribute != UNKNOWN) {
                    attributes.put(attribute.name, attribute);
                }
            }
        }

        static Attribute forName(String name) {
            final Attribute attribute = attributes.get(name);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NAME:
                    patchConfigBuilder.setPatchId(reader.getElementText());
                    break;
                case DESCRIPTION:
                    patchConfigBuilder.setDescription(reader.getElementText());
                    break;
                case CUMULATIVE:
                    parsePatchType(reader, Patch.PatchType.CUMULATIVE, patchConfigBuilder);
                    break;
                case ONE_OFF:
                    parsePatchType(reader, Patch.PatchType.ONE_OFF, patchConfigBuilder);
                    break;
                case GENERATE_BY_DIFF:
                    parseGenerateByDiff(reader, patchConfigBuilder);
                    break;
                case SPECIFIED_CONTENT:
                    parseSpecifiedContent(reader, patchConfigBuilder);
                    break;
                case MODULES:
                    parseModules(reader, patchConfigBuilder);
                    break;
                case BUNDLES:
                    parseBundles(reader, patchConfigBuilder);
                    break;
                case MISC_FILES:
                    parseMiscFiles(reader, patchConfigBuilder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private static void parsePatchType(final XMLExtendedStreamReader reader, final Patch.PatchType type, final PatchConfigBuilder builder) throws XMLStreamException {
        //
        builder.setPatchType(type);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case APPLIES_TO_VERSION:
                    builder.addAppliesTo(value);
                    break;
                case RESULTING_VERSION:
                    if(type == Patch.PatchType.CUMULATIVE) {
                        builder.setResultingVersion(value);
                        break;
                    }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
    }

    private static void parseGenerateByDiff(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        patchConfigBuilder.setGenerateByDiff(true);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case IN_RUNTIME_USE:
                    parseInRuntimeUse(reader, patchConfigBuilder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private static void parseInRuntimeUse(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        String path = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATH:
                    path = value;
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }

        if (path == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.PATH.name));
        }

        requireNoContent(reader);

        patchConfigBuilder.addRuntimeUseItem(DistributionContentItem.createMiscItemForPath(path));
    }

    private static void parseSpecifiedContent(XMLExtendedStreamReader reader, PatchConfigBuilder patchConfigBuilder) throws XMLStreamException {

        patchConfigBuilder.setGenerateByDiff(false);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case MODULES:
                    parseModules(reader, patchConfigBuilder);
                    break;
                case BUNDLES:
                    parseBundles(reader, patchConfigBuilder);
                    break;
                case MISC_FILES:
                    parseMiscFiles(reader, patchConfigBuilder);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private static void parseModules(final XMLExtendedStreamReader reader, final PatchConfigBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED_MODULE:
                    // TODO
                case UPDATED_MODULE:
                    // TODO
                case REMOVED_MODULE:
                    // TODO
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private static void parseMiscFiles(final XMLExtendedStreamReader reader, final PatchConfigBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED_MISC_CONTENT:
                    // TODO
                case UPDATED_MISC_CONTENT:
                    // TODO
                case REMOVED_MISC_CONTENT:
                    // TODO
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private static void parseBundles(final XMLExtendedStreamReader reader, final PatchConfigBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADDED_BUNDLE:
                    // TODO
                case UPDATED_BUNDLE:
                    // TODO
                case REMOVED_BUNDLE:
                    // TODO
                default:
                    throw unexpectedElement(reader);
            }
        }
    }
}
