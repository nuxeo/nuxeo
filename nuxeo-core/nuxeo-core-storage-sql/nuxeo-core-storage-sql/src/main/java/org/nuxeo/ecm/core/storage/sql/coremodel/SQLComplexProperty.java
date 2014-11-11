/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        super(type, node == null ? null : node.getName(), readonly);
        this.node = node;
        this.session = session;
    }

    // for SQLDocument
    public Node getNode() {
        return node;
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    @Override
    public String getName() {
        return node.getName();
    }

    @Override
    public Object getValue() throws DocumentException {
        Map<String, Object> map = new HashMap<String, Object>();
        Collection<Property> properties = getProperties();
        for (Property property : properties) {
            map.put(property.getName(), property.getValue());
        }
        return map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValue(Object value) throws DocumentException {
        checkWritable();
        if (value == null) {
            // XXX should delete the node?
            for (Property property : getProperties()) {
                property.setValue(null);
            }
        } else {
            if (!(value instanceof Map)) {
                throw new DocumentException(
                        "Invalid value for complex property (map needed): "
                                + value);
            }
            Map<String, Object> map = (Map<String, Object>) value;
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
        return session.makeProperty(node, name, (ComplexType) type, null,
                readonly);
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

    @Override
    public Map<String, Object> exportFlatMap(String[] schemas)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Map<String, Object>> exportMap(String[] schemas)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> exportMap(String schemaName)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importFlatMap(Map<String, Object> map) throws DocumentException {
        checkWritable();
        throw new UnsupportedOperationException();
    }

    @Override
    public void importMap(Map<String, Map<String, Object>> map)
            throws DocumentException {
        checkWritable();
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getDirtyFields() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getPropertyValue(String name) throws DocumentException {
        // when called from AbstractSession.getDataModelFields,
        // we may get an unprefixed name...
        return getProperty(name).getValue();
    }

    @Override
    public String getString(String name) throws DocumentException {
        return (String) getProperty(name).getValue();
    }

    @Override
    public boolean getBoolean(String name) throws DocumentException {
        Boolean value = (Boolean) getProperty(name).getValue();
        return value == null ? false : value.booleanValue();
    }

    @Override
    public long getLong(String name) throws DocumentException {
        Long value = (Long) getProperty(name).getValue();
        return value == null ? 0L : value.longValue();
    }

    @Override
    public double getDouble(String name) throws DocumentException {
        Double value = (Double) getProperty(name).getValue();
        return value == null ? 0D : value.doubleValue();
    }

    @Override
    public Calendar getDate(String name) throws DocumentException {
        return (Calendar) getProperty(name).getValue();
    }

    @Override
    public Blob getContent(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPropertyValue(String name, Object value)
            throws DocumentException {
        // TODO check constraints
        try {
            getProperty(name).setValue(value);
            // TODO mark dirty fields
        } catch (DocumentException e) {
            // we log a debugging message here as it is a point where the
            // property name is known
            log.debug("Error setting property: " + name + " value: " + value);
            throw e;
        }
    }

    @Override
    public void setString(String name, String value) throws DocumentException {
        setPropertyValue(name, value);
    }

    @Override
    public void setBoolean(String name, boolean value) throws DocumentException {
        setPropertyValue(name, Boolean.valueOf(value));
    }

    @Override
    public void setLong(String name, long value) throws DocumentException {
        setPropertyValue(name, Long.valueOf(value));
    }

    @Override
    public void setDouble(String name, double value) throws DocumentException {
        setPropertyValue(name, Double.valueOf(value));
    }

    @Override
    public void setDate(String name, Calendar value) throws DocumentException {
        setPropertyValue(name, value);
    }

    @Override
    public void setContent(String name, Blob value) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProperty(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

}
