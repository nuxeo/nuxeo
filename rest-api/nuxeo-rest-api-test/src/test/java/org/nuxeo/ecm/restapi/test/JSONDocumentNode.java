/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Simple test object to ease manipulation off json document streams
 *
 * @since 5.7.2
 */
public class JSONDocumentNode {

    private ObjectNode node;

    private ObjectMapper mapper = new ObjectMapper();

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
     * @param key
     * @param values
     *
     * @since 5.9.2
     */
    public void setPropertyArray(String key, String... values) {
        ObjectNode on = (ObjectNode) node.findValue("properties");
        ArrayNode array = on.putArray(key);
        for(String value : values) {
            array.add(value);
        }
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
