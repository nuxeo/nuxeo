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
 * underlying {@link CollectionFragment}.
 *
 * @author Florent Guillaume
 */
public class CollectionProperty extends BaseProperty {

    /** The {@link CollectionFragment} holding the information. */
    private final CollectionFragment fragment;

    /**
     * Creates a {@link CollectionProperty}.
     */
    public CollectionProperty(String name, PropertyType type, boolean readonly,
            CollectionFragment fragment) {
        super(name, type, readonly);
        this.fragment = fragment;
    }

    // ----- getters -----

    public Serializable[] getValue() throws StorageException {
        return fragment.get();
    }

    public String[] getStrings() throws StorageException {
        switch (type) {
        case ARRAY_STRING:
            Serializable[] res = fragment.get();
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
            fragment.set(type.normalize(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("item of list property '" +
                    name + "': " + e.getMessage());
        }
        // mark fragment dirty!
    }

}
