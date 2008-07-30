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

import org.nuxeo.ecm.core.storage.sql.db.Column;

/**
 * A type of fragment corresponding to several rows with the same id.
 *
 * @author Florent Guillaume
 */
public abstract class CollectionFragment extends Fragment {

    private static final long serialVersionUID = 1L;

    public CollectionFragment(String tableName, Serializable id, State state,
            PersistenceContextByTable context) {
        super(tableName, id, state, context);
    }

    /**
     * Sets a value.
     *
     * @param value the value
     */
    public abstract void set(Serializable[] value);

    /**
     * Gets the value.
     *
     * @return the value
     */
    public abstract Serializable[] get();

    /**
     * Gets a string representing just the collection (for debug).
     */
    public abstract String toSimpleString();

    /**
     * Interface for a class that knows how to build collection fragments from a
     * SQL result set.
     */
    public static interface CollectionFragmentMaker {

        CollectionFragment make(String tableName, Serializable id,
                ResultSet rs, List<Column> columns,
                PersistenceContextByTable context, Model model)
                throws SQLException;

        CollectionFragment makeEmpty(String tableName, Serializable id,
                PersistenceContextByTable context, Model model);
    }

    /**
     * Gets a specialized iterator allowing setting of values to a SQL prepared
     * statement.
     */
    public abstract CollectionFragmentIterator getIterator();

    public static interface CollectionFragmentIterator extends
            Iterator<Serializable> {

        /**
         * Sets the current value of the iterator to a SQL prepared statement.
         */
        void setToPreparedStatement(List<Column> columns, PreparedStatement ps,
                Model model, List<Serializable> debugValues)
                throws SQLException;
    }

}
