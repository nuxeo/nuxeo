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
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.xml.XMLConstants;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.joda.time.DateTime;
import org.nuxeo.ecm.core.storage.State;
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
        Element element = DocumentHelper.createElement(MarkLogicHelper.serializeKey(key));
        for (Entry<String, Serializable> entry : state.entrySet()) {
            serialize(entry.getKey(), entry.getValue()).ifPresent(element::add);
        }
        return element;
    }

    public static Optional<Element> serialize(String key, Object value) {
        Optional<Element> result;
        if (value == null) {
            result = Optional.empty();
        } else if (value instanceof State) {
            result = Optional.of(serialize(key, (State) value));
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) value;
            result = Optional.of(serialize(key, values));
        } else if (value instanceof Object[]) {
            result = Optional.of(serialize(key, Arrays.asList((Object[]) value)));
        } else {
            String nodeValue;
            if (value instanceof Calendar) {
                nodeValue = MarkLogicHelper.serializeCalendar((Calendar) value);
            } else if (value instanceof DateTime) {
                nodeValue = ((DateTime) value).toString(MarkLogicHelper.DATE_TIME_FORMATTER);
            } else {
                nodeValue = value.toString();
            }
            Element element = DocumentHelper.createElement(MarkLogicHelper.serializeKey(key));
            element.addAttribute(MarkLogicHelper.ATTRIBUTE_XSI_TYPE, ElementType.getType(value.getClass()).getKey());
            element.setText(nodeValue);
            result = Optional.of(element);
        }
        return result;
    }

    private static Element serialize(String key, List<Object> list) {
        Element array = DocumentHelper.createElement(MarkLogicHelper.serializeKey(key));
        for (Object object : list) {
            serialize(MarkLogicHelper.ARRAY_ITEM_KEY, object).ifPresent(array::add);
        }
        return array;
    }

    public static void addDefaultNamespaces(Element root) {
        root.addNamespace("xs", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        root.addNamespace("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
    }

}
