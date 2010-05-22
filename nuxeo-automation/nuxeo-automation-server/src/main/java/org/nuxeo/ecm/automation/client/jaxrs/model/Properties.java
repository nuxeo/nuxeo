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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.automation.client.jaxrs.spi.DateUtils;


/**
 * A flat representation of a document properties.
 * Dates are in YYYY-MM-DDThh:mm:ssZ (UTC) format
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Properties {

    protected Map<String,String> map;

    public Properties() {
        map = new HashMap<String, String>();
    }

    public Properties(Properties props) {
        map = new HashMap<String, String>(props.map);
    }

    public Properties(Map<String, String> map) {
        map = new HashMap<String, String>(map);
    }

    public Properties(int size) {
        map = new HashMap<String, String>(size);
    }

    public String getString(String key) {
        return map.get(key);
    }

    public Long getLong(String key) {
        return Long.valueOf(map.get(key));
    }

    public Double getDouble(String key) {
        return Double.valueOf(map.get(key));
    }

    public Date getDate(String key) {
        String value = map.get(key);
        return value != null ? DateUtils.parseDate(value) : null;
    }

    public String getString(String key, String defValue) {
        String v = getString(key);
        return v == null ? defValue : v;
    }

    public Long getLong(String key, Long defValue) {
        Long v = getLong(key);
        return v == null ? defValue : v;
    }

    public Double getDouble(String key, Double defValue) {
        Double v = getDouble(key);
        return v == null ? defValue : v;
    }

    public Date getDate(String key, Date defValue) {
        Date v = getDate(key);
        return v == null ? defValue : v;
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

    public void set(String key, Long value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value == null ? null : value.toString());
    }

    public void set(String key, Double value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value == null ? null : value.toString());
    }

    public void set(String key, Properties value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value == null ? null : value.toString());
    }

    public void set(String key, Date value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value == null ? null : DateUtils.formatDate(value));
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String,String> entry : map.entrySet()) {
            buf.append(entry.getKey()).append("=").append(entry.getValue()).append("\n"); //TODO escape \n in value
        }
        return buf.toString();
    }
}
