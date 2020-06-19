/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.documentation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Helper for XML secure content management.
 *
 * @since 11.2
 */
public class SecureXMLHelper {

    protected static final String KEYWORDS_PROPERTY = "org.nuxeo.apidoc.secure.xml.keywords";

    public static final List<String> DEFAULT_KEYWORDS = List.of("password", "Password", "secret", "apiKey");

    protected static final String WHITELISTED_KEYWORDS_PROPERTY = "org.nuxeo.apidoc.secure.xml.keywords.whitelisted";

    public static final List<String> DEFAULT_WHITELISTED_KEYWORDS = List.of("passwordField", "passwordHashAlgorithm");

    protected static final String SECRET_VALUE = "********";

    protected static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    protected static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    protected static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();

    /**
     * Makes sure no passwords and similar sensitive data are embedded in the XML.
     */
    public static String secure(String xml) {
        List<String> keywords = getKeywords();
        if (!isChangeNeeded(xml, keywords)) {
            return xml;
        }
        List<String> whitelist = getWhitelistedKeywords();
        try {
            return secureStAX(xml, keywords, whitelist);
        } catch (XMLStreamException e) {
            return secureRegexp(xml, keywords, whitelist);
        }
    }

    protected static boolean isChangeNeeded(String xml, List<String> keywords) {
        if (StringUtils.isBlank(xml) || keywords.stream().noneMatch(xml::contains)) {
            return false;
        }
        return true;
    }

    public static List<String> getKeywords() {
        return getKeywordList(KEYWORDS_PROPERTY, DEFAULT_KEYWORDS);
    }

    public static List<String> getWhitelistedKeywords() {
        return getKeywordList(WHITELISTED_KEYWORDS_PROPERTY, DEFAULT_WHITELISTED_KEYWORDS);
    }

    protected static List<String> getKeywordList(String property, List<String> defaultValue) {
        return Framework.getService(ConfigurationService.class)
                        .getString(property)
                        .map(v -> v.split("\\s*,[,\\s]*"))
                        .map(List::of)
                        .orElse(defaultValue);
    }

