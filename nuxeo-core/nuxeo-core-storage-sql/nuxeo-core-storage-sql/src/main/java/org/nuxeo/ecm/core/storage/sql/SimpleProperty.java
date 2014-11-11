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
 * A {@link SimpleProperty} gives access to a scalar value stored in an
 * underlying {@link SimpleFragment}.
 *
 * @author Florent Guillaume
 */
public class SimpleProperty extends BaseProperty {

    /** The {@link SimpleFragment} holding the information. */
    private final SimpleFragment row;

    /** The key in the dataRow */
    private final String key;

    /**
     * Creates a {@link SimpleProperty}, with specific info about row and key.
     */
    public SimpleProperty(String name, PropertyType type, boolean readonly,
            SimpleFragment row, String key) {
        super(name, type, readonly);
        this.row = row;
        this.key = key;
    }

    // ----- getters -----

    public Serializable getValue() throws StorageException {
        return row.get(key);
    }

    public String getString() throws StorageException {
        switch (type) {
        case STRING:
            return (String) row.get(key);
        default:
            throw new RuntimeException("Not a String property: " + type);
        }
    }

    public Long getLong() throws StorageException {
        switch (type) {
        case LONG:
            return (Long) row.get(key);
        default:
            throw new RuntimeException("Not a Long property: " + type);
        }
    }

    // ----- setters -----

    public void setValue(Serializable value) throws StorageException {
        checkWritable();
        row.put(key, type.normalize(value));
        // mark fragment dirty!
    }

}
