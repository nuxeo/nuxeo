/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.model;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;



/**
 * A flat representation of a document properties.
 * Dates are in YYYY-MM-DDThh:mm:ssZ (UTC) format
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PropertyMap {

    protected LinkedHashMap<String,Object> map;

    public PropertyMap() {
        map = new LinkedHashMap<String, Object>();
    }

    public PropertyMap(PropertyMap props) {
        map = new LinkedHashMap<String, Object>(props.map);
    }

    public PropertyMap(Map<String, String> map) {
        map = new LinkedHashMap<String, String>(map);
    }

    public PropertyMap(int size) {
        map = new LinkedHashMap<String, Object>(size);
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

    public void set(String key, String value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value);
    }

    public void set(String key, Boolean value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value.toString());
    }

    public void set(String key, Long value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value.toString());
    }

    public void set(String key, Double value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value.toString());
    }

    public void set(String key, Date value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, DateUtils.formatDate(value));
    }

    public void set(String key, PropertyList value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value);
    }

    public void set(String key, PropertyMap value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value);
    }

    public Map<String,Object> map() {
        return map;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String,Object> entry : map.entrySet()) {
            Object v = entry.getValue();
            if (v != null) {
                if (v.getClass() == String.class) {
                    buf.append(entry.getKey()).append("=").append(entry.getValue()).append("\n"); //TODO escape \n in value
                } else {
                    //TODO - use full xpath
                    //buf.append(entry.getKey()).append("=").append(entry.getValue()).append("\n"); //TODO escape \n in value
                }
            } else {
                buf.append(entry.getKey()).append("=").append("\n");
            }
        }
        return buf.toString();
    }
}
