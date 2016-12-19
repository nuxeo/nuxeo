/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A flat representation of a document properties. Dates are in YYYY-MM-DDThh:mm:ssZ (UTC) format
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PropertyMap implements Serializable {

    private static final long serialVersionUID = -3260084599278006841L;

    protected final LinkedHashMap<String, Object> map;

    public PropertyMap() {
        map = new LinkedHashMap<>();
    }

    public PropertyMap(PropertyMap props) {
        map = new LinkedHashMap<>(props.map);
    }

    public PropertyMap(Map<String, Object> map) {
        this.map = new LinkedHashMap<>(map);
    }

    public PropertyMap(int size) {
        map = new LinkedHashMap<>(size);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    public Long getLong(String key) {
        return getLong(key, null);
    }

    public Double getDouble(String key) {
        return getDouble(key, null);
    }

    public Date getDate(String key) {
        return getDate(key, null);
    }

    public PropertyList getList(String key) {
        return getList(key, null);
    }

    public PropertyMap getMap(String key) {
        return getMap(key, null);
    }

    public String getString(String key, String defValue) {
        return PropertiesHelper.getString(map.get(key), defValue);
    }

    public Boolean getBoolean(String key, Boolean defValue) {
        return PropertiesHelper.getBoolean(map.get(key), defValue);
    }

    public Long getLong(String key, Long defValue) {
        return PropertiesHelper.getLong(map.get(key), defValue);
    }

    public Double getDouble(String key, Double defValue) {
        return PropertiesHelper.getDouble(map.get(key), defValue);
    }

    public Date getDate(String key, Date defValue) {
        return PropertiesHelper.getDate(map.get(key), defValue);
    }

    public PropertyList getList(String key, PropertyList defValue) {
        return PropertiesHelper.getList(map.get(key), defValue);
    }

    public PropertyMap getMap(String key, PropertyMap defValue) {
        return PropertiesHelper.getMap(map.get(key), defValue);
    }

    public Object get(String key) {
        return map.get(key);
    }

    public Set<String> getKeys() {
        return map.keySet();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Map<String, Object> map() {
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object v = entry.getValue();
            if (v != null) {
                if (v.getClass() == String.class) {
                    buf.append(entry.getKey()).append("=").append(entry.getValue()).append("\n"); // TODO escape \n
                                                                                                  // in value
                } else {
                    // TODO - use full xpath
                    // buf.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
                    // //TODO escape \n in value
                }
            } else {
                buf.append(entry.getKey()).append("=").append("\n");
            }
        }
        return buf.toString();
    }
}
