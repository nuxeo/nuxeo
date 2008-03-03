/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.properties;

import java.util.Collection;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.repository.jcr.JCRNodeProxy;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class JCRScalarProperty implements Property {

    javax.jcr.Property property;

    final JCRNodeProxy parent;

    final Field field;


    protected JCRScalarProperty(JCRNodeProxy parent, javax.jcr.Property property, Field field) {
        this.parent = parent;
        this.property = property;
        this.field = field;
    }

    public String getName() throws DocumentException {
        if (field == null) {
            assert property != null;
            try {
                return property.getName();
            } catch (RepositoryException e) {
                throw new DocumentException("Failed to get property name", e);
            }
        }
        return field.getName().getPrefixedName();
    }

    public Type getType() {
        return field.getType();
    }

    public boolean isNull() throws DocumentException {
        return property == null;
    }

    public void setNull() throws DocumentException {
        try {
            if (property != null) {
                property.remove();
                property = null;
            }
        } catch (RepositoryException e) {
            throw new DocumentException("failed to set property " + field.getName());
        }
    }

    public void setValue(Object value) throws DocumentException {
        try {
            if (property == null) {
                assert field != null && parent != null;
                if (!parent.isConnected()) {
                    parent.connect();
                }
                property = create(value);
            } else {
                set(value);
            }
        } catch (RepositoryException e) {
            throw new DocumentException("failed to set scalar property " + field.getName(), e);
        }
    }


    public Object getValue() throws DocumentException {
        if (property == null) {
            return field.getDefaultValue();
        }
        try {
            return get();
        } catch (RepositoryException e) {
            throw new DocumentException("failed to get scalar property " + field.getName(), e);
        }
    }

    public Collection<Property> getProperties() throws DocumentException {
        throw new UnsupportedOperationException("scalar properties cannot contains children");
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        throw new UnsupportedOperationException("scalar properties cannot contains children");
    }

    public Property getProperty(String name) throws DocumentException {
        throw new UnsupportedOperationException("scalar nproperties cannot contains children");
    }

    public boolean isPropertySet(String name) throws DocumentException {
        throw new UnsupportedOperationException("scalar nproperties cannot contains children");
    }


    protected abstract javax.jcr.Property create(Object value) throws DocumentException;

    protected abstract void set(Object value) throws RepositoryException, DocumentException;

    protected abstract Object get() throws RepositoryException, DocumentException;

}
