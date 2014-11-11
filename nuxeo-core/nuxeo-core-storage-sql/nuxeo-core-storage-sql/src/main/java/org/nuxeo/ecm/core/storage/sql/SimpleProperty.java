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
 * A SimpleProperty gives access to a scalar value stored in an underlying
 * {@link SimpleFragment}.
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
    public SimpleProperty(String name, PropertyType type, boolean readonly,
            SimpleFragment fragment, String key) {
        super(name, type, readonly);
        this.fragment = fragment;
        this.key = key;
    }

    // ----- getters -----

    public Serializable getValue() throws StorageException {
        return fragment.get(key);
    }

    public String getString() throws StorageException {
        switch (type) {
        case STRING:
            return (String) fragment.get(key);
        default:
            throw new RuntimeException("Not a String property: " + type);
        }
    }

    public Long getLong() throws StorageException {
        switch (type) {
        case LONG:
            return (Long) fragment.get(key);
        default:
            throw new RuntimeException("Not a Long property: " + type);
        }
    }

    // ----- setters -----

    public void setValue(Serializable value) throws StorageException {
        checkWritable();
        fragment.put(key, type.normalize(value));
        // mark fragment dirty!
    }

}
