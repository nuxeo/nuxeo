/*
 * (C) Copyright 2016-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.marklogic.MarkLogicHelper.ElementType;

/**
 * MarkLogic Deserializer to convert {@link String} into {@link State}.
 *
 * @since 8.3
 */
final class MarkLogicStateDeserializer {

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    static {
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    }

    private MarkLogicStateDeserializer() {
        // nothing
    }

    public static State deserialize(String s) {
        try (InputStream is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))) {
            return deserialize(is);
        } catch (IOException ioe) {
            throw new NuxeoException("Error during deserialization", ioe);
        }
    }

    public static State deserialize(InputStream is) {
        XMLEventReader xmler = null;
        try {
            xmler = xmlInputFactory.createXMLEventReader(is);
            XMLEvent event;
            while (xmler.hasNext()) {
                event = xmler.nextEvent();
                if (event.isStartElement()) {
                    return deserializeState(xmler);
                }
            }
        } catch (XMLStreamException e) {
            // TODO change that
            throw new RuntimeException(e);
        } finally {
            if (xmler != null) {
                try {
                    xmler.close();
                } catch (XMLStreamException e) {
                    // TODO change that
                    throw new RuntimeException(e);
                }
            }
        }
        throw new NuxeoException("An error occurred during xml deserialization.");
    }

    private static State deserializeState(XMLEventReader xmler) throws XMLStreamException {
        State state = new State();
        XMLEvent event;
        while (xmler.hasNext()) {
            event = xmler.nextEvent();
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                state.put(MarkLogicHelper.deserializeKey(startElement.getName().getLocalPart()),
                        deserializeValue(xmler, startElement));
            } else if (event.isEndElement()) {
                break;
            }
        }
        return state;
    }

    private static Serializable deserializeValue(XMLEventReader xmler, StartElement element) throws XMLStreamException {
        // Here previous event was a start element (element parameter), several possible cases for next event:
        // - start element for a sub state or list
        // - character for an element value
        // - character for a whitespace between tag
        Serializable result;
        Optional<ElementType> typeOpt = getElementType(element);
        if (typeOpt.isPresent()) {
            String text = xmler.peek().isEndElement() ? "" : xmler.nextEvent().asCharacters().getData();
            switch (typeOpt.get()) {
            case BOOLEAN:
                result = Boolean.parseBoolean(text);
                break;
            case DOUBLE:
                // Due to MarkLogic issue on replace+apply on number we need to handle xs:double type for Delta
                if (text.contains(".")) {
                    result = Double.parseDouble(text);
                } else {
                    result = Long.parseLong(text);
                }
                break;
            case LONG:
                result = Long.parseLong(text);
                break;
            case CALENDAR:
                result = MarkLogicHelper.deserializeCalendar(text);
                break;
            case STRING:
            default:
                result = text;
                break;
            }
            // consume end element event
            XMLEvent event = xmler.nextEvent();
            if (event.isCharacters() && event.asCharacters().isIgnorableWhiteSpace()) {
                xmler.nextEvent();
            }
        } else {
            // Remove whitespace event
            if (xmler.peek().isCharacters() && xmler.peek().asCharacters().isWhiteSpace()) {
                xmler.nextEvent();
            }
            XMLEvent event = xmler.peek();
            if (event.isEndElement()) {
                result = null;
                xmler.nextEvent();
            } else if (event.asStartElement()
                            .getName()
                            .getLocalPart()
                            .endsWith(MarkLogicHelper.ARRAY_ITEM_KEY_SUFFIX)) {
                result = deserializeList(xmler);
            } else {
                result = deserializeState(xmler);
            }
        }
        return result;
    }

    private static Serializable deserializeList(XMLEventReader xmler) throws XMLStreamException {
        Serializable result;
        // try to retrieve element type
        Optional<ElementType> type = getElementType(xmler.peek().asStartElement());
        if (type.isPresent()) {
            List<Object> l = new ArrayList<>();
            while (xmler.hasNext()) {
                XMLEvent event = xmler.nextEvent();
                if (event.isStartElement()) {
                    l.add(deserializeValue(xmler, event.asStartElement()));
                } else if (event.isEndElement()) {
                    // end of list
                    break;
                }
            }
            Class<?> scalarType = scalarTypeToSerializableClass(type.get(), l.get(0).toString());
            result = l.toArray((Object[]) Array.newInstance(scalarType, l.size()));
        } else {
            ArrayList<Serializable> l = new ArrayList<>();
            while (xmler.hasNext()) {
                XMLEvent event = xmler.nextEvent();
                if (event.isStartElement()) {
                    l.add(deserializeState(xmler));
                } else if (event.isEndElement()) {
                    // end of list
                    break;
                }
            }
            result = l;
        }
        return result;
    }

    private static Optional<ElementType> getElementType(StartElement element) {
        return Optional.ofNullable(
                element.getAttributeByName(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type", "xsi")))
                       .map(Attribute::getValue)
                       .map(ElementType::of);
    }

    private static Class<?> scalarTypeToSerializableClass(ElementType type, String content) {
        Class<?> result;
        switch (type) {
        case BOOLEAN:
            result = Boolean.class;
            break;
        case DOUBLE:
            // Due to MarkLogic issue on replace+apply on number we need to handle xs:double type for Delta
            if (content.contains(".")) {
                result = Double.class;
            } else {
                result = Long.class;
            }
            break;
        case LONG:
            result = Long.class;
            break;
        case CALENDAR:
            result = Calendar.class;
            break;
        case STRING:
        default:
            result = String.class;
            break;
        }
        return result;
    }

}
