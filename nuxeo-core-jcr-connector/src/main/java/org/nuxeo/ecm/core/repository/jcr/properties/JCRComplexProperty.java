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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.repository.jcr.JCRDocument;
import org.nuxeo.ecm.core.repository.jcr.JCRNodeProxy;
import org.nuxeo.ecm.core.repository.jcr.ModelAdapter;
import org.nuxeo.ecm.core.repository.jcr.PropertyContainerAdapter;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings({"SuppressionAnnotation"})
public class JCRComplexProperty implements Property, JCRNodeProxy {

    protected Node node;
    protected final ComplexType type; // cached to avoid casts
    protected final JCRNodeProxy parent;
    protected final Field field;

    JCRComplexProperty(JCRNodeProxy parent, Node node, Field field) {
        type = (ComplexType) field.getType();
        this.parent = parent;
        this.node = node;
        this.field = field;
    }

    public JCRDocument getDocument() {
        if (parent == null) {
            throw new AssertionError("JCRNodeProxy for properties must have a non null parent");
        }
        return parent.getDocument();
    }

    public Type getType() throws DocumentException {
        return type;
    }

    public String getName() throws DocumentException {
        return field.getName().getPrefixedName();
    }

    public boolean isPropertySet(String path) throws DocumentException {
        if (node == null) {
            return false;
        }
        return PropertyContainerAdapter.hasProperty(node, path);
    }

    public Property getProperty(String name) throws DocumentException {
        return PropertyFactory.getProperty(this, name);
    }


    public Collection<Property> getProperties() throws DocumentException {
        return PropertyContainerAdapter.getProperties(this);
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        return PropertyContainerAdapter.getPropertyIterator(this);
    }

    public Object getValue() throws DocumentException {
        if (node == null) {
            return field.getDefaultValue();
        }
        return doGetValue();
    }

    public void setValue(Object value) throws DocumentException {
        if (node == null) {
            connect();
        }
        doSetValue(value);
    }


    public void setNull() throws DocumentException {
        if (node == null) {
            return;
        }
        try {
            node.remove();
        } catch (RepositoryException e) {
            throw new DocumentException("failed to remove property " + field.getName(), e);
        }
    }

    public boolean isNull() throws DocumentException {
        return node == null;
    }


    // ----------- JCRNodeProxy ----------

    public Node getNode() throws DocumentException {
        if (node == null) {
            connect();
        }
        return node;
    }

    public Node connect() throws DocumentException {
        if (parent == null) {
            throw new Error("null property have no parent - should be a bug");
        }
        try {
            if (!parent.isConnected()) {
                parent.connect(); // make sure parent exists
            }
            node = ModelAdapter.addPropertyNode(parent.getNode(),
                    field.getName().getPrefixedName(), type.getName());
            if (type.isUnstructured()) {
                ModelAdapter.setUnstructured(node);
            }
        } catch (RepositoryException e) {
            throw new DocumentException("failed to create complex property: "
                    + field.getName(), e);
        }
        return node;
    }

    public boolean isConnected() {
        return node != null;
    }

    public Field getField(String name) {
        return type.getField(name);
    }

    public Field getField(String schema, String name) {
        return type.getField(name); // TODO
    }

    public ComplexType getSchema(String schema) {
        return type;
    }

    public Collection<Field> getFields() {
        return type.getFields();
    }



    protected Object doGetValue() throws DocumentException {
        Map<String, Object> map = new HashMap<String, Object>();
        Collection<Property> props = getProperties();
        for (Property property : props) {
            map.put(property.getName(), property.getValue());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    protected void doSetValue(Object value) throws DocumentException {
        Map<String, Object> map = (Map<String, Object>) value;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            getProperty(entry.getKey()).setValue(entry.getValue());
        }
    }

}
