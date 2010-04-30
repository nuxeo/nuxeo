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
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A type of fragment corresponding to a single row in a table and its
 * associated in-memory information (state, dirty fields, attached context).
 */
public final class SimpleFragment extends Fragment {

    private static final long serialVersionUID = 1L;

    private static final Row UNKNOWN_ROW = new Row((Serializable) null);

    public static final SimpleFragment UNKNOWN = new SimpleFragment(
            UNKNOWN_ROW, State.DETACHED, null);

    /**
     * The row data.
     */
    private Row row;

    /**
     * The fragment old data, from the time of construction / refetch. The size
     * of the allocated part of the array is following {@link #row.size}.
     */
    private Serializable[] olddata;

    /**
     * Constructs an empty {@link SimpleFragment} of the given table with the
     * given id (which may be a temporary one).
     *
     * @param id the id
     * @param state the initial state for the fragment
     * @param context the persistence context to which the row is tied, or
     *            {@code null}
     */
    public SimpleFragment(Serializable id, State state, Context context) {
        super(id, state, context);
        setRow(new Row(id));
    }

    /**
     * Constructs a {@link SimpleFragment} of the given table from a {@link Row}
     * .
     *
     * @param row the row, or {@code null}
     * @param state the initial state for the fragment
     * @param context the persistence context to which the row is tied, or
     *            {@code null}
     */
    public SimpleFragment(Row row, State state, Context context) {
        super(row.id, state, context);
        setRow(row);
    }

    private void setRow(Row row) {
        this.row = row;
        olddata = new Serializable[row.data.length / 2];
        clearDirty();
    }

    @Override
    public Serializable getId() {
        return row.id;
    }

    @Override
    public void setId(Serializable id) {
        row.id = id;
    }

    public Row getRow() {
        return row;
    }

    @Override
    protected State refetch() throws StorageException {
        Context context = getContext();
        Row row = context.mapper.readSingleRow(context.getTableName(), getId());
        if (row == null) {
            this.row.size = 0;
            return State.ABSENT;
        } else {
            setRow(row);
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
        row.put(key, value);
        markModified();
        // resize olddata to follow row if needed
        if (olddata.length < row.data.length / 2) {
            Serializable[] tmp = olddata;
            olddata = new Serializable[row.data.length / 2];
            System.arraycopy(tmp, 0, olddata, 0, tmp.length);
        }

    }

    /**
     * Gets the dirty fields (fields changed since last clear).
     *
     * @return the dirty fields
     */
    public List<String> getDirty() {
        List<String> dirty = new LinkedList<String>();
        for (int i = 0, r = 0; r < row.size; i++, r += 2) {
            if (!same(olddata[i], row.data[r + 1])) {
                dirty.add((String) row.data[r]);
            }
        }
        return dirty;
    }

    /**
     * Clears the dirty fields.
     */
    public void clearDirty() {
        for (int i = 0, r = 0; r < row.size; i++, r += 2) {
            olddata[i] = row.data[r + 1];
        }
    }

    private static final boolean same(Object a, Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
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
        return row.get(key);
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
        buf.append(", row=");
        buf.append(row.toString());
        buf.append(')');
        return buf.toString();
    }

}
