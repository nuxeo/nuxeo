/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.bulk.io;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.nuxeo.ecm.core.api.NuxeoException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 10.3
 */
public class BulkParameters {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private BulkParameters() {
        // utility class
    }

    public static Map<String, Serializable> paramsToMap(String jsonParams) throws IOException {
        return paramsToMap(OBJECT_MAPPER.readTree(jsonParams));
    }

    public static HashMap<String, Serializable> paramsToMap(JsonNode node) {
        // we declare variable and method return type as HashMap to be compliant with Serializable in params map
        HashMap<String, Serializable> params = new HashMap<>();
        Iterable<Map.Entry<String, JsonNode>> paramNodes = node::fields;
        for (Map.Entry<String, JsonNode> paramNode : paramNodes) {
            params.put(paramNode.getKey(), toSerializable(paramNode.getValue()));
        }
        return params;
    }

    protected static ArrayList<Serializable> toList(JsonNode value) {
        // we declare method return type as ArrayList to be compliant with Serializable in params map
        // spliterator calls iterator which is a bridge method of JsonNode#elements
        return StreamSupport.stream(value.spliterator(), false)
                            .map(BulkParameters::toSerializable)
                            .collect(Collectors.toCollection(ArrayList::new));
    }

    protected static Serializable toSerializable(JsonNode value) {
        Serializable serializableValue;
        switch (value.getNodeType()) {
        case STRING:
        case BINARY: // binary will be converted to base64
            serializableValue = value.asText();
            break;
        case BOOLEAN:
            serializableValue = value.asBoolean();
            break;
        case NUMBER:
            serializableValue = value.asLong();
            break;
        case ARRAY:
            serializableValue = toList(value);
            break;
        case OBJECT:
            serializableValue = paramsToMap(value);
            break;
        default:
            throw new NuxeoException("Node type=" + value.getNodeType() + " is not supported");
        }
        return serializableValue;
    }
}
