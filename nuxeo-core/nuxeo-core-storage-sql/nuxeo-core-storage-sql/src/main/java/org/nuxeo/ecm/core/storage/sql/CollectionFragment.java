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

    /** The collection actually holding the data. */
    public Serializable[] array;

    protected boolean dirty;

    /**
     * Constructs an empty {@link CollectionFragment} of the given table with
     * the given id (which may be a temporary one).
     *
     * @param id the id
     * @param state the initial state for the fragment
     * @param context the persistence context to which the row is tied, or
     *            {@code null}
     * @param array the initial collection data to use, or {@code null}
     */
    public CollectionFragment(Serializable id, State state, Context context,
            Serializable[] array) {
        super(id, state, context);
        assert array != null; // for now
        this.array = array;
    }

    /**
     * Sets a value.
     *
     * @param value the value
     */
    public void set(Serializable[] value) throws StorageException {
        // unless invalidated (in which case don't try to refetch the value just
        // to compare state), don't mark modified or dirty if there is no change
        if (getState() != State.INVALIDATED_MODIFIED) {
            // not invalidated, so no need to call accessed()
            if (Arrays.equals(array, value)) {
                return;
            }
        }
        array = value.clone();
        markModified();
        setDirty(true);
    }

    /**
     * Gets the value.
     *
     * @return the value
     * @throws StorageException
     */
    public Serializable[] get() throws StorageException {
        accessed();
        return array.clone();
    }

    @Override
    protected State refetch() throws StorageException {
        Context context = getContext();
        array = context.mapper.readCollectionArray(getId(), context);
        return State.PRISTINE;
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
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
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
        for (int i = 0; i < array.length; i++) {
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

}
