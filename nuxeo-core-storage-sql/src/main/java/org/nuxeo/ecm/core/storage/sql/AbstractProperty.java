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
import java.util.Date;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A {@code Property} gives access to a scalar or array value stored in an
 * underlying table. This base class contains common code.
 * <p>
 * When stored, the values are normalized to their standard type.
 *
 * @author Florent Guillaume
 */
public abstract class AbstractProperty implements
        org.nuxeo.ecm.core.model.Property {

    /** The property name. */
    private final String name;

    /** The property name. */
    protected final PropertyType type;

    /** Is this property readonly (for system properties). */
    private final boolean readonly;

    /**
     * Creates a Property.
     */
    public AbstractProperty(String name, PropertyType type, boolean readonly) {
        this.name = name;
        this.type = type;
        this.readonly = readonly;
    }

    // ----- basics -----

    public String getName() {
        return name;
    }

    /**
     * Normalize a value to its storage format.
     * <p>
     * Note: date-based values are normalized to a java {@link Calendar}.
     */
    public Serializable normalize(Serializable value) throws StorageException {
        if (value == null) {
            return null;
        }
        switch (type.isArray() ? type.getArrayBaseType() : type) {
        case STRING:
            if (value instanceof String) {
                return value;
            }
            throw new StorageException("Value is not a String: " + value);
        case BOOLEAN:
            if (value instanceof Boolean) {
                return value;
            }
            throw new StorageException("Value is not a Boolean: " + value);
        case DATETIME:
            if (value instanceof Calendar) {
                return value;
            }
            if (value instanceof Date) {
                Calendar cal = new GregorianCalendar(); // XXX timezone
                cal.setTime((Date) value);
                return cal;
            }
            throw new StorageException("Value is not a Calendar: " + value);
        default:
            throw new UnsupportedOperationException(type.toString());
        }
    }

    // ----- modification -----

    public void refresh(boolean keepChanges) throws StorageException {
        throw new UnsupportedOperationException();
    }

    public void remove() throws StorageException {
        throw new UnsupportedOperationException();
    }

    public void save() throws StorageException {
        throw new UnsupportedOperationException();
    }

    protected void checkWritable() throws StorageException {
        if (readonly) {
            throw new StorageException("Cannot write property: " + name);
        }
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    public Type getType() throws DocumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
