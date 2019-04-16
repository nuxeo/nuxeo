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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.xml.XMLConstants;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.joda.time.DateTime;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.marklogic.MarkLogicHelper.ElementType;

/**
 * MarkLogic Serializer to convert {@link State} into {@link String}.
 *
 * @since 8.3
 */
final class MarkLogicStateSerializer {

    private MarkLogicStateSerializer() {
        // nothing
    }

    public static String serialize(State state) {
        // Serialize root
        Element root = serialize(MarkLogicHelper.DOCUMENT_ROOT, state);
        // Add namespaces
        addDefaultNamespaces(root);
        // Create document
        return DocumentHelper.createDocument(root).asXML();
    }

    private static Element serialize(String key, State state) {
        boolean update = state instanceof StateDiff;
        Element element = DocumentHelper.createElement(MarkLogicHelper.serializeKey(key));
        for (Entry<String, Serializable> entry : state.entrySet()) {
            Optional<Element> child = serialize(entry.getKey(), entry.getValue());
            if (child.isPresent()) {
                element.add(child.get());
            } else if (update) {
                element.add(createNullElement(entry.getKey()));
            }
        }
        return element;
    }

    private static Optional<Element> serialize(String key, Object value) {
        Optional<Element> result;
        if (value == null) {
            result = Optional.empty();
        } else if (value instanceof State) {
            State state = (State) value;
            if (state.isEmpty()) {
                result = Optional.empty();
            } else {
                result = Optional.of(serialize(key, state));
            }
        } else if (value instanceof ListDiff) {
            result = Optional.of(serialize(key, (ListDiff) value));
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) value;
            if (values.isEmpty()) {
                result = Optional.empty();
            } else {
                result = Optional.of(serialize(key, values));
            }
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            if (array.length == 0) {
                result = Optional.empty();
            } else {
                result = Optional.of(serialize(key, Arrays.asList(array)));
            }
        } else {
            String nodeValue = serializeValue(value);
            Element element = DocumentHelper.createElement(MarkLogicHelper.serializeKey(key));
            element.addAttribute(MarkLogicHelper.ATTRIBUTE_XSI_TYPE, ElementType.getType(value).get());
            element.setText(nodeValue);
            result = Optional.of(element);
        }
        return result;
    }

    private static Element serialize(String key, ListDiff listDiff) {
        Element diffParent = DocumentHelper.createElement(MarkLogicHelper.serializeKey(key));

        // diff serialization
        Element diff = DocumentHelper.createElement(MarkLogicHelper.serializeKey("diff"));
        if (listDiff.diff != null) {
            for (Object object : listDiff.diff) {
                diff.add(serialize(key + MarkLogicHelper.ARRAY_ITEM_KEY_SUFFIX, object).orElseGet(
                        () -> createNullElement(key + MarkLogicHelper.ARRAY_ITEM_KEY_SUFFIX)));
            }
        }
        diffParent.add(diff);

        // rpush serialization
        List<Object> rpush = listDiff.rpush;
        if (rpush == null) {
            rpush = Collections.emptyList();
        }
        diffParent.add(serialize("rpush", key + MarkLogicHelper.ARRAY_ITEM_KEY_SUFFIX, rpush));
        return diffParent;
    }

    private static Element serialize(String key, List<Object> list) {
        return serialize(key, key + MarkLogicHelper.ARRAY_ITEM_KEY_SUFFIX, list);
    }

    private static Element serialize(String key, String itemKey, List<Object> list) {
        Element array = DocumentHelper.createElement(MarkLogicHelper.serializeKey(key));
        for (Object object : list) {
            serialize(itemKey, object).ifPresent(array::add);
        }
        return array;
    }

    public static String serializeValue(Object value) {
        String serializedValue;
        if (value instanceof Calendar) {
            serializedValue = MarkLogicHelper.serializeCalendar((Calendar) value);
        } else if (value instanceof DateTime) {
            serializedValue = ((DateTime) value).toString(MarkLogicHelper.DATE_TIME_FORMATTER);
        } else {
            serializedValue = value.toString();
        }
        return serializedValue;
    }

    private static void addDefaultNamespaces(Element root) {
        root.addNamespace("xs", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        root.addNamespace("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
    }

    private static Element createNullElement(String key) {
        Element element = DocumentHelper.createElement(MarkLogicHelper.serializeKey(key));
        element.setText("NULL");
        return element;
    }

}
