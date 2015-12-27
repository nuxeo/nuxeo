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
import java.util.Arrays;

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
    public void set(Serializable[] value) {
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
    public Serializable[] get() {
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
    protected State refetch() {
        row.values = context.mapper.readCollectionRowArray(row);
        clearDirty();
        return State.PRISTINE;
    }

    @Override
    protected State refetchDeleted() {
        row.values = context.model.getCollectionFragmentType(row.tableName).getEmptyArray();
        clearDirty();
        return State.PRISTINE;
    }

}
