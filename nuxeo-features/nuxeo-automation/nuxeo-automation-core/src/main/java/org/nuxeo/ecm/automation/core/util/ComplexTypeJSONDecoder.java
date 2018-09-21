/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.automation.core.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper to handle Complex types decoding from a JSON encoded String entries of a property file
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
public class ComplexTypeJSONDecoder {

    private static final ObjectMapper mapper = new ObjectMapper();

    protected static List<JSONBlobDecoder> blobDecoders = new ArrayList<>();
    static {
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        blobDecoders.add(new JSONStringBlobDecoder());
        blobDecoders.add(new JSONObjectBlobDecoder());
        blobDecoders.add(new JSONManagedBlobDecoder());
    }

    public static void registerBlobDecoder(JSONBlobDecoder blobDecoder) {
        blobDecoders.add(blobDecoder);
    }

    public static List<Object> decodeList(ListType lt, String json) throws IOException {
        ArrayNode jsonArray = (ArrayNode) mapper.readTree(json);
        return decodeList(lt, jsonArray);
    }

    public static List<Object> decodeList(ListType lt, ArrayNode jsonArray) {
        List<Object> result = new ArrayList<Object>();
        Type currentObjectType = lt.getFieldType();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonNode node = jsonArray.get(i);
            if (node.isArray()) {
                result.add(decodeList((ListType) currentObjectType, (ArrayNode) node));
            } else if (node.isObject()) {
                result.add(decode((ComplexType) currentObjectType, (ObjectNode) node));
            } else if (node.isTextual()) {
                result.add(node.textValue());
            } else if (node.isNumber()) {
                result.add(node.numberValue());
            } else if (node.isBoolean()) {
                result.add(node.booleanValue());
            }
        }
        return result;
    }

    public static Object decode(ComplexType ct, String json) throws IOException {
        ObjectNode jsonObject = (ObjectNode) mapper.readTree(json);
        return decode(ct, jsonObject);
    }

    public static Object decode(ComplexType ct, ObjectNode jsonObject) {

        Map<String, Object> result = new HashMap<String, Object>();

        String jsonType = "";
        if (jsonObject.has("type")) {
            jsonType = jsonObject.get("type").textValue();
        }
        if (jsonType.equals("blob") || ct.getName().equals("content")) {
            return getBlobFromJSON(jsonObject);
        }

        Iterator<Map.Entry<String, JsonNode>> it = jsonObject.fields();

        while (it.hasNext()) {
            Map.Entry<String, JsonNode> nodeEntry = it.next();
            if (ct.hasField(nodeEntry.getKey())) {

                Field field = ct.getField(nodeEntry.getKey());
                Type fieldType = field.getType();
                if (fieldType.isSimpleType()) {
                    Object value;
                    if (fieldType == DateType.INSTANCE && nodeEntry.getValue().isIntegralNumber()) {
                        value = Calendar.getInstance();
                        ((Calendar) value).setTimeInMillis(nodeEntry.getValue().asLong());
                    } else {
                        value = ((SimpleType) fieldType).decode(nodeEntry.getValue().asText());
                    }
                    result.put(nodeEntry.getKey(), value);
                } else {
                    JsonNode subNode = nodeEntry.getValue();
                    if (subNode.isArray()) {
                        result.put(nodeEntry.getKey(), decodeList(((ListType) fieldType), (ArrayNode) subNode));
                    } else {
                        result.put(nodeEntry.getKey(), decode(((ComplexType) fieldType), (ObjectNode) subNode));
                    }
                }
            }
        }

        return result;
    }

    public static Blob getBlobFromJSON(ObjectNode jsonObject) {
        Blob blob = null;
        for (JSONBlobDecoder blobDecoder : blobDecoders) {
            blob = blobDecoder.getBlobFromJSON(jsonObject);
            if (blob != null) {
                return blob;
            }
        }
        return blob;
    }

}
