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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.PropertyContainer;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * A {@link SQLComplexListProperty} gives access to a wrapped collection of
 * SQL-level {@link Node}s.
 *
 * @author Florent Guillaume
 */
public class SQLComplexListProperty extends SQLBaseProperty implements
        PropertyContainer {

    protected final Node node;

    protected final String name;

    protected final SQLSession session;

    protected final ComplexType elementType;

    /**
     * Creates a {@link SQLComplexListProperty} to wrap a collection of
     * {@link Node}s.
     */
    public SQLComplexListProperty(Node node, ListType type, String name,
            SQLSession session, boolean readonly) {
        super(type, name, readonly);
        this.node = node;
        this.name = name;
        this.session = session;
        elementType = (ComplexType) type.getFieldType();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Object> getValue() throws DocumentException {
        List<Property> properties = getProperties();
        List<Object> list = new ArrayList<Object>(properties.size());
        for (Property property : properties) {
            list.add(property.getValue());
        }
        return list;
    }

    @Override
    public void setValue(Object value) throws DocumentException {
        checkWritable();
        if (value instanceof ListDiff) {
            setList((ListDiff) value);
        } else if (value instanceof List) {
            setList((List<?>) value);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported value object for a complex list: "
                            + value.getClass().getName());
        }
    }

    /*
     * ----- Property & PropertyContainer -----
     */

    @Override
    public Property getProperty(String posString) throws DocumentException {
        int pos;
        try {
            pos = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            throw new NoSuchPropertyException(name + '/' + posString);
        }
        return session.makeProperty(node, name, type, readonly, pos);
    }

    @Override
    public List<Property> getProperties() throws DocumentException {
        return session.makeProperties(node, name, type, null, null, readonly,
                -1, -1);
    }

    @Override
    public Iterator<Property> getPropertyIterator() throws DocumentException {
        return getProperties().iterator();
    }

    @Override
    public boolean isPropertySet(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- PropertyContainer -----
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
        getProperty(name).setValue(value);
        // TODO mark dirty fields
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

    /*
     * ----- internal -----
     */

    public void setList(List<?> list) throws DocumentException {
        // don't add/remove nodes for unchanged complex value
        if (getValue().equals(list)) {
            return;
        }

        List<Property> properties = getProperties();
        int oldSize = properties.size();
        int newSize = list.size();
        // remove extra list elements
        if (oldSize > newSize) {
            for (int i = newSize; i < oldSize; i++) {
                SQLComplexProperty property = (SQLComplexProperty) properties.get(i);
                session.removeProperty(property.getNode());
            }
            for (int i = newSize; i < oldSize; i++) {
                properties.remove(newSize);
            }
        }
        // add new list elements
        if (oldSize < newSize) {
            List<Property> newProperties = session.makeProperties(node, name,
                    type, null, null, readonly, properties.size(), newSize);
            properties.addAll(newProperties);
        }
        // set values
        int i = 0;
        for (Object value : list) {
            properties.get(i++).setValue(value);
        }
    }

    public void setList(ListDiff list) {
        if (!list.isDirty()) {
            return;
        }
        for (ListDiff.Entry entry : list.diff()) {
            switch (entry.type) {
            case ListDiff.ADD:
                // add(entry.value);
                break;
            case ListDiff.REMOVE:
                // remove(entry.index);
                break;
            case ListDiff.INSERT:
                // insert(entry.index, entry.value);
                break;
            case ListDiff.MOVE:
                // move(entry.index, (Integer) entry.value);
                break;
            case ListDiff.MODIFY:
                // modify(entry.index, entry.value);
                break;
            case ListDiff.CLEAR:
                // clear();
                break;
            }
        }
        throw new UnsupportedOperationException();
    }

}