    public static String secureStAX(String xml, List<String> keywords, List<String> whitelist)
            throws XMLStreamException {
        if (!isChangeNeeded(xml, keywords)) {
            return xml;
        }
        try (InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            XMLEventReader reader = INPUT_FACTORY.createXMLEventReader(stream);
            XMLEventWriter writer = OUTPUT_FACTORY.createXMLEventWriter(output);

            boolean skipContent = false;
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isEndElement() && skipContent) {
                    skipContent = false;
                    continue;
                }
                if (skipContent) {
                    continue;
                }
                if (event.isStartElement()) {
                    StartElement el = event.asStartElement();
                    String name = el.getName().getLocalPart();
                    if (matches(name, keywords, whitelist)) {
                        // skip attributes
                        writer.add(EVENT_FACTORY.createStartElement(el.getName(), null, null));
                        // replace content if any
                        replaceTagContent(reader, writer);
                        writer.add(EVENT_FACTORY.createEndElement(el.getName(), null));
                        skipContent = true;
                        continue;
                    }
                    // first pass on attributes to assess content
                    List<String> attrNameMatches = new ArrayList<>();
                    List<String> attrValueMatches = new ArrayList<>();
                    Iterator<Attribute> attrIt = el.getAttributes();
                    while (attrIt.hasNext()) {
                        Attribute attr = attrIt.next();
                        String attrName = attr.getName().getLocalPart();
                        String value = attr.getValue();
                        if (matches(attrName, keywords, whitelist)) {
                            attrNameMatches.add(attrName);
                        } else if (matches(value, keywords, whitelist)) {
                            attrValueMatches.add(attrName);
                        }
                    }
                    if (attrNameMatches.isEmpty() && attrValueMatches.isEmpty()) {
                        writer.add(event);
                        continue;
                    }
                    // second pass to replace content
                    writer.add(EVENT_FACTORY.createStartElement(el.getName(), null, null));
                    attrIt = el.getAttributes();
                    while (attrIt.hasNext()) {
                        Attribute attr = attrIt.next();
                        String attrName = attr.getName().getLocalPart();
                        String value = attr.getValue();
                        if (!attrValueMatches.isEmpty()) {
                            // replace all attributes value except the one matching
                            if (attrValueMatches.contains(attrName)) {
                                writer.add(EVENT_FACTORY.createAttribute(attrName, value));
                            } else {
                                writer.add(EVENT_FACTORY.createAttribute(attrName, SECRET_VALUE));
                            }
                        } else if (attrNameMatches.contains(attrName)) {
                            writer.add(EVENT_FACTORY.createAttribute(attrName, SECRET_VALUE));
                        } else {
                            writer.add(EVENT_FACTORY.createAttribute(attrName, value));
                        }
                    }
                    // replace content if any
                    replaceTagContent(reader, writer);
                    writer.add(EVENT_FACTORY.createEndElement(el.getName(), null));
                    skipContent = true;
                } else if (event.isStartDocument()) {
                    if (((StartDocument) event).getVersion() == null) {
                        // skip it, it's been added by the reader processing
                        continue;
                    } else {
                        writer.add(event);
                        writer.add(EVENT_FACTORY.createCharacters("\n"));
                    }
                } else {
                    writer.add(event);
                }
            }
            writer.flush();
            writer.close();

            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    protected static void replaceTagContent(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException {
        String data = "";
        String comments = "";
        XMLEvent peek = reader.peek();
        while (peek != null
                && (XMLEvent.CHARACTERS == peek.getEventType() || XMLEvent.COMMENT == peek.getEventType())) {
            XMLEvent nextEvent = reader.nextEvent();
            if (XMLEvent.CHARACTERS == nextEvent.getEventType()) {
                data += nextEvent.asCharacters().getData();
            }
            if (XMLEvent.COMMENT == nextEvent.getEventType()) {
                String c = nextEvent.toString();
                comments += c.substring(4, c.length() - 3);
            }
            peek = reader.peek();
        }
        if (StringUtils.isNotBlank(data) || StringUtils.isNotBlank(comments)) {
            if (StringUtils.isNotBlank(comments)) {
                writer.add(EVENT_FACTORY.createCharacters("\n  "));
                writer.add(EVENT_FACTORY.createComment(comments));
                writer.add(EVENT_FACTORY.createCharacters("\n  " + SECRET_VALUE + "\n"));
            } else {
                writer.add(EVENT_FACTORY.createCharacters(SECRET_VALUE));
            }
        }
    }

    protected static boolean matches(String name, List<String> keywords, List<String> whitelist) {
        if (!whitelist.contains(name) && (keywords.stream().anyMatch(kw -> name.startsWith(kw))
                || keywords.stream().anyMatch(kw -> name.endsWith(kw)))) {
            return true;
        }
        return false;
    }

    public static String secureRegexp(String xml, List<String> keywords, List<String> whitelist) {
        if (!isChangeNeeded(xml, keywords)) {
            return xml;
        }
        String res = xml;
        for (String kw : keywords) {
            if (res.contains(kw)) {
                for (String pattern : List.of(
                        // node startswith
                        String.format("(?<start><(?<key>\\w*%s)\\s*>)[^<]*(?<end></\\w*%s>)", kw, kw),
                        // node endswith
                        String.format("(?<start><(?<key>%s\\w*)\\s*>)[^<]*(?<end></%s\\w*>)", kw, kw),
                        // attributes startswith
                        String.format("(?<start>(?<key>\\w*%s)=\")[^\"]*(?<end>\")", kw),
                        String.format("(?<start>(?<key>\\w*%s)=')[^']*(?<end>')", kw),
                        String.format("(?<start>(?<key>\\w*%s)\"\\s*>)[^<]*(?<end><)", kw),
                        String.format("(?<start>(?<key>\\w*%s)'\\s*>)[^<]*(?<end><)", kw),
                        // attributes endswith
                        String.format("(?<start>(?<key>%s\\w*)=\")[^\"]*(?<end>\")", kw),
                        String.format("(?<start>(?<key>%s\\w*)=')[^']*(?<end>\")", kw),
                        String.format("(?<start>(?<key>%s\\w*)\"\\s*>)[^<]*(?<end><)", kw),
                        String.format("(?<start>(?<key>%s\\w*)'\\s*>)[^<]*(?<end><)", kw))) {
                    StringBuffer out = new StringBuffer();
                    Matcher m = Pattern.compile(pattern).matcher(res);
                    while (m.find()) {
                        String replacement;
                        if (whitelist.contains(m.group("key"))) {
                            replacement = m.group();
                        } else {
                            replacement = m.group("start") + SECRET_VALUE + m.group("end");
                        }
                        m.appendReplacement(out, replacement);
                    }
                    res = m.appendTail(out).toString();
                }
            }
        }
        return res;
    }

}
