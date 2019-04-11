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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.RowMapper.RowUpdate;

/**
 * A type of fragment corresponding to a single row in a table and its associated in-memory information (state, dirty
 * fields, attached context).
 */
public final class SimpleFragment extends Fragment {

    private static final long serialVersionUID = 1L;

    private static final Row UNKNOWN_ROW = new Row(null, (Serializable) null);

    public static final SimpleFragment UNKNOWN = new SimpleFragment(UNKNOWN_ROW, State.DETACHED, null);

    /**
     * Constructs a {@link SimpleFragment} from a {@link Row}.
     *
     * @param row the row, or {@code null}
     * @param state the initial state for the fragment
     * @param context the persistence context to which the fragment is tied, or {@code null}
     */
    public SimpleFragment(Row row, State state, PersistenceContext context) {
        super(row, state, context);
    }

    @Override
    protected State refetch() {
        Row newrow = context.mapper.readSimpleRow(row);
        if (newrow == null) {
            row.size = 0;
            return State.ABSENT;
        } else {
            row = newrow;
            clearDirty();
            return State.PRISTINE;
        }
    }

    @Override
    protected State refetchDeleted() {
        row.size = 0;
        return State.ABSENT;
    }

    /**
     * Gets a value by key.
     *
     * @param key the key
     * @return the value
     */
    public Serializable get(String key) {
        accessed();
        return row.get(key);
    }

    /**
     * Puts a value by key.
     *
     * @param key the key
     * @param value the value
     */
    public void put(String key, Serializable value) {
        accessed(); // maybe refetch other values
        row.put(key, value, oldvalues); // pass oldvalues to be able to deal with deltas
        // resize oldvalues to follow row if needed
        if (oldvalues.length < row.values.length) {
            Serializable[] tmp = oldvalues;
            oldvalues = new Serializable[row.values.length];
            System.arraycopy(tmp, 0, oldvalues, 0, tmp.length);
        }
        if (getState() != State.ABSENT || value != null) {
            // don't mark modified when setting null in an absent fragment
            // to avoid creating unneeded rows
            markModified();
        }
    }

    /**
     * Returns a {@code String} value.
     *
     * @param key the key
     * @return the value as a {@code String}
     * @throws ClassCastException if the value is not a {@code String}
     */
    public String getString(String key) {
        return (String) get(key);
    }

    @Override
    public RowUpdate getRowUpdate() {
        Collection<String> keys = getDirtyKeys();
        if (keys.isEmpty()) {
            return null;
        }
        return new RowUpdate(row, keys);
    }

    /**
     * Gets the dirty keys (keys of values changed since last clear).
     *
     * @return the dirty keys
     */
    public List<String> getDirtyKeys() {
        List<String> keys = null;
        for (int i = 0; i < row.size; i++) {
            if (!same(oldvalues[i], row.values[i])) {
                if (keys == null) {
                    keys = new LinkedList<>();
                }
                keys.add(row.keys[i]);
            }
        }
        return keys == null ? Collections.<String> emptyList() : keys;
    }

    private static boolean same(Object a, Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }

    /**
     * Comparator of {@link SimpleFragment}s according to a field.
     */
    public static class FieldComparator implements Comparator<SimpleFragment> {

        public final String key;

        public FieldComparator(String key) {
            this.key = key;
        }

        @Override
        public int compare(SimpleFragment frag1, SimpleFragment frag2) {
            return doCompare(frag1, frag2);
        }

        // separate function because we need a free generic type
        // which is incompatible with the super signature
        @SuppressWarnings("unchecked")
        public <T> int doCompare(SimpleFragment frag1, SimpleFragment frag2) {
            Comparable<T> value1 = (Comparable<T>) frag1.get(key);
            T value2 = (T) frag2.get(key);
            if (value1 == null && value2 == null) {
                // coherent sort
                return frag1.hashCode() - frag2.hashCode();
            }
            if (value1 == null) {
                return 1;
            }
            if (value2 == null) {
                return -1;
            }
            return value1.compareTo(value2);
        }
    }

}
