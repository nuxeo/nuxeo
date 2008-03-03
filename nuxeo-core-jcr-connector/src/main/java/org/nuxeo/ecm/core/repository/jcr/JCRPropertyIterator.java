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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.repository.jcr.properties.PropertyFactory;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.schema.types.ComplexType;

/**
 * An iterator over JCR nodes corresponding to properties in a document.
 * <p>
 * This is iterating only properties existing in the storage
 * (properties explicitely set by the user)
 * <p>
 * To iterate over all properties (schema fields) of the document you must use the
 * {@link PropertyIterator}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JCRPropertyIterator implements Iterator<Property>, NodeFilter, PropertyFilter {

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
    private final ComplexType schema;
    private final FilteredPropertyIterator propertyIterator;
    private final FilteredNodeIterator nodeIterator;

    public JCRPropertyIterator(JCRNodeProxy parent, String schema) throws DocumentException {
        this.schema = schema == null ? null : parent.getSchema(schema);
        this.parent = parent;
        try {
            nodeIterator = new FilteredNodeIterator(parent.getNode(), this);
            propertyIterator = new FilteredPropertyIterator(parent.getNode(), this);
        } catch (RepositoryException e) {
            throw new DocumentException("failed to create iterator for property " + parent, e);
        }
    }

    public boolean hasNext() {
        return propertyIterator.hasNext() || nodeIterator.hasNext();
    }

    public Property next() {
        //TODO optimize getProperty since the schema check is already done in the iterator filter
        // avoid doing it twice
        try {
            if (propertyIterator.hasNext()) {
                return PropertyFactory.getProperty(parent, propertyIterator.nextProperty());
            }
            if (nodeIterator.hasNext()) {
                return PropertyFactory.getProperty(parent, nodeIterator.nextNode());
            }
        } catch (DocumentException e) {
            e.printStackTrace();
            return next();
        }
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    public boolean accept(Node node) throws RepositoryException {
        String name = node.getName();
        if (schema != null) {
            return schema.getField(name) != null;
        }
        return parent.getField(name) != null;
    }

    public boolean accept(javax.jcr.Property property) throws RepositoryException {
        String name = property.getName();
        if (schema != null) {
            return schema.getField(name) != null;
        }
        return parent.getField(name) != null;
    }

}
