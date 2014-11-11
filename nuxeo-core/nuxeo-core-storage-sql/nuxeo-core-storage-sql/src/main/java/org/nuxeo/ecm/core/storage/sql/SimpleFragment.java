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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A type of fragment corresponding to a single row in a table and its
 * associated in-memory information (state, dirty fields, attached context).
 */
public final class SimpleFragment extends Fragment {

    private static final long serialVersionUID = 1L;

    private static final Row UNKNOWN_ROW = new Row(null, (Serializable) null);

    public static final SimpleFragment UNKNOWN = new SimpleFragment(
            UNKNOWN_ROW, State.DETACHED, null);

    /**
     * Constructs a {@link SimpleFragment} from a {@link Row}.
     *
     * @param row the row, or {@code null}
     * @param state the initial state for the fragment
     * @param context the persistence context to which the fragment is tied, or
     *            {@code null}
     */
    public SimpleFragment(Row row, State state, PersistenceContext context) {
        super(row, state, context);
    }

    @Override
    protected State refetch() throws StorageException {
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
    protected State refetchDeleted() throws StorageException {
        row.size = 0;
        return State.ABSENT;
    }

    /**
     * Gets a value by key.
     *
     * @param key the key
     * @return the value
     */
    public Serializable get(String key) throws StorageException {
        accessed();
        return row.get(key);
    }

    /**
     * Puts a value by key.
     *
     * @param key the key
     * @param value the value
     */
    public void put(String key, Serializable value) throws StorageException {
        accessed(); // maybe refetch other values
        row.put(key, value);
        // resize olddata to follow row if needed
        if (oldvalues.length < row.values.length) {
            Serializable[] tmp = oldvalues;
            oldvalues = new Serializable[row.values.length];
            System.arraycopy(tmp, 0, oldvalues, 0, tmp.length);
        }
        markModified();
    }

    /**
     * Returns a {@code String} value.
     *
     * @param key the key
     * @return the value as a {@code String}
     * @throws ClassCastException if the value is not a {@code String}
     */
    public String getString(String key) throws StorageException {
        return (String) get(key);
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
                    keys = new LinkedList<String>();
                }
                keys.add(row.keys[i]);
            }
        }
        return keys == null ? Collections.<String> emptyList() : keys;
    }

    private static final boolean same(Object a, Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }

    /**
     * Comparator of {@link SimpleFragment}s according to their pos field.
     */
    public static class PositionComparator implements
            Comparator<SimpleFragment> {

        protected final String posKey;

        public PositionComparator(String posKey) {
            this.posKey = posKey;
        }

        public int compare(SimpleFragment frag1, SimpleFragment frag2) {
            try {
                Long pos1 = (Long) frag1.get(posKey);
                Long pos2 = (Long) frag2.get(posKey);
                if (pos1 == null && pos2 == null) {
                    // coherent sort
                    return frag1.hashCode() - frag2.hashCode();
                }
                if (pos1 == null) {
                    return 1;
                }
                if (pos2 == null) {
                    return -1;
                }
                return pos1.compareTo(pos2);
            } catch (StorageException e) {
                // shouldn't happen
                return frag1.hashCode() - frag2.hashCode();
            }
        }
    }

}
