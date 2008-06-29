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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.repository.jcr.properties.PropertyFactory;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class PropertyContainerAdapter {

    private PropertyContainerAdapter() {

    }

    public static Object getDefaultValue(DocumentType type, String name)
            throws NoSuchPropertyException {
        Field field = type.getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return field.getDefaultValue();
    }

    public static boolean hasProperty(Node node, String path)
            throws DocumentException {
        try {
            return ModelAdapter.hasField(node, path);
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public static void removeProperty(Node node, String name)
            throws DocumentException {
        try {
            if (node.hasProperty(name)) {
                node.getProperty(name).remove();
            } else if (node.hasNode(name)) {
                node.getNode(name).remove();
            }
        } catch (PathNotFoundException e) {
            throw new NoSuchPropertyException(name, e);
        } catch (RepositoryException e) {
            throw new DocumentException("failed to get scalar property " + name, e);
        }
    }

    public static String getString(DocumentType type, Node node, String name)
            throws DocumentException {
        try {
            javax.jcr.Property p = node.getProperty(name);
            return p.getString();
        } catch (PathNotFoundException e) {
            // node not found - check the schema
            return (String) getDefaultValue(type, name);
        } catch (RepositoryException e) {
            throw new DocumentException("failed to get scalar property: " + name, e);
        }
    }

    public static boolean getBoolean(DocumentType type, Node node, String name)
            throws DocumentException {
        try {
            javax.jcr.Property p = node.getProperty(name);
            return p.getBoolean();
        } catch (PathNotFoundException e) {
            // node not found - check the schema
            Boolean val = (Boolean) getDefaultValue(type, name);
            return val == null ? false : val;
        } catch (RepositoryException e) {
            throw new DocumentException("failed to get scalar property: " + name, e);
        }
    }

    public static double getDouble(DocumentType type, Node node, String name)
            throws DocumentException {
        try {
            javax.jcr.Property p = node.getProperty(name);
            return p.getDouble();
        } catch (PathNotFoundException e) {
            // node not found - check the schema
            Double val = (Double) getDefaultValue(type, name);
            return val == null ? 0 : val;
        } catch (RepositoryException e) {
            throw new DocumentException("failed to get scalar property: " + name, e);
        }
    }

    public static long getLong(DocumentType type, Node node, String name)
            throws DocumentException {
        try {
            javax.jcr.Property p = node.getProperty(name);
            return p.getLong();
        } catch (PathNotFoundException e) {
            // node not found - check the schema
            Long val = (Long) getDefaultValue(type, name);
            return val == null ? 0 : val;
        } catch (RepositoryException e) {
            throw new DocumentException("failed to get scalar property: " + name, e);
        }
    }

    public static Calendar getDate(DocumentType type, Node node, String name)
            throws DocumentException {
        try {
            javax.jcr.Property p = node.getProperty(name);
            return p.getDate();
        } catch (PathNotFoundException e) {
            // node not found - check the schema
            return (Calendar) getDefaultValue(type, name);
        } catch (RepositoryException e) {
            throw new DocumentException("failed to get scalar property: " + name, e);
        }
    }

    public static Blob getContent(DocumentType type, Node node, String name)
            throws DocumentException {
        try {
            return new JCRBlob(node.getNode(name));
        } catch (PathNotFoundException e) {
            // node not found - check the schema
            return (Blob) getDefaultValue(type, name);
        } catch (RepositoryException e) {
            throw new DocumentException("getContent failed", e);
        }
    }

    public static void setString(Node node, String name, String value)
            throws DocumentException {
        try {
            node.setProperty(name, value);
        } catch (PathNotFoundException e) {
            throw new NoSuchPropertyException(name, e);
        } catch (RepositoryException e) {
            throw new DocumentException("failed to set string property: " + name, e);
        }
    }

    public static void setBoolean(Node node, String name, boolean value)
            throws DocumentException {
        try {
            node.setProperty(name, value);
        } catch (PathNotFoundException e) {
            throw new NoSuchPropertyException(name, e);
        } catch (RepositoryException e) {
            throw new DocumentException("failed to set boolean property: " + name, e);
        }
    }

    public static void setLong(Node node, String name, long value)
            throws DocumentException {
        try {
            node.setProperty(name, value);
        } catch (PathNotFoundException e) {
            throw new NoSuchPropertyException(name, e);
        } catch (RepositoryException e) {
            throw new DocumentException("failed to set long property: " + name, e);
        }
    }

    public static void setDouble(Node node, String name, double value)
            throws DocumentException {
        try {
            node.setProperty(name, value);
        } catch (PathNotFoundException e) {
            throw new NoSuchPropertyException(name, e);
        } catch (RepositoryException e) {
            throw new DocumentException("failed to set double property: " + name, e);
        }
    }

    public static void setDate(Node node, String name, Calendar value)
            throws DocumentException {
        try {
            node.setProperty(name, value);
        } catch (PathNotFoundException e) {
            throw new NoSuchPropertyException(name, e);
        } catch (RepositoryException e) {
            throw new DocumentException("failed to set date property: " + name, e);
        }
    }

    public static void setContent(Node node, String name, Blob value)
            throws DocumentException {
        JCRBlob.setContent(node, name, value);
    }

    public static Collection<org.nuxeo.ecm.core.model.Property> getProperties(JCRNodeProxy node)
            throws DocumentException {
        Node jcrNode  = node.getNode();
        if (jcrNode == null) {
            return Collections.emptyList();
        }
        Collection<Field> fields = node.getFields();
        List<Property> props = new ArrayList<Property>(fields.size());
        for (Field field : fields) {
            props.add(PropertyFactory.getProperty(node, field));
        }
        return props;
    }

    public static Iterator<org.nuxeo.ecm.core.model.Property> getPropertyIterator(JCRNodeProxy node)
            throws DocumentException {
        Node jcrNode  = node.getNode();
        if (jcrNode == null) {
            return PropertyIterator.EMPTY_ITERATOR;
        }
        return new PropertyIterator(node, null);
    }

}
