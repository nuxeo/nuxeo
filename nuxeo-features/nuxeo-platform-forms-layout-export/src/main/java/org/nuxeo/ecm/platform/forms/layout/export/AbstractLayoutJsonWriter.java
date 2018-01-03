/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.platform.forms.layout.export;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.MapUtils;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.1
 */
public abstract class AbstractLayoutJsonWriter<EntityType> extends AbstractJsonWriter<EntityType> {

    protected SortedMap<String, Map<String, Serializable>> cleanAndSort(Map<String, Map<String, Serializable>> map) {
        SortedMap<String, Map<String, Serializable>> sortedMap  = new TreeMap<>();
        for (Entry<String, Map<String, Serializable>> entry : map.entrySet()) {
            if (MapUtils.isNotEmpty(entry.getValue())) {
                sortedMap.put(entry.getKey(), new TreeMap<>(entry.getValue()));
            }
        }
        return sortedMap;
    }

    /**
     * Writes a map whose values are a map.
     *
     * @param fieldName The name of the Json field in which the maps will be written.
     * @param map The map to write.
     * @param jg The {@link JsonGenerator} used to write the given map.
     */
    protected void writeSerializableMapMapField(String fieldName, Map<String, Map<String, Serializable>> map,
            JsonGenerator jg) throws IOException {
        jg.writeObjectFieldStart(fieldName);
        for (Entry<String, Map<String, Serializable>> entry : map.entrySet()) {
            writeSerializableMapField(entry.getKey(), entry.getValue(), jg);
        }
        jg.writeEndObject();
    }

}
