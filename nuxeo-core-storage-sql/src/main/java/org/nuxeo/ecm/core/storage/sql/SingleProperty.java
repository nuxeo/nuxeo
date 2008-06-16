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
 * A {@link SingleProperty} gives access to a scalar value stored in an
 * underlying {@link SingleRow}.
 *
 * @author Florent Guillaume
 */
public class SingleProperty extends AbstractProperty {

    /** The {@link SingleRow} holding the information. */
    private final SingleRow row;

    /** The key in the dataRow */
    private final String key;

    /**
     * Creates a {@link SingleProperty}, with specific info about row and key.
     */
    public SingleProperty(String name, PropertyType type, boolean readonly,
            SingleRow row, String key) {
        super(name, type, readonly);
        this.row = row;
        this.key = key;
    }

    // ----- getters -----

    public String getString() {
        switch (type) {
        case STRING:
            return (String) row.get(key);
        default:
            throw new RuntimeException("Not implemented: " + type);
        }
    }

    // ----- setters -----

    public void setValue(Serializable value) throws StorageException {
        checkWritable();
        switch (type) {
        case STRING:
            if (!(value instanceof String)) {
                throw new RuntimeException("Value is not a string: " + value);
            }
            row.put(key, value);
            // mark dataRow as dirty!
            break;
        default:
            throw new RuntimeException("Not implemented: " + type);
        }
    }

}
