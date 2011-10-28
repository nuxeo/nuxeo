/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.automation.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleType;

/**
 * Helper to handle Complex types decoding from a JSON encoded String entries of
 * a property file
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 *
 */
public class ComplexTypeJSONDecoder {

    protected static List<JSONBlobDecoder> blobDecoders = new ArrayList<JSONBlobDecoder>();

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        blobDecoders.add(new JSONStringBlobDecoder());
    }

    public static void registerBlobDecoder(JSONBlobDecoder blobDecoder) {
        blobDecoders.add(blobDecoder);
    }

    public static List<Object> decodeList(ListType lt, String json)
            throws Exception {
        ArrayNode jsonArray = (ArrayNode) mapper.readTree(json);
        return decodeList(lt, jsonArray);
    }

    public static List<Object> decodeList(ListType lt, ArrayNode jsonArray) {
        List<Object> result = new ArrayList<Object>();

        ComplexType ct = (ComplexType) lt.getFieldType();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonNode node = jsonArray.get(i);
            if (node.isArray()) {
                result.add(decodeList((ListType) ct, (ArrayNode) node));
            } else if (node.isObject()) {
                result.add(decode(ct, (ObjectNode) node));
            }
        }
        return result;
    }

    public static Object decode(ComplexType ct, String json) throws Exception {
        ObjectNode jsonObject = (ObjectNode) mapper.readTree(json);
        return decode(ct, jsonObject);
    }

    public static Object decode(ComplexType ct, ObjectNode jsonObject) {

        Map<String, Object> result = new HashMap<String, Object>();

        String jsonType = "";
        if (jsonObject.has("type")) {
            jsonType = jsonObject.get("type").getTextValue();
        }
        if (jsonType.equals("blob") || ct.getName().equals("content")) {
            return getBlobFromJSON(jsonObject);
        }

        Iterator<Map.Entry<String, JsonNode>> it = jsonObject.getFields();

        while (it.hasNext()) {
            Map.Entry<String, JsonNode> nodeEntry = it.next();
            if (ct.hasField(nodeEntry.getKey())) {

                Field field = ct.getField(nodeEntry.getKey());
                if (field.getType().isSimpleType()) {
                    result.put(
                            nodeEntry.getKey(),
                            ((SimpleType) field.getType()).decode(nodeEntry.getValue().getValueAsText()));
                } else {
                    JsonNode subNode = nodeEntry.getValue();
                    if (subNode.isArray()) {
                        result.put(nodeEntry.getKey(), decodeList(
                                ((ListType) field.getType()),
                                (ArrayNode) subNode));
                    } else {
                        result.put(nodeEntry.getKey(), decode(
                                ((ComplexType) field.getType()),
                                (ObjectNode) subNode));
                    }
                }
            }
        }

        return result;
    }

    protected static Blob getBlobFromJSON(ObjectNode jsonObject) {
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
