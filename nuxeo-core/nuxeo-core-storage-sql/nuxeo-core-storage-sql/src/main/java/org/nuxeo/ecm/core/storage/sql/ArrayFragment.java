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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.db.Column;

/**
 * A type of fragment corresponding to several rows forming an array.
 *
 * @author Florent Guillaume
 */
public class ArrayFragment extends CollectionFragment {

    private static final long serialVersionUID = 1L;

    /** The collection actually holding the data. */
    protected Serializable[] array;

    /**
     * Constructs an empty {@link ArrayFragment} of the given table with the
     * given id (which may be a temporary one).
     *
     * @param id the id
     * @param state the initial state for the fragment
     * @param context the persistence context to which the row is tied, or
     *            {@code null}
     * @param array the initial collection data to use, or {@code null}
     */
    public ArrayFragment(Serializable id, State state, Context context,
            Serializable[] array) {
        super(id, state, context);
        assert array != null; // for now
        this.array = array;
    }

    @Override
    protected State refetch() throws StorageException {
        Context context = getContext();
        array = context.mapper.readCollectionArray(getId(), context);
        return State.PRISTINE;
    }

    @Override
    public void set(Serializable[] value) {
        // no need to call accessed() as we overwrite all
        array = value.clone();
        markModified();
    }

    @Override
    public Serializable[] get() throws StorageException {
        accessed();
        return array.clone();
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
        buf.append('[');
        for (int i=0; i < array.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            Serializable value = array[i];
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
        buf.append("])");
        return buf.toString();
    }

    protected static final CollectionMaker MAKER = new CollectionMaker() {

        public Serializable[] makeArray(Serializable id, ResultSet rs,
                List<Column> columns, Context context, Model model)
                throws SQLException {
            // find the column containing the value
            // (the pos column is ignored, results are ordered)
            Column column = null;
            for (Column col : columns) {
                if (col.getKey().equals(model.COLL_TABLE_VALUE_KEY)) {
                    column = col;
                }
            }
            if (column == null) {
                throw new AssertionError(columns);
            }
            List<Serializable> list = new ArrayList<Serializable>();
            while (rs.next()) {
                int i = 0;
                Serializable value = null;
                for (Column col : columns) {
                    i++;
                    if (col == column) {
                        value = column.getFromResultSet(rs, i);
                    }
                }
                list.add(value);
            }
            return column.listToArray(list);
        }

        public CollectionFragment makeCollection(Serializable id,
                Serializable[] array, Context context) {
            return new ArrayFragment(id, State.PRISTINE, context, array);
        }

        public CollectionFragment makeEmpty(Serializable id, Context context,
                Model model) {
            Serializable[] empty = model.getCollectionFragmentType(
                    context.getTableName()).getEmptyArray();
            return new ArrayFragment(id, State.CREATED, context, empty);
        }
    };

    @Override
    protected CollectionFragmentIterator getIterator() {
        return new ArrayFragmentIterator();
    }

    /**
     * This iterator assumes no concurrent changes to the array.
     */
    protected class ArrayFragmentIterator implements CollectionFragmentIterator {

        protected int i;

        public ArrayFragmentIterator() {
            i = -1;
        }

        public boolean hasNext() {
            return array.length > i + 1;
        }

        public Serializable next() {
            i++;
            return array[i];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void setToPreparedStatement(List<Column> columns,
                PreparedStatement ps, Model model,
                List<Serializable> debugValues) throws SQLException {
            int n = 0;
            for (Column column : columns) {
                n++;
                String key = column.getKey();
                Serializable v;
                if (key.equals(model.MAIN_KEY)) {
                    v = getId();
                } else if (key.equals(model.COLL_TABLE_POS_KEY)) {
                    v = (long) i;
                } else if (key.equals(model.COLL_TABLE_VALUE_KEY)) {
                    v = array[i];
                } else {
                    throw new AssertionError(key);
                }
                column.setToPreparedStatement(ps, n, v);
                if (debugValues != null) {
                    debugValues.add(v);
                }
            }
        }

    }
}
