/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.Arrays;

import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowUpdate;

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
     * @param context the persistence context to which the fragment is tied, or {@code null}
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
    public RowUpdate getRowUpdate() {
        if (Arrays.equals(row.values, oldvalues)) {
            return null;
        }
        // check if we have a list append
        if (oldvalues == null) {
            // row.values != null otherwise we would have returned already
            return new RowUpdate(row, 0);
        } else if (row.values != null && isPrefix(oldvalues, row.values)) {
            return new RowUpdate(row, oldvalues.length);
        } else {
            // full update, row.values may be null
            return new RowUpdate(row);
        }
    }

    /**
     * Checks if the left array is a strict prefix of the right one.
     *
     * @since 8.3
     */
    public static boolean isPrefix(Serializable[] left, Serializable[] right) {
        if (left.length >= right.length) {
            return false;
        }
        for (int i = 0; i < left.length; i++) {
            Serializable o1 = left[i];
            Serializable o2 = right[i];
            if (!(o1 == null ? o2 == null : o1.equals(o2))) {
                return false;
            }
        }
        return true;
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
