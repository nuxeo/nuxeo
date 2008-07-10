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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.repository.jcr.properties.PropertyFactory;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;

/**
 * A {@link SQLComplexProperty} gives access to a wrapped SQL-level {@link Node}
 * .
 *
 * @author Florent Guillaume
 */
public class SQLComplexProperty implements Property {

    private final Node node;

    private final Field field;

    private final ComplexType type;

    /**
     * Creates a {@link SQLComplexProperty} to wrap a {@link Node}.
     */
    public SQLComplexProperty(Node node, Field field) {
        this.node = node;
        this.field = field;
        type = (ComplexType) field.getType();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    public String getName() {
        return node.getName();
    }

    public ComplexType getType() {
        return type;
    }

    public boolean isNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void setNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getValue() throws DocumentException {
        Map<String, Object> map = new HashMap<String, Object>();
        Collection<Property> properties = getProperties();
        for (Property property : properties) {
            map.put(property.getName(), property.getValue());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object value) throws DocumentException {
        Map<String, Object> map = (Map<String, Object>) value;
        for (Entry<String, Object> entry : map.entrySet()) {
            getProperty(entry.getKey()).setValue(entry.getValue());
        }
    }

    public boolean isPropertySet(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Property getProperty(String name) throws DocumentException {
        Field field = type.getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return SQLPropertyHelper.getProperty(node, field);
    }

    public Collection<Property> getProperties() throws DocumentException {
        Collection<Field> fields = type.getFields();
        List<Property> properties = new ArrayList<Property>(fields.size());
        for (Field childField : fields) {
            String name = childField.getName().getPrefixedName();
            SQLSession session;
            Node child = session.getChildPropertyNode(node, name);
            // XXX TODO may be null!
            properties.add(SQLPropertyHelper.getProperty(child, childField));
        }
        return properties;
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        throw new UnsupportedOperationException();
    }

}
