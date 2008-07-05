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
import java.util.Arrays;

/**
 * A type of fragment corresponding to several rows forming a collection.
 *
 * @author Florent Guillaume
 */
public class CollectionFragment extends Fragment {

    private static final long serialVersionUID = 1L;

    /** The collection actually holding the data. */
    private Serializable[] col;

    /**
     * Constructs an empty {@link CollectionFragment} of the given table with
     * the given id (which may be a temporary one).
     *
     * @param tableName the table name
     * @param id the id
     * @param state the initial state for the fragment
     * @param context the persistence context to which the row is tied, or
     *            {@code null}
     * @param col the initial collection data to use, or {@code null}
     */
    public CollectionFragment(String tableName, Serializable id, State state,
            PersistenceContextByTable context, Serializable[] col) {
        super(tableName, id, state, context);
        assert col != null; // for now
        this.col = col;
    }

    /**
     * Sets a value.
     *
     * @param value the value
     */
    public void set(Serializable[] value) {
        col = value.clone();
        markModified();
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public Serializable[] get() {
        return col.clone();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + tableName + ", id=" +
                getId() + ", " + Arrays.asList(col) + ')';
    }

}
