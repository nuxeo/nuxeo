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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Simple test object to ease manipulation off json document streams
 *
 * @since 5.7.2
 */
public class JSONDocumentNode {

    public ObjectNode node;

    private ObjectMapper mapper = new ObjectMapper();

    public JSONDocumentNode(InputStream in) throws IOException {
        node = (ObjectNode) mapper.readTree(in);

    }

    /**
     * Update a string property value on the JSON object.
     *
     * @since 5.7.2
     */
    public void setPropertyValue(String key, String value) {
        ObjectNode on = (ObjectNode) node.findValue("properties");
        on.put(key, value);
        node.set("properties", on);

    }

    /**
     * Update a property value on the JSON object.
     *
     * @since 10.3
     */
    public void setPropertyValue(String key, JsonNode jsonNode) {
        ObjectNode on = (ObjectNode) node.findValue("properties");
        on.set(key, jsonNode);
        node.set("properties", on);
    }

    /**
     * Put a json array a a property.
     *
     * @since 5.9.2
     */
    public void setPropertyArray(String key, String... values) {
        ObjectNode on = (ObjectNode) node.findValue("properties");
        ArrayNode array = on.putArray(key);
        for (String value : values) {
            array.add(value);
        }
        node.set("properties", on);
    }

    /**
     * Returns a property as a {@link JsonNode} object.
     *
     * @since 10.3
     */
    public JsonNode getPropertyAsJsonNode(String key) {
        ObjectNode on = (ObjectNode) node.findValue("properties");
        return on.get(key);
    }

    /**
     * Return the object as JSON.
     *
     * @since 5.7.2
     */
    public String asJson() throws IOException {
        return mapper.writeValueAsString(node);
    }
}
