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

import java.util.Collection;
import java.util.Iterator;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @author Florent Guillaume
 */
public abstract class SQLBaseProperty implements Property {

    protected final Type type;

    protected final boolean readonly;

    public SQLBaseProperty(Type type, boolean readonly) {
        this.type = type;
        this.readonly = readonly;
    }

    // for SQLDocument
    public void checkWritable() throws DocumentException {
        if (readonly) {
            throw new DocumentException("Cannot write property: " + getName());
        }
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    public Type getType() {
        return type;
    }

    public boolean isNull() {
        throw new UnsupportedOperationException();
    }

    public void setNull() throws DocumentException {
        throw new UnsupportedOperationException();
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
