/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A {@link CollectionProperty} gives access to a collection value stored in an
 * underlying {@link Fragment}.
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
    public CollectionProperty(String name, PropertyType type, boolean readonly,
            CollectionFragment fragment) {
        super(name, type, readonly);
        this.fragment = fragment;
        this.key = null;
    }

    /**
     * Creates a {@link CollectionProperty}.
     */
    public CollectionProperty(String name, PropertyType type, boolean readonly,
            SimpleFragment fragment, String key) {
        super(name, type, readonly);
        this.fragment = fragment;
        this.key = key;
    }

    // ----- getters -----

    public Serializable[] getValue() throws StorageException {
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

    public String[] getStrings() throws StorageException {
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

    public void setValue(Object[] value) throws StorageException {
        checkWritable();
        try {
            if (hasCollectionFragment()) {
                ((CollectionFragment) fragment).set(type.normalize(value));
            } else {
                ((SimpleFragment) fragment).put(key, type.normalize(value));
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("item of list property '" +
                    name + "': " + e.getMessage());
        }
        // mark fragment dirty!
    }
    
    private boolean hasCollectionFragment() {
        return key == null;
    }

}
