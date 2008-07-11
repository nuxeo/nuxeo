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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.SimpleProperty;

/**
 * A {@link SQLSimpleProperty} gives access to a wrapped SQL-level
 * {@link SimpleProperty}.
 *
 * @author Florent Guillaume
 */
public class SQLSimpleProperty implements Property {

    private final SimpleProperty property;

    private final Type type;

    /**
     * Creates a {@link SQLSimpleProperty} to wrap a {@link SimpleProperty}.
     */
    public SQLSimpleProperty(SimpleProperty property, Type type) {
        this.property = property;
        this.type = type;
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    public String getName() {
        return property.getName();
    }

    public Type getType() {
        return type;
    }

    public boolean isNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void setNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Serializable getValue() throws DocumentException {
        return property.getValue();
    }

    public void setValue(Object value) throws DocumentException {
        if (value != null && !(value instanceof Serializable)) {
            throw new DocumentException("Value is not Serializable: " + value);
        }
        try {
            property.setValue((Serializable) value);
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
