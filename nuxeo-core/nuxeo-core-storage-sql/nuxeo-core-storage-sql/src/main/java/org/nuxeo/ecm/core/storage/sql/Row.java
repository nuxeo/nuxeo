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
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.storage.binary.Binary;

/**
 * The data of a single row in a table (keys/values form a map), or of multiple
 * rows with the same id (values is an array of Serializable).
 * <p>
 * The id of the row is distinguished internally from other columns. For
 * fragments corresponding to created data, the initial id is a temporary one,
 * and it will be changed after database insert.
 */
public final class Row extends RowId implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT = 5;

    private enum OpaqueValue {
        OPAQUE_VALUE
    }

    /**
     * A database value we don't care about reading. When present in a fragment,
     * it won't be written, but any other value will be.
     */
    public static final Serializable OPAQUE = OpaqueValue.OPAQUE_VALUE;

    /**
     * The row keys, for single row.
     */
    protected String[] keys;

    /**
     * The row values.
     */
    public Serializable[] values;

    /**
     * The size of the allocated part of {@link #values}, for single rows.
     */
    protected int size;

    /** Copy constructor. */
    private Row(Row row) {
        super(row);
        keys = row.keys == null ? null : row.keys.clone();
        values = row.values == null ? null : row.values.clone();
        size = row.size;
    }

    @Override
    public Row clone() {
        return new Row(this);
    }

    /**
     * Constructs an empty {@link Row} for the given table with the given id
     * (may be {@code null}).
     */
    public Row(String tableName, Serializable id) {
        super(tableName, id);
        keys = new String[DEFAULT];
        values = new Serializable[DEFAULT];
        // size = 0;
    }

    /**
     * Constructs a new {@link Row} from a map.
     *
     * @param map the initial data to use
     */
    public Row(String tableName, Map<String, Serializable> map) {
        super(tableName, null); // id set through map
        keys = new String[map.size()];
        values = new Serializable[map.size()];
        // size = 0;
        for (Entry<String, Serializable> entry : map.entrySet()) {
            putNew(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Constructs a new {@link Row} from an array of values.
     *
     * @param array the initial data to use
     */
    public Row(String tableName, Serializable id, Serializable[] array) {
        super(tableName, id);
        values = array.clone();
        keys = null;
        size = -1;
    }

    public boolean isCollection() {
        return size == -1;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > values.length) {
            Serializable[] k = keys;
            Serializable[] d = values;
            int newCapacity = (values.length * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            keys = new String[newCapacity];
            values = new Serializable[newCapacity];
            System.arraycopy(d, 0, values, 0, size);
            System.arraycopy(k, 0, keys, 0, size);
        }
    }

    /**
     * Puts a key/value.
     *
     * @param key the key
     * @param value the value
     */
    public void put(String key, Serializable value) {
        if (key.equals(Model.MAIN_KEY)) {
            id = value;
            return;
        }
        // linear search but the array is small
        for (int i = 0; i < size; i++) {
            if (key.equals(keys[i])) {
                Serializable oldValue = values[i];
                if (oldValue instanceof Delta) {
                    Delta oldDelta = (Delta) oldValue;
                    if (value instanceof Delta) {
                        if (value != oldDelta) {
                            // add a delta to another delta
                            value = oldDelta.add((Delta) value);
                        }
                    } else if (oldDelta.getFullValue().equals(value)) {
                        // don't overwrite a delta with the full value
                        // that actually comes from it
                        return;
                    }
                }
                values[i] = value;
                return;
            }
        }
        ensureCapacity(size + 1);
        keys[size] = key;
        values[size++] = value;
    }

    /**
     * Puts a key/value, assuming the key is not already there.
     *
     * @param key the key
     * @param value the value
     */
    public void putNew(String key, Serializable value) {
        if (key.equals(Model.MAIN_KEY)) {
            id = value;
            return;
        }
        ensureCapacity(size + 1);
        keys[size] = key;
        values[size++] = value;
    }

    /**
     * Gets a value from a key.
     *
     * @param key the key
     * @return the value
     */
    public Serializable get(String key) {
        if (key.equals(Model.MAIN_KEY)) {
            return id;
        }
        // linear search but the array is small
        for (int i = 0; i < size; i++) {
            if (key.equals(keys[i])) {
                return values[i];
            }
        }
        return null;
    }

    /**
     * Gets the list of keys. The id is not included.
     */
    public List<String> getKeys() {
        List<String> list = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            list.add(keys[i]);
        }
        return list;
    }

    /**
     * Gets the list of values. The id is not included.
     */
    public List<Serializable> getValues() {
        List<Serializable> list = new ArrayList<Serializable>(size);
        for (int i = 0; i < size; i++) {
            list.add(values[i]);
        }
        return list;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append('(');
        buf.append(tableName);
        buf.append(", ");
        buf.append(id);
        if (size != -1) {
            // single row
            buf.append(", {");
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(keys[i]);
                buf.append('=');
                printValue(values[i], buf);
            }
            buf.append('}');
        } else {
            // multiple rows
            buf.append(", [");
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                printValue(values[i], buf);
            }
            buf.append(']');
        }
        buf.append(')');
        return buf.toString();
    }

    public static final int MAX_STRING = 100;

    public static final int MAX_ARRAY = 10;

    @SuppressWarnings("boxing")
    public static void printValue(Serializable value, StringBuilder buf) {
        if (value == null) {
            buf.append("NULL");
        } else if (value instanceof String) {
            String v = (String) value;
            if (v.length() > MAX_STRING) {
                v = v.substring(0, MAX_STRING) + "...(" + v.length()
                        + " chars)...";
            }
            buf.append('"');
            buf.append(v);
            buf.append('"');
        } else if (value instanceof Calendar) {
            Calendar cal = (Calendar) value;
            char sign;
            int offset = cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 60000;
            if (offset < 0) {
                offset = -offset;
                sign = '-';
            } else {
                sign = '+';
            }
            buf.append(String.format(
                    "Calendar(%04d-%02d-%02dT%02d:%02d:%02d.%03d%c%02d:%02d)",
                    cal.get(Calendar.YEAR), //
                    cal.get(Calendar.MONTH) + 1, //
                    cal.get(Calendar.DAY_OF_MONTH), //
                    cal.get(Calendar.HOUR_OF_DAY), //
                    cal.get(Calendar.MINUTE), //
                    cal.get(Calendar.SECOND), //
                    cal.get(Calendar.MILLISECOND), //
                    sign, offset / 60, offset % 60));
        } else if (value instanceof Binary) {
            buf.append("Binary(");
            buf.append(((Binary) value).getDigest());
            buf.append(')');
        } else if (value.getClass().isArray()) {
            Serializable[] v = (Serializable[]) value;
            buf.append('[');
            for (int i = 0; i < v.length; i++) {
                if (i > 0) {
                    buf.append(',');
                    if (i > MAX_ARRAY) {
                        buf.append("...(");
                        buf.append(v.length);
                        buf.append(" items)...");
                        break;
                    }
                }
                printValue(v[i], buf);
            }
            buf.append(']');
        } else {
            buf.append(value.toString());
        }
    }

}
