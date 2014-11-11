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
import java.util.Arrays;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A type of fragment corresponding to several rows with the same id.
 */
public class CollectionFragment extends Fragment {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@link CollectionFragment} from a {@link Row}.
     *
     * @param row the row
     * @param state the initial state for the fragment
     * @param context the persistence context to which the fragment is tied, or
     *            {@code null}
     */
    public CollectionFragment(Row row, State state, PersistenceContext context) {
        super(row, state, context);
    }

    /**
     * Sets a collection value.
     *
     * @param value the value
     */
    public void set(Serializable[] value) throws StorageException {
        // unless invalidated (in which case don't try to refetch the value just
        // to compare state), don't mark modified or dirty if there is no change
        if (getState() != State.INVALIDATED_MODIFIED) {
            // not invalidated, so no need to call accessed()
            if (Arrays.equals(row.values, value)) {
                return;
            }
        }
        row.values = value.clone();
        markModified();
    }

    /**
     * Gets the collection value.
     *
     * @return the value
     */
    public Serializable[] get() throws StorageException {
        accessed();
        return row.values.clone();
    }

    /**
     * Checks if the array is dirty (values changed since last clear).
     *
     * @return {@code true} if the array changed
     */
    public boolean isDirty() {
        return !Arrays.equals(row.values, oldvalues);
    }

    @Override
    protected State refetch() throws StorageException {
        row.values = context.mapper.readCollectionRowArray(row);
        clearDirty();
        return State.PRISTINE;
    }

    @Override
    protected State refetchDeleted() throws StorageException {
        row.values = context.model.getCollectionFragmentType(row.tableName).getEmptyArray();
        clearDirty();
        return State.PRISTINE;
    }

}
