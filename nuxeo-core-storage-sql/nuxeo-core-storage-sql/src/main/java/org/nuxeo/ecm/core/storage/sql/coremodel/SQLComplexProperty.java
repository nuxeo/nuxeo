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
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.PropertyContainer;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * A {@link SQLComplexProperty} gives access to a wrapped SQL-level {@link Node}
 * . This is used for documents and for complex properties.
 *
 * @author Florent Guillaume
 */
public class SQLComplexProperty extends SQLBaseProperty implements
        PropertyContainer {

    private static final Log log = LogFactory.getLog(SQLComplexProperty.class);

    private final Node node;

    protected final SQLSession session;

    /**
     * Creates a {@link SQLComplexProperty} to wrap a {@link Node}.
     */
    public SQLComplexProperty(Node node, ComplexType type, SQLSession session,
            boolean readonly) {
        super(type, readonly);
        this.node = node;
        this.session = session;
    }

    /**
     * Returns the node with info about the object's data.
     */
    protected Node getDataNode() {
        return node;
    }

    /**
     * Returns the node with info about the hierarchy location.
     * <p>
     * It's only different from the {@link #getDataNode} for proxies.
     */
    protected Node getHierarchyNode() {
        return node;
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    public String getName() {
        return node.getName();
    }

    public Object getValue() throws DocumentException {
        Map<String, Object> map = new HashMap<String, Object>();
        Collection<Property> properties = getProperties();
        for (Property property : properties) {
            map.put(property.getName(), property.getValue());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object value) throws DocumentException {
        checkWritable();
        Map<String, Object> map = (Map<String, Object>) value;
        if (map == null) {
            // XXX should delete the node?
            for (Property property : getProperties()) {
                property.setValue(null);
            }
        } else {
            for (Entry<String, Object> entry : map.entrySet()) {
                Property property = getProperty(entry.getKey());
                property.setValue(entry.getValue());
            }
        }
    }

    /*
     * ----- Property & PropertyContainer -----
     */

    @Override
    public boolean isPropertySet(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty(String name) throws DocumentException {
        return session.makeProperty(node, name, (ComplexType) type, readonly);
    }

    @Override
    public Collection<Property> getProperties() throws DocumentException {
        Collection<Field> fields = ((ComplexType) type).getFields();
        List<Property> properties = new ArrayList<Property>(fields.size());
        for (Field field : fields) {
            properties.add(getProperty(field.getName().getPrefixedName()));
        }
        return properties;
    }

    @Override
    public Iterator<Property> getPropertyIterator() throws DocumentException {
        return getProperties().iterator();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.PropertyContainer -------------------
     * (used for SQLDocument, SQLComplexProperty itself doesn't need it)
     */

    public Map<String, Object> exportFlatMap(String[] schemas)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Map<String, Map<String, Object>> exportMap(String[] schemas)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> exportMap(String schemaName)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void importFlatMap(Map<String, Object> map) throws DocumentException {
        checkWritable();
        throw new UnsupportedOperationException();
    }

    public void importMap(Map<String, Map<String, Object>> map)
            throws DocumentException {
        checkWritable();
        throw new UnsupportedOperationException();
    }

    public List<String> getDirtyFields() {
        throw new UnsupportedOperationException();
    }

    public Object getPropertyValue(String name) throws DocumentException {
        // when called from AbstractSession.getDataModelFields,
        // we may get an unprefixed name...
        try {
            return getProperty(name).getValue();
        } catch (NoSuchPropertyException e) {
            // XXX we do this because when reading prefetched values,
            // only DocumentException is expected
            // (see DocumentModelFactory.createDocumentModel)
            throw new DocumentException(e);
        }
    }

    public String getString(String name) throws DocumentException {
        return (String) getProperty(name).getValue();
    }

    public boolean getBoolean(String name) throws DocumentException {
        Boolean value = (Boolean) getProperty(name).getValue();
        return value == null ? false : value.booleanValue();
    }

    public long getLong(String name) throws DocumentException {
        Long value = (Long) getProperty(name).getValue();
        return value == null ? 0L : value.longValue();
    }

    public double getDouble(String name) throws DocumentException {
        Double value = (Double) getProperty(name).getValue();
        return value == null ? 0D : value.doubleValue();
    }

    public Calendar getDate(String name) throws DocumentException {
        return (Calendar) getProperty(name).getValue();
    }

    public Blob getContent(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void setPropertyValue(String name, Object value)
            throws DocumentException {
        // TODO check constraints
        try {
            getProperty(name).setValue(value);
            // TODO mark dirty fields
        } catch (DocumentException e) {
            // we log a debugging message here as it is a point where the
            // property name is known
            log.error("Error setting property: " + name + " value: " + value);
            throw e;
        }
    }

    public void setString(String name, String value) throws DocumentException {
        setPropertyValue(name, value);
    }

    public void setBoolean(String name, boolean value) throws DocumentException {
        setPropertyValue(name, Boolean.valueOf(value));
    }

    public void setLong(String name, long value) throws DocumentException {
        setPropertyValue(name, Long.valueOf(value));
    }

    public void setDouble(String name, double value) throws DocumentException {
        setPropertyValue(name, Double.valueOf(value));
    }

    public void setDate(String name, Calendar value) throws DocumentException {
        setPropertyValue(name, value);
    }

    public void setContent(String name, Blob value) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void removeProperty(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

}
