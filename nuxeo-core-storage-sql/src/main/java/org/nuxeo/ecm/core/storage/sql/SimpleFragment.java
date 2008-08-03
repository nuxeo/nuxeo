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
import java.util.HashMap;
import java.util.Map;

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

    /** The map actually holding the data. */
    private final Map<String, Serializable> map;

    /**
     * Constructs an empty {@link SimpleFragment} of the given table with the
     * given id (which may be a temporary one).
     *
     * @param tableName the table name
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
    }

    @Override
    protected State refetch() throws StorageException {
        Context context = getContext();
        Map<String, Serializable> newMap = context.mapper.readSingleRowMap(
                context.getTableName(), getId(), context);
        map.clear();
        if (newMap == null) {
            return State.ABSENT;
        } else {
            map.putAll(newMap);
            return State.PRISTINE;
        }
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
        modified();
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
