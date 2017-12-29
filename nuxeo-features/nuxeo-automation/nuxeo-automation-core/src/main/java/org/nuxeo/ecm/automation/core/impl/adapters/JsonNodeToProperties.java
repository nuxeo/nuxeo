/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonNodeToProperties implements TypeAdapter {

    @Override
    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt) throws TypeAdaptException {

        JsonNode json = (JsonNode) objectToAdapt;
        Map<String, String> map = new HashMap<String, String>();

        Iterator<Entry<String, JsonNode>> it = json.fields();
        while (it.hasNext()) {
            Entry<String, JsonNode> entry = it.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isArray()) {
                int size = value.size();
                if (size == 0) {
                    map.put(key, null);
                } else if (size == 1) {
                    map.put(key, value.get(0).asText());
                } else {
                    StringBuilder buf = new StringBuilder(size * 32);
                    buf.append(value.get(0).asText());
                    for (int i = 1; i < size; i++) {
                        buf.append(',').append(value.get(i).asText());
                    }
                    map.put(key, buf.toString());
                }
            } else {
                if (value.isTextual()) {
                    map.put(key, value.textValue());
                } else {
                    map.put(key, value.toString());
                }
            }
        }
        return new Properties(map);
    }

}
