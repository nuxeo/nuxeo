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
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A {@link SimpleProperty} gives access to a scalar value stored in an
 * underlying {@link SimpleFragment}.
 *
 * @author Florent Guillaume
 */
public class SimpleProperty extends AbstractProperty {

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
        row.put(key, normalize(value));
        // mark fragment dirty!
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    public boolean isNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void setNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Serializable getValue() throws DocumentException {
        return row.get(key);
    }

    public void setValue(Object value) throws DocumentException {
        if (value != null && !(value instanceof Serializable)) {
            throw new DocumentException("Value is not Serializable: " + value);
        }
        try {
            setValue((Serializable) value);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    public boolean isPropertySet(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Property getProperty(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Collection<Property> getProperties() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        throw new UnsupportedOperationException();
    }

}
