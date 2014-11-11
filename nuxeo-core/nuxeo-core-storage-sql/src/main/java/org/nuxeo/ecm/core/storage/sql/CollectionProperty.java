/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
            throw new RuntimeException("Not implemented: " + type);
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
