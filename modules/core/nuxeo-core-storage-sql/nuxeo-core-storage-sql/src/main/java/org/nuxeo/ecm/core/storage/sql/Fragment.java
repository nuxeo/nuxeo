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
import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowUpdate;

/**
 * A rich value corresponding to one row or a collection of rows in a table.
 * <p>
 * In addition to the basic {@link Row}, this holds the old values (to check dirty state), the state and a reference to
 * the session.
 * <p>
 * This class has two kinds of state-changing methods:
 * <ul>
 * <li>the "set" ones, which only change the state,</li>
 * <li>the "mark" ones, which change the state and do the corresponding changes in the pristine/modified maps of the
 * context.</li>
 * <li></li>
 * </ul>
 *
 * @author Florent Guillaume
 */
public abstract class Fragment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The possible states of a fragment.
     */
    public enum State {

        /**
         * The fragment is not attached to a persistence context.
         */
        DETACHED, // first is default

        /**
         * The fragment has been read and found to be absent in the database. It contains default data (usually
         * {@code null}). It lives in the context's pristine map. Upon modification, the state will change to
         * {@link #CREATED}.
         */
        ABSENT,

        /**
         * The fragment exists in the database but hasn't been changed yet. It lives in the context's pristine map. Upon
         * modification, the state will change to {@link #MODIFIED}.
         */
        PRISTINE,

        /**
         * The fragment does not exist in the database and will be inserted upon save. It lives in the context's
         * modified map. Upon save it will be inserted in the database and the state will change to {@link #PRISTINE}.
         */
        CREATED,

        /**
         * The fragment has been modified. It lives in the context's modified map. Upon save the database will be
         * updated and the state will change to {@link #PRISTINE}.
         */
        MODIFIED,

        /**
         * The fragment has been deleted. It lives in the context's modified map. Upon save it will be deleted from the
         * database and the state will change to {@link #DETACHED}.
         */
        DELETED,

        /**
         * The fragment has been deleted as a consequence of another fragment being deleted (cascade). It lives in the
         * context's modified map. Upon save it will be implicitly deleted from the database by the deletion of a
         * {@link #DELETED} fragment, and the state will change to {@link #DETACHED}.
         */
        DELETED_DEPENDENT,

        /**
         * The fragment has been invalidated by a modification or creation. Any access must refetch it. It lives in the
         * context's pristine map.
         */
        INVALIDATED_MODIFIED,

        /**
         * The fragment has been invalidated by a deletion. It lives in the context's pristine map.
         */
        INVALIDATED_DELETED
    }

    /**
     * The row holding the data.
     */
    protected Row row;

    /**
     * The row old values, from the time of construction / refetch. The size of the the array is following {@link #row.values.length}.
     */
    protected Serializable[] oldvalues;

    private State state; // default is DETACHED

    protected PersistenceContext context;

    /**
     * Constructs a {@link Fragment} from a {@link Row}.
     *
     * @param row the row
     * @param state the initial state for the fragment
     * @param context the persistence context to which the fragment is tied, or {@code null}
     */
    protected Fragment(Row row, State state, PersistenceContext context) {
        this.row = row;
        this.state = state;
        this.context = context;
        switch (state) {
        case DETACHED:
            if (context != null) {
                throw new IllegalArgumentException();
            }
            break;
        case CREATED:
        case DELETED:
        case DELETED_DEPENDENT:
            context.setFragmentModified(this); // not in pristine
            break;
        case ABSENT:
        case PRISTINE:
            context.setFragmentPristine(this); // not in modified
            break;
        case MODIFIED:
        case INVALIDATED_MODIFIED:
        case INVALIDATED_DELETED:
            throw new IllegalArgumentException(state.toString());
        }
        clearDirty();
    }

    /**
     * Gets the state.
     *
     * @return the state
     */
    public State getState() {
        return state;
    }

    /**
     * Sets the id. This only used at most once to change a temporary id to the persistent one.
     *
     * @param id the new persistent id
     */
    public void setId(Serializable id) {
        row.id = id;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Serializable getId() {
        return row.id;
    }

    /**
     * Clears the dirty state.
     */
    public void clearDirty() {
        // turn back deltas into full values
        Serializable[] values = row.values;
        int len = values.length;
        for (int i = 0; i < len; i++) {
            Serializable ob = values[i];
            if (ob instanceof Delta) {
                values[i] = ((Delta) ob).getFullValue();
            }
        }
        // clone to clear the dirty state
        oldvalues = values.clone();
    }

    /**
     * Returns the row update to do in the database to write this value.
     *
     * @return a row update, or {@code null} if the value is unchanged since last clear
     * @since 8.3
     */
    public abstract RowUpdate getRowUpdate();

    /**
     * Refetches this fragment from the database. Needed when an invalidation has been received and the fragment is
     * accessed again.
     *
     * @return the new state, {@link State#PRISTINE} or {@link State#ABSENT}
     */
    protected abstract State refetch();

    /**
     * Resets the data for a fragment that was invalidated by deletion.
     *
     * @return the new state, {@link State#PRISTINE} or {@link State#ABSENT}
     */
    protected abstract State refetchDeleted();

    /**
     * Checks that access to the fragment is possible. Called internally before a get, so that invalidated fragments can
     * be refetched.
     */
    protected void accessed() {
        switch (state) {
        case DETACHED:
        case ABSENT:
        case PRISTINE:
        case CREATED:
        case MODIFIED:
        case DELETED:
        case DELETED_DEPENDENT:
            break;
        case INVALIDATED_MODIFIED:
            state = refetch();
            break;
        case INVALIDATED_DELETED:
            state = refetchDeleted();
        }
    }

    /**
     * Marks the fragment modified. Called internally after a put/set.
     */
    protected void markModified() {
        switch (state) {
        case ABSENT:
            context.setFragmentModified(this);
            state = State.CREATED;
            break;
        case INVALIDATED_MODIFIED:
            // can only happen if overwrite all invalidated (array)
            // fall through
        case PRISTINE:
            context.setFragmentModified(this);
            state = State.MODIFIED;
            break;
        case DETACHED:
        case CREATED:
        case MODIFIED:
        case DELETED:
        case DELETED_DEPENDENT:
            break;
        case INVALIDATED_DELETED:
            throw new ConcurrentModificationException("Modifying a concurrently deleted value");
        }
    }

    /**
     * Marks the fragment deleted. Called after a remove.
     */
    protected void setDeleted(boolean primary) {
        switch (state) {
        case DETACHED:
            break;
        case ABSENT:
        case INVALIDATED_DELETED:
        case CREATED:
            context = null;
            state = State.DETACHED;
            break;
        case PRISTINE:
        case INVALIDATED_MODIFIED:
            state = primary ? State.DELETED : State.DELETED_DEPENDENT;
            break;
        case MODIFIED:
            state = primary ? State.DELETED : State.DELETED_DEPENDENT;
            break;
        case DELETED:
        case DELETED_DEPENDENT:
            throw new RuntimeException(this.toString());
        }
    }

    /**
     * Detaches the fragment from its persistence context. The caller makes sure that the fragment is removed from the
     * context map.
     */
    protected void setDetached() {
        state = State.DETACHED;
        context = null;
    }

    /**
     * Sets the (created/modified) fragment in the pristine state. Called after a save.
     */
    protected void setPristine() {
        switch (state) {
        case CREATED:
        case MODIFIED:
            state = State.PRISTINE;
            break;
        case ABSENT:
        case PRISTINE:
        case DELETED:
        case DELETED_DEPENDENT:
        case DETACHED:
        case INVALIDATED_MODIFIED:
        case INVALIDATED_DELETED:
            // incoherent with the pristine map + expected state
            throw new RuntimeException(this.toString());
        }
    }

    /**
     * Sets the fragment in the "invalidated from a modification" state. This is called:
     * <ul>
     * <li>when a database operation does non-tracked changes, which means that on access a refetch will be needed,
     * <li>during post-commit invalidation.
     * </ul>
     */
    protected void setInvalidatedModified() {
        switch (state) {
        case ABSENT:
        case PRISTINE:
        case CREATED:
        case MODIFIED:
        case DELETED:
        case DELETED_DEPENDENT:
            state = State.INVALIDATED_MODIFIED;
            break;
        case INVALIDATED_MODIFIED:
        case INVALIDATED_DELETED:
            break;
        case DETACHED:
            throw new RuntimeException(this.toString());
        }
    }

    /**
     * Sets the fragment in the "invalidated from a deletion" state. This is called:
     * <ul>
     * <li>when a database operation does a delete,
     * <li>during post-commit invalidation.
     * </ul>
     */
    protected void setInvalidatedDeleted() {
        switch (state) {
        case ABSENT:
        case PRISTINE:
        case CREATED:
        case MODIFIED:
        case DELETED:
        case DELETED_DEPENDENT:
        case INVALIDATED_MODIFIED:
            state = State.INVALIDATED_DELETED;
            break;
        case INVALIDATED_DELETED:
            break;
        case DETACHED:
            throw new RuntimeException(this.toString());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("(row=");
        sb.append(row);
        sb.append(", state=");
        sb.append(getState());
        sb.append(')');
        return sb.toString();
    }
}

/**
 * A fragments map holds all {@link Fragment}s for non-main tables.
 */
class FragmentsMap extends HashMap<String, Fragment> {

    private static final long serialVersionUID = 1L;

}

/**
 * Utility class grouping a main {@link Fragment} with a related hierarchy {@link Fragment} and additional fragments.
 * <p>
 * If the main and hierarchy tables are not separate, then the hierarchy fragment is unused.
 * <p>
 * This is all the data needed to describe a {@link Node}.
 */
class FragmentGroup {

    public final SimpleFragment hier;

    public final FragmentsMap fragments;

    public FragmentGroup(SimpleFragment hier, FragmentsMap fragments) {
        this.hier = hier;
        this.fragments = fragments;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + hier + ", " + fragments + ')';
    }
}
