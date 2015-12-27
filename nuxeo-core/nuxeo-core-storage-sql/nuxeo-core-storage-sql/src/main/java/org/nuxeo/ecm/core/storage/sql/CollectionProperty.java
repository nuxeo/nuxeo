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
 * A {@link CollectionProperty} gives access to a collection value stored in an underlying {@link Fragment}.
 *
 * @author Florent Guillaume
 */
public class CollectionProperty extends BaseProperty {

    /** The {@link Fragment} holding the information. */
    private final Fragment fragment;

    /** The key in the row if it is a SimpleFragment otherwise null */
    private final String key;

    /**
     * Creates a {@link CollectionProperty}.
     */
    public CollectionProperty(String name, PropertyType type, boolean readonly, CollectionFragment fragment) {
        super(name, type, readonly);
        this.fragment = fragment;
        this.key = null;
    }

    /**
     * Creates a {@link CollectionProperty}.
     */
    public CollectionProperty(String name, PropertyType type, boolean readonly, SimpleFragment fragment, String key) {
        super(name, type, readonly);
        this.fragment = fragment;
        this.key = key;
    }

    // ----- getters -----

    public Serializable[] getValue() {
        Serializable[] value = null;
        if (hasCollectionFragment()) {
            value = ((CollectionFragment) fragment).get();
        } else {
            value = (Serializable[]) ((SimpleFragment) fragment).get(key);
            if (value == null) {
                value = type.getEmptyArray();
            }
        }
        return value;
    }

    public String[] getStrings() {
        switch (type) {
        case ARRAY_STRING:
            Serializable[] res = getValue();
            if (res.length == 0) {
                // special case because we may have an empty Serializable[]
                res = new String[0];
            }
            return (String[]) res;
        default:
            throw new UnsupportedOperationException("Not implemented: " + type);
        }
    }

    // ----- setters -----

    public void setValue(Object[] value) {
        checkWritable();
        try {
            if (hasCollectionFragment()) {
                ((CollectionFragment) fragment).set(type.normalize(value));
            } else {
                ((SimpleFragment) fragment).put(key, type.normalize(value));
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("item of list property '" + name + "': " + e.getMessage());
        }
        // mark fragment dirty!
    }

    private boolean hasCollectionFragment() {
        return key == null;
    }

}
