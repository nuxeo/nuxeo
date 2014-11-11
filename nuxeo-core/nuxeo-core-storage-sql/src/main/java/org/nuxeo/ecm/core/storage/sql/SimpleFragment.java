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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    public static final SimpleFragment UNKNOWN = new SimpleFragment(null,
            State.DETACHED, null, null);

    private static enum OpaqueValue {
        OPAQUE_VALUE
    }

    /**
     * A database value we don't care about reading. When present in a fragment,
     * it won't be written, but any other value will be.
     */
    public static final Serializable OPAQUE = OpaqueValue.OPAQUE_VALUE;

    /** The map actually holding the data. */
    private final Map<String, Serializable> map;

    /** The previous values, to be able to find dirty fields. */
    private Map<String, Serializable> old;

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
        if (map == null) {
            map = new HashMap<String, Serializable>();
        }
        this.map = map;
        clearDirty();
    }

    @Override
    protected State refetch() throws StorageException {
        Context context = getContext();
        Map<String, Serializable> newMap = context.mapper.readSingleRowMap(
                context.getTableName(), getId(), context);
        map.clear();
        State state;
        if (newMap == null) {
            state = State.ABSENT;
        } else {
            map.putAll(newMap);
            state = State.PRISTINE;
        }
        clearDirty();
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
        map.put(key, value);
        markModified();
    }

    /**
     * Gets the dirty fields (fields changed since last clear).
     *
     * @return the dirty fields
     */
    public Collection<String> getDirty() {
        List<String> dirty = new LinkedList<String>();
        for (Entry<String, Serializable> entry : map.entrySet()) {
            String key = entry.getKey();
            Serializable value = entry.getValue();
            Serializable oldValue = old.get(key);
            if (value == null) {
                if (oldValue != null) {
                    dirty.add(key);
                }
            } else if (!value.equals(oldValue)) {
                dirty.add(key);
            }
        }
        return dirty;
    }

    /**
     * Clears the dirty fields.
     */
    @SuppressWarnings("unchecked")
    public void clearDirty() {
        old = (HashMap) ((HashMap) map).clone();
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
        return map.get(key);
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
        accessed();
        return (String) map.get(key);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getTableName() + ", id=" +
                getId() + ", " + map + ')';
    }

}
