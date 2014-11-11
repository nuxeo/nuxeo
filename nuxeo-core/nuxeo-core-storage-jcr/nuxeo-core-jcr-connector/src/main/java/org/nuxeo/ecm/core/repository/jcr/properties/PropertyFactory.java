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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.repository.jcr.JCRNodeProxy;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class PropertyFactory {

    // This is an utility class.
    private PropertyFactory() { }

    /**
     * Creates a property instance for the given JCR property.
     * <p>
     * The parent can be null if not known. In this case it will be computed
     * by introspecting the JCR tree.
     *
     * @param parent the parent node proxy. May be null.
     * @param item the JCR property. Must be not null
     * @return the ECM property instance
     * @throws DocumentException
     */
    public static Property getProperty(JCRNodeProxy parent, javax.jcr.Property item)
                throws DocumentException {
        String name;
        try {
            name = item.getName();
        } catch (RepositoryException e) {
            throw new DocumentException("failed to get property name for " + item, e);
        }
        if (parent == null) {
            // TODO find the parent using jcr tree introspection
        }
        Field field = parent.getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return ScalarPropertyFactory.getProperty(parent, item, field);
    }

    /**
     * Gets a property instance given the node proxy parent and the property name.
     *
     * @param parent the parent proxy node. Must be not null
     * @param name the property name. Must be not null
     * @return the ECM property instance
     *
     * @throws DocumentException
     */
    public static Property getProperty(JCRNodeProxy parent, String name) throws DocumentException {
        Field field = parent.getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return getProperty(parent, field);
    }

    public static Property getProperty(JCRNodeProxy parent, Field field) throws DocumentException {
        Type type = field.getType();
        if (type.isSimpleType()) {
            return ScalarPropertyFactory.getProperty(parent, null, field);
        } else if (type.isListType()) {
            ListType listType = (ListType) type;
            if (listType.getFieldType().isSimpleType()) {
                return ScalarPropertyFactory.getProperty(parent, null, field);
            } else {
                return CompositePropertyFactory.getProperty(parent, null, field);
            }
        } else {
            return CompositePropertyFactory.getProperty(parent, null, field);
        } // TODO handle LIST type?
    }

    /**
     * Create a property instance for the given JCR node
     * <p>
     * The parent can be null if not known. In this case it will be computed
     * by introspecting the JCR tree.
     *
     * @param parent the parent node proxy. May be null.
     * @param item the JCR property. Must be not null
     * @return the ECM property instance
     * @throws DocumentException
     */
    public static Property getProperty(JCRNodeProxy parent, Node item)
                throws DocumentException {
        String name;
        try {
            name = item.getName();
        } catch (RepositoryException e) {
            throw new DocumentException("failed to get property name for " + item, e);
        }
        if (parent == null) {
            // TODO find the parent using jcr tree introspection
        }
        Field field = parent.getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return CompositePropertyFactory.getProperty(parent, item, field);
    }

}
