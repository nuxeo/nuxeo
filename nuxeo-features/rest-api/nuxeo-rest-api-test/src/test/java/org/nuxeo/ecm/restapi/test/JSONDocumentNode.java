/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Simple test object to ease manipulation off json document streams
 *
 * @since 5.7.2
 */
public class JSONDocumentNode {

    public ObjectNode node;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * @since 11.1
     */
    public JSONDocumentNode(String content) throws IOException {
        node = (ObjectNode) mapper.readTree(content);
    }

    /**
     * @param in
     * @throws IOException
     */
    public JSONDocumentNode(InputStream in) throws IOException {
        node = (ObjectNode) mapper.readTree(in);
    }

    /**
     * Update a property value on the JSON object
     *
     * @param key
     * @param value
     * @since 5.7.2
     */
    public void setPropertyValue(String key, String value) {
        ObjectNode on = (ObjectNode) node.findValue("properties");
        on.put(key, value);
        node.put("properties", on);
    }

    /**
     * Put a json array a a property.
     *
     * @param key
     * @param values
     * @since 5.9.2
     */
    public void setPropertyArray(String key, String... values) {
        consumePropertiesNode(on -> {
            ArrayNode array = on.putArray(key);
            for (String value : values) {
                array.add(value);
            }
        });
    }

    /**
     * Removes a property value on the JSON object.
     *
     * @since 11.1
     */
    public void removePropertyValue(String key) {
        consumePropertiesNode(on -> on.remove(key));
    }

    protected void consumePropertiesNode(Consumer<ObjectNode> consumer) {
        ObjectNode on = (ObjectNode) node.get("properties");
        consumer.accept(on);
        node.put("properties", on);
    }

    /**
     * Return the object as JSON
     *
     * @return
     * @throws IOException
     * @since 5.7.2
     */
    public String asJson() throws IOException {
        return mapper.writeValueAsString(node);
    }
}
