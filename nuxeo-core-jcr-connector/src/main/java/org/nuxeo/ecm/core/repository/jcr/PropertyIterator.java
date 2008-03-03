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

package org.nuxeo.ecm.core.repository.jcr;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.repository.jcr.properties.PropertyFactory;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * TODO: refactor this class
 */
public class PropertyIterator implements Iterator<Property> {

    public static final Iterator<Property> EMPTY_ITERATOR = new Iterator<Property>() {
        public Property next() {
            throw new NoSuchElementException();
        }
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
        public boolean hasNext() {
            return false;
        }
    };

    private final JCRNodeProxy parent;
    private final Iterator<Field> fieldIterator;


    public PropertyIterator(JCRNodeProxy parent, String schemaName) {
        if (schemaName != null) {
            ComplexType schema = parent.getSchema(schemaName);
            fieldIterator = schema.getFields().iterator();
        } else {
            fieldIterator = parent.getFields().iterator();
        }
        this.parent = parent;
    }

    public boolean hasNext() {
        return fieldIterator.hasNext();
    }

    public Property next() {
        if (fieldIterator.hasNext()) {
            try {
                return PropertyFactory.getProperty(parent,
                        fieldIterator.next());
            } catch (DocumentException e) {
                e.printStackTrace();
                return next();
            }
        }
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

}
