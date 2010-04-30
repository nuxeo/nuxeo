/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * The data of a single row in a table.
 * <p>
 * The content of the row is a mapping between keys and other values. The keys
 * correspond to schema fields, the values are scalars.
 */
public final class Row implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT = 5;

    /**
     * The id.
     */
    public Serializable id;

    /**
     * The row data. The array contains a sequence of:
     * <ul>
     * <li>key 1</li>
     * <li>value 1</li>
     * <li>key 2</li>
     * <li>value 2</li>
     * <li>...</li>
     * </ul>
     * The key is always a String (never {@code null}), the values are
     * {@link Serializable}.
     */
    protected Serializable[] data;

    /**
     * The size of the allocated part of {@link #data}.
     */
    protected int size;

    /**
     * Constructs an empty {@link Row} with the given id (may be {@code null}).
     */
    public Row(Serializable id) {
        this.id = id;
        data = new Serializable[2 * DEFAULT];
        // size = 0;
    }

    /**
     * Constructs a new {@link Row} from a map.
     *
     * @param map the initial data to use
     */
    public Row(Map<String, Serializable> map) {
        data = new Serializable[2 * map.size()];
        for (Entry<String, Serializable> entry : map.entrySet()) {
            putNew(entry.getKey(), entry.getValue());
        }
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > data.length) {
            Serializable[] old = data;
            int newCapacity = (data.length * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            data = new Serializable[newCapacity];
            System.arraycopy(old, 0, data, 0, size);
        }
    }

    /**
     * Puts a key/value.
     *
     * @param key the key
     * @param value the value
     * @return {@code true} if an old key was overwritten
     * @throws StorageException
     */
    public void put(String key, Serializable value) {
        if (key.equals(Model.MAIN_KEY)) {
            id = value;
            return;
        }
        // linear search but the array is small
        for (int i = 0; i < size; i += 2) {
            if (key.equals(data[i])) {
                data[i + 1] = value;
                return;
            }
        }
        ensureCapacity(size + 2);
        data[size++] = key;
        data[size++] = value;
    }

    /**
     * Puts a key/value, assuming the key is not already there.
     *
     * @param key the key
     * @param value the value
     * @throws StorageException
     */
    public void putNew(String key, Serializable value) {
        if (key.equals(Model.MAIN_KEY)) {
            id = value;
            return;
        }
        ensureCapacity(size + 2);
        data[size++] = key;
        data[size++] = value;
    }

    /**
     * Gets a value.
     *
     * @param key the key
     * @return the value
     * @throws StorageException
     */
    public Serializable get(String key) {
        if (key.equals(Model.MAIN_KEY)) {
            return id;
        }
        // linear search but the array is small
        for (int i = 0; i < size; i += 2) {
            if (key.equals(data[i])) {
                return data[i + 1];
            }
        }
        return null;
    }

    /**
     * Gets the list of keys. The id is not included.
     */
    public List<String> getKeys() {
        List<String> list = new ArrayList<String>(size / 2);
        for (int i = 0; i < size; i += 2) {
            list.add((String) data[i]);
        }
        return list;
    }

    /**
     * Gets the list of values. The id is not included.
     */
    public List<Serializable> getValues() {
        List<Serializable> list = new ArrayList<Serializable>(size / 2);
        for (int i = 0; i < size; i += 2) {
            list.add(data[i + 1]);
        }
        return list;
    }

    /**
     * Gets the data array. Only to be used read-only.
     */
    public Serializable[] getData() {
        return data;
    }

    /**
     * Gets the size of the allocated part of the data array.
     */
    public int getDataSize() {
        return size;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append("(id=");
        buf.append(id);
        for (int i = 0; i < size; i += 2) {
            buf.append(", ");
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
        buf.append(')');
        return buf.toString();
    }

}
