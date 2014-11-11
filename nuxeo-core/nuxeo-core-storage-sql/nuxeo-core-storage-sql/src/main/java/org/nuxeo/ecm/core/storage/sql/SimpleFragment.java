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

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A type of fragment corresponding to a single row in a table.
 * <p>
 * The content of the row is a mapping between keys and other values. The keys
 * correspond to schema fields, the values can be simple or collection values.
 *
 * @author Florent Guillaume
 */
public class SimpleFragment extends Fragment {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SimpleFragment.class);

    public static final SimpleFragment UNKNOWN = new SimpleFragment(null,
            State.DETACHED, null, null);

    private enum OpaqueValue {
        OPAQUE_VALUE
    }

    /**
     * A database value we don't care about reading. When present in a fragment,
     * it won't be written, but any other value will be.
     */
    public static final Serializable OPAQUE = OpaqueValue.OPAQUE_VALUE;

    /**
     * The fragment data. The array contains a sequence of:
     * <ul>
     * <li>key 1</li>
     * <li>value 1</li>
     * <li>old value 1</li>
     * <li>key 2</li>
     * <li>value 2</li>
     * <li>old value 2</li>
     * <li>...</li>
     * </ul>
     * The key is always a String, the values are Serializable.
     */
    private Serializable[] data;

    /**
     * The size of the allocated part of {@link #data}.
     */
    private int size;

    /**
     * Constructs an empty {@link SimpleFragment} of the given table with the
     * given id (which may be a temporary one).
     *
     * @param id the id
     * @param state the initial state for the fragment
     * @param context the persistence context to which the row is tied, or
     *            {@code null}
     * @param map the initial row data to use, or {@code null}
     */
    public SimpleFragment(Serializable id, State state, Context context,
            Map<String, Serializable> map) {
        super(id, state, context);
        data = new Serializable[context == null ? 3
                : context.getTableSize() * 3];
        if (map != null) {
            addMap(map);
        }
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > data.length) {
            // should not happen, initial capacity is designed to fit the table
            // size
            log.warn("Had to extend capacity from " + data.length + " to fit "
                    + minCapacity + " for " + this);
            Serializable[] old = data;
            int newCapacity = (data.length * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            data = new Serializable[newCapacity];
            System.arraycopy(old, 0, data, 0, size);
        }
    }

    private void addMap(Map<String, Serializable> map) {
        ensureCapacity(3 * map.size());
        for (Entry<String, Serializable> entry : map.entrySet()) {
            data[size++] = entry.getKey();
            Serializable value = entry.getValue();
            data[size++] = value; // value
            data[size++] = value; // old value
        }
    }

    @Override
    protected State refetch() throws StorageException {
        Context context = getContext();
        // TODO make readSingleRowMap return something smaller than a HashMap
        Map<String, Serializable> map = context.mapper.readSingleRowMap(
                context.getTableName(), getId(), context);
        State state;
        if (map == null) {
            // clear all data (for GC)
            for (int i = 0; i < size; i++) {
                data[i] = null;
            }
            size = 0;
            state = State.ABSENT;
        } else {
            int oldSize = size;
            size = 0;
            addMap(map);
            // clear rest of data (for GC)
            for (int i = size; i < oldSize; i++) {
                data[i] = null;
            }
            state = State.PRISTINE;
        }
        return state;
    }

    /**
     * Puts a value.
     *
     * @param key the key
     * @param value the value
     * @throws StorageException
     */
    public void put(String key, Serializable value) throws StorageException {
        accessed(); // maybe refetch other values
        // linear search but the array is small
        for (int i = 0; i < size; i += 3) {
            if (key.equals(data[i])) {
                data[i + 1] = value;
                markModified();
                return;
            }
        }
        ensureCapacity(size + 3);
        data[size++] = key;
        data[size++] = value;
        data[size++] = null; // no old value
        markModified();
    }

    /**
     * Gets the dirty fields (fields changed since last clear).
     *
     * @return the dirty fields
     */
    public Collection<String> getDirty() {
        List<String> dirty = new LinkedList<String>();
        for (int i = 0; i < size; i += 3) {
            Serializable value = data[i + 1];
            Serializable oldValue = data[i + 2];
            if (value == null) {
                if (oldValue != null) {
                    dirty.add((String) data[i]);
                }
            } else if (!value.equals(oldValue)) {
                dirty.add((String) data[i]);
            }
        }
        return dirty;
    }

    /**
     * Clears the dirty fields.
     */
    public void clearDirty() {
        for (int i = 0; i < size; i += 3) {
            data[i + 2] = data[i + 1];
        }
    }

    /**
     * Gets a value.
     *
     * @param key the key
     * @return the value
     * @throws StorageException
     */
    public Serializable get(String key) throws StorageException {
        accessed();
        // linear search but the array is small
        for (int i = 0; i < size; i += 3) {
            if (key.equals(data[i])) {
                return data[i + 1];
            }
        }
        return null;
    }

    /**
     * Returns a {@code String} value.
     *
     * @param key the key
     * @return the value as a {@code String}
     * @throws StorageException
     * @throws ClassCastException if the value is not a {@code String}
     */
    public String getString(String key) throws StorageException {
        return (String) get(key);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append('(');
        buf.append(getTableName());
        buf.append(", id=");
        buf.append(getId());
        buf.append(", state=");
        buf.append(getState());
        buf.append(", ");
        buf.append('{');
        for (int i = 0; i < size; i += 3) {
            if (i != 0) {
                buf.append(", ");
            }
            buf.append(data[i]);
            buf.append('=');
            Serializable value = data[i + 1];
            boolean truncated = false;
            if (value instanceof String && ((String) value).length() > 100) {
                value = ((String) value).substring(0, 100);
                truncated = true;
            }
            buf.append(value);
            if (truncated) {
                buf.append("...");
            }
        }
        buf.append("})");
        return buf.toString();
    }

}
