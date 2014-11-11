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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.db.Column;

/**
 * A type of fragment corresponding to several rows with the same id.
 *
 * @author Florent Guillaume
 */
public abstract class CollectionFragment extends Fragment {

    private static final long serialVersionUID = 1L;

    protected boolean dirty;

    public CollectionFragment(Serializable id, State state, Context context) {
        super(id, state, context);
    }

    /**
     * Sets a value.
     *
     * @param value the value
     */
    public abstract void set(Serializable[] value) throws StorageException;

    /**
     * Gets the value.
     *
     * @return the value
     * @throws StorageException
     */
    public abstract Serializable[] get() throws StorageException;

    /**
     * Interface for a class that knows how to build an array from a SQL result
     * set, and build the appropriate collection fragments.
     */
    protected interface CollectionMaker {

        Serializable[] makeArray(ResultSet rs, List<Column> columns,
                Context context, Model model) throws SQLException;

        /**
         * Makes arrays for multiple fragments. The result sets have to contain
         * values for the id column, and be ordered by id, pos.
         */
        Map<Serializable, Serializable[]> makeArrays(ResultSet rs,
                List<Column> columns, Context context, Model model)
                throws SQLException;

        CollectionFragment makeCollection(Serializable id,
                Serializable[] array, Context context);

        CollectionFragment makeEmpty(Serializable id, Context context,
                Model model);
    }

    /**
     * Gets a specialized iterator allowing setting of values to a SQL prepared
     * statement.
     */
    protected abstract CollectionFragmentIterator getIterator();

    protected interface CollectionFragmentIterator extends
            Iterator<Serializable> {

        /**
         * Sets the current value of the iterator to a SQL prepared statement.
         */
        void setToPreparedStatement(List<Column> columns, PreparedStatement ps,
                Model model, List<Serializable> debugValues)
                throws SQLException;
    }

    /**
     * Checks if the fragment is dirty (value changed since last clear).
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Sets the fragment's dirty state;
     */
    protected void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

}
