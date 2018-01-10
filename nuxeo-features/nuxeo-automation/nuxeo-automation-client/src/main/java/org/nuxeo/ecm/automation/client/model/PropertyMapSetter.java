/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Stéphane Lacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.client.model;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @since 5.7 Delegate containing data injection for PropertyMap object. Keeping dirty properties in memory.
 */
public class PropertyMapSetter {

    protected final Map<String, Object> map;

    protected final Set<String> dirties = new HashSet<String>();

    public PropertyMapSetter(PropertyMap propertyMap) {
        map = propertyMap.map;
    }

    /**
     * @since 5.7 This method fetch all dirty properties that has been defined. Warning: Dirty properties are not
     *        flushed when getting it.
     * @return PropertyMap
     */
    public PropertyMap getDirties() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        for (String key : dirties) {
            Object value = map.get(key);
            resultMap.put(key, value);
        }
        return new PropertyMap(resultMap);
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
        } else {
            map.put(key, value.toString());
        }
        dirties.add(key);
    }

    public void set(String key, Long value) {
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value.toString());
        }
        dirties.add(key);
    }

    public void set(String key, Double value) {
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value.toString());
        }
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
