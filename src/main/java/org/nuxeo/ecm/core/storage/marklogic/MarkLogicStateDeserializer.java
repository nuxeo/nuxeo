/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.marklogic.MarkLogicHelper.ElementType;

/**
 * MarkLogic Deserializer to convert {@link String} into {@link State}.
 *
 * @since 8.3
 */
final class MarkLogicStateDeserializer {

    private MarkLogicStateDeserializer() {
        // nothing
    }

    public static State deserialize(String s) {
        try {
            Document document = DocumentHelper.parseText(s);
            return deserializeState(document.getRootElement());
        } catch (DocumentException e) {
            // TODO change that
            throw new RuntimeException(e);
        }
    }

    private static State deserializeState(Element parent) {
        State state = new State(parent.nodeCount());
        Iterator elements = parent.elementIterator();
        while (elements.hasNext()) {
            Element element = (Element) elements.next();
            state.put(MarkLogicHelper.deserializeKey(element.getQualifiedName()), deserializeValue(element));
        }
        return state;
    }

    private static Serializable deserializeValue(Element element) {
        Serializable result;
        Iterator children = element.elementIterator();
        if (children.hasNext()) {
            Element first = (Element) children.next();
            if (MarkLogicHelper.ARRAY_ITEM_KEY.equals(first.getQualifiedName())) {
                result = deserializeList(element);
            } else {
                result = deserializeState(element);
            }
        } else {
            ElementType type = getElementType(element)
            // fallback on String
            .orElse(ElementType.STRING);
            switch (type) {
            case BOOLEAN:
                result = Boolean.parseBoolean(element.getText());
                break;
            case LONG:
                result = Long.parseLong(element.getText());
                break;
            case CALENDAR:
                result = MarkLogicHelper.deserializeCalendar(element.getText());
                break;
            case STRING:
            default:
                result = element.getText();
                if (element.attribute(MarkLogicHelper.ATTRIBUTE_TYPE) == null && "".equals(result)) {
                    // element is not xs:string type, so it's an empty list
                    result = new Object[] {};
                }
                break;
            }
        }
        return result;
    }

    private static Serializable deserializeList(Element array) {
        Serializable result;
        List items = array.elements();
        if (items.isEmpty()) {
            result = null;
        } else {
            Element first = (Element) items.get(0);
            Optional<ElementType> type = getElementType(first);
            if (first.elements().isEmpty() && type.isPresent()) {
                List<Object> l = new ArrayList<>(items.size());
                for (Object element : items) {
                    l.add(deserializeValue((Element) element));
                }
                Class<?> scalarType = scalarTypeToSerializableClass(type.get());
                result = l.toArray((Object[]) Array.newInstance(scalarType, l.size()));
            } else {
                ArrayList<Serializable> l = new ArrayList<>(items.size());
                for (Object element : items) {
                    l.add(deserializeState((Element) element));
                }
                result = l;
            }
        }
        return result;
    }

    private static Optional<ElementType> getElementType(Element element) {
        return Optional.ofNullable(element.attributeValue(MarkLogicHelper.ATTRIBUTE_TYPE)).map(ElementType::of);
    }

    private static Class<?> scalarTypeToSerializableClass(ElementType type) {
        Class<?> result;
        switch (type) {
        case BOOLEAN:
            result = Boolean.class;
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
