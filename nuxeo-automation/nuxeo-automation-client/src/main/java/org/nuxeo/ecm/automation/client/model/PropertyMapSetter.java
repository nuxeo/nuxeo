/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.model;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @since 5.7 Delegates data injection into PropertyMap objects. Keeping dirty
 *        properties in memory.
 */
public class PropertyMapSetter {

    protected final LinkedHashMap<String, Object> map;

    protected final Set<String> dirties = new HashSet<String>();

    public PropertyMapSetter(PropertyMap propertyMap) {
        map = propertyMap.map;
    }

    /**
     * @since 5.7 This method fetch all dirty properties that has been defined.
     *        Warning: Dirty properties set is flushed when getting it.
     * @return PropertyMap
     */
    public PropertyMap getDirties() {
        try {
            Map<String, Object> resultMap = new HashMap<String, Object>();
            for (String key : dirties) {
                Object value = map.get(key);
                resultMap.put(key, value);
            }
            return new PropertyMap(resultMap);
        } finally {
            dirties.clear();
        }
    }

    public void set(String key, String value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value);
        dirties.add(key);
    }

    public void set(String key, Boolean value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value.toString());
        dirties.add(key);
    }

    public void set(String key, Long value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value.toString());
        dirties.add(key);
    }

    public void set(String key, Double value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value.toString());
        dirties.add(key);
    }

    public void set(String key, Date value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, DateUtils.formatDate(value));
        dirties.add(key);
    }

    public void set(String key, PropertyList value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value);
        dirties.add(key);
    }

    public void set(String key, PropertyMap value) {
        if (value == null) {
            map.remove(key);
        }
        map.put(key, value);
        dirties.add(key);
    }

}
