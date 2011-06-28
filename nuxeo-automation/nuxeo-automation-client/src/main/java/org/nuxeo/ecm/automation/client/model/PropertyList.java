/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PropertyList implements Serializable {

    private static final long serialVersionUID = -3058252973413747313L;

    protected final List<Object> list;

    public PropertyList() {
        list = new ArrayList<Object>();
    }

    public PropertyList(int size) {
        list = new ArrayList<Object>(size);
    }

    public PropertyList(List<Object> list) {
        this.list = new ArrayList<Object>(list);
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public String getString(int i) {
        return getString(i, null);
    }

    public Boolean getBoolean(int i) {
        return getBoolean(i, null);
    }

    public Long getLong(int i) {
        return getLong(i, null);
    }

    public Double getDouble(int i) {
        return getDouble(i, null);
    }

    public Date getDate(int i) {
        return getDate(i, null);
    }

    public PropertyList getList(int i) {
        return getList(i, null);
    }

    public PropertyMap getMap(int i) {
        return getMap(i, null);
    }

    public String getString(int i, String defValue) {
        return PropertiesHelper.getString(list.get(i), defValue);
    }

    public Boolean getBoolean(int i, Boolean defValue) {
        return PropertiesHelper.getBoolean(list.get(i), defValue);
    }

    public Long getLong(int i, Long defValue) {
        return PropertiesHelper.getLong(list.get(i), defValue);
    }

    public Double getDouble(int i, Double defValue) {
        return PropertiesHelper.getDouble(list.get(i), defValue);
    }

    public Date getDate(int i, Date defValue) {
        return PropertiesHelper.getDate(list.get(i), defValue);
    }

    public PropertyList getList(int i, PropertyList defValue) {
        return PropertiesHelper.getList(list.get(i), defValue);
    }

    public PropertyMap getMap(int i, PropertyMap defValue) {
        return PropertiesHelper.getMap(list.get(i), defValue);
    }

    public void set(int i, String value) {
        if (value == null) {
            list.remove(i);
        }
        list.set(i, value);
    }

    public void set(int i, Boolean value) {
        if (value == null) {
            list.remove(i);
        }
        list.set(i, value.toString());
    }

    public void set(int i, Long value) {
        if (value == null) {
            list.remove(i);
        }
        list.set(i, value.toString());
    }

    public void set(int i, Double value) {
        if (value == null) {
            list.remove(i);
        }
        list.set(i, value.toString());
    }

    public void set(int i, Date value) {
        if (value == null) {
            list.remove(i);
        }
        list.set(i, DateUtils.formatDate(value));
    }

    public void set(int i, PropertyList value) {
        if (value == null) {
            list.remove(i);
        }
        list.set(i, value);
    }

    public void set(int i, PropertyMap value) {
        if (value == null) {
            list.remove(i);
        }
        list.set(i, value);
    }

    public void add(String value) {
        list.add(value);
    }

    public void add(Boolean value) {
        list.add(value.toString());
    }

    public void add(Long value) {
        list.add(value.toString());
    }

    public void add(Double value) {
        list.add(value.toString());
    }

    public void add(Date value) {
        list.add(DateUtils.formatDate(value));
    }

    public void add(PropertyList value) {
        list.add(value);
    }

    public void add(PropertyMap value) {
        list.add(value);
    }

    public List<Object> list() {
        return list;
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
