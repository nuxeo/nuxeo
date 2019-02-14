/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.Objects;

import org.nuxeo.ecm.core.api.model.Delta;

/**
 * The data of a single row in a table (keys/values form a map), or of multiple rows with the same id (values is an
 * array of Serializable).
 * <p>
 * The id of the row is distinguished internally from other columns. For fragments corresponding to created data, the
 * initial id is a temporary one, and it will be changed after database insert.
 */
public final class Row extends RowId implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT = 5;

    private enum OpaqueValue {
        OPAQUE_VALUE
    }

    /**
     * A database value we don't care about reading. When present in a fragment, it won't be written, but any other
     * value will be.
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
     * Constructs an empty {@link Row} for the given table with the given id (may be {@code null}).
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
     * Puts a key/value. Does not deal with deltas.
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
                values[i] = value;
                return;
            }
        }
        ensureCapacity(size + 1);
        keys[size] = key.intern();
        values[size++] = value;
    }

    /**
     * Puts a key/value where the current or new value may be a delta. To resolve a delta, the oldvalues (in-database
     * state) must be consulted.
     *
     * @param key the key
     * @param value the value
     * @param oldvalues the old values
     */
    public void put(String key, Serializable value, Serializable[] oldvalues) {
        if (key.equals(Model.MAIN_KEY)) {
            id = value;
            return;
        }
        // linear search but the array is small
        for (int i = 0; i < size; i++) {
            if (key.equals(keys[i])) {
                if (value instanceof Delta) {
                    // the new value is a delta
                    Delta delta = (Delta) value;
                    Serializable deltaBase = delta.getBase();
                    Serializable oldValue = oldvalues[i];
                    if (!Objects.equals(oldValue, deltaBase)) {
                        // delta's base is not the in-database value
                        // -> set a new value, don't use a delta update
                        value = delta.getFullValue();
                    }
                    // else delta's base is the in-database value
                    // because base is consistent with old value, assume the delta is already properly computed
                }
                // else use the new non-delta value
                values[i] = value;
                return;
            }
        }
        ensureCapacity(size + 1);
        keys[size] = key.intern();
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
        keys[size] = key.intern();
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
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(keys[i]);
        }
        return list;
    }

    /**
     * Gets the list of values. The id is not included.
     */
    public List<Serializable> getValues() {
        List<Serializable> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(values[i]);
        }
        return list;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append('(');
        sb.append(tableName);
        sb.append(", ");
        sb.append(id);
        if (size != -1) {
            // single row
            sb.append(", {");
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(keys[i]);
                sb.append('=');
                printValue(values[i], sb);
            }
            sb.append('}');
        } else {
            // multiple rows
            sb.append(", [");
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                printValue(values[i], sb);
            }
            sb.append(']');
        }
        sb.append(')');
        return sb.toString();
    }

    public static final int MAX_STRING = 100;

    public static final int MAX_ARRAY = 10;

    @SuppressWarnings("boxing")
    public static void printValue(Serializable value, StringBuilder sb) {
        if (value == null) {
            sb.append("NULL");
        } else if (value instanceof String) {
            String v = (String) value;
            if (v.length() > MAX_STRING) {
                v = v.substring(0, MAX_STRING) + "...(" + v.length() + " chars)...";
            }
            sb.append('"');
            sb.append(v);
            sb.append('"');
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
            sb.append(String.format("Calendar(%04d-%02d-%02dT%02d:%02d:%02d.%03d%c%02d:%02d)", cal.get(Calendar.YEAR), //
                    cal.get(Calendar.MONTH) + 1, //
                    cal.get(Calendar.DAY_OF_MONTH), //
                    cal.get(Calendar.HOUR_OF_DAY), //
                    cal.get(Calendar.MINUTE), //
                    cal.get(Calendar.SECOND), //
                    cal.get(Calendar.MILLISECOND), //
                    sign, offset / 60, offset % 60));
        } else if (value.getClass().isArray()) {
            Serializable[] v = (Serializable[]) value;
            sb.append('[');
            for (int i = 0; i < v.length; i++) {
                if (i > 0) {
                    sb.append(',');
                    if (i > MAX_ARRAY) {
                        sb.append("...(");
                        sb.append(v.length);
                        sb.append(" items)...");
                        break;
                    }
                }
                printValue(v[i], sb);
            }
            sb.append(']');
        } else {
            sb.append(value.toString());
        }
    }

}
