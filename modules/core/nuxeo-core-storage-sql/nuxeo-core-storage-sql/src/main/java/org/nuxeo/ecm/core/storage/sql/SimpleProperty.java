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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;

/**
 * A SimpleProperty gives access to a scalar value stored in an underlying {@link SimpleFragment}.
 *
 * @author Florent Guillaume
 */
public class SimpleProperty extends BaseProperty {

    /** The {@link SimpleFragment} holding the information. */
    private final SimpleFragment fragment;

    /** The key in the dataRow */
    private final String key;

    /**
     * Creates a SimpleProperty, with specific info about row and key.
     */
    public SimpleProperty(String name, PropertyType type, boolean readonly, SimpleFragment fragment, String key) {
        super(name, type, readonly);
        this.fragment = fragment;
        this.key = key;
    }

    // ----- getters -----

    public Serializable getValue() {
        return fragment.get(key);
    }

    public String getString() {
        switch (type) {
        case STRING:
        case BINARY:
            return (String) fragment.get(key);
        default:
            throw new RuntimeException("Not a String property: " + type);
        }
    }

    public Long getLong() {
        switch (type) {
        case LONG:
            return (Long) fragment.get(key);
        default:
            throw new RuntimeException("Not a Long property: " + type);
        }
    }

    // ----- setters -----

    public void setValue(Object value) {
        checkWritable();
        fragment.put(key, type.normalize(value));
        // mark fragment dirty!
    }

}
