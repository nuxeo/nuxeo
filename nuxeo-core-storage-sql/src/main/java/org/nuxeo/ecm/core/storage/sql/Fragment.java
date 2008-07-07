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
import java.util.HashMap;

/**
 * A rich value corresponding to one row or a collection of rows in a table.
 * <p>
 * The table is identified by its table name, which the {@link Mapper} knows
 * about.
 * <p>
 * The id of the fragment is distinguished internally from other columns. For
 * fragments corresponding to created data, the initial id is a temporary one,
 * and it will be changed after database insert.
 *
 * @author Florent Guillaume
 */
public abstract class Fragment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The table name. */
    protected final String tableName;

    /**
     * The id. If the fragment was just created, the initial temporary id will
     * be changed at save time to its final value.
     */
    private Serializable id;

    /**
     * The possible states of a fragment.
     */
    public static enum State {
        /**
         * The fragment is not attached to a persistence context.
         */
        DETACHED, // first is default

        /**
         * The fragment has been read and found to be absent in the database. It
         * contains default information. Upon modification, it will change to
         * {@link #CREATED}.
         */
        ABSENT,

        /**
         * The fragment does not exist in the database and will be inserted upon
         * save.
         */
        CREATED,

        /**
         * The fragment exists in the database but hasn't been changed yet. Upon
         * modification, it will change to {@link #MODIFIED}.
         */
        PRISTINE,

        /**
         * The fragment has been modified. Upon save the database will be
         * updated.
         */
        MODIFIED,

        /**
         * The fragment has been deleted. Upon save it will be deleted from the
         * database.
         */
        DELETED;
    }

    private transient State state; // default is State.DETACHED;

    private transient PersistenceContextByTable context;

    /**
     * Constructs an empty {@link Fragment} of the given table with the given id
     * (which may be a temporary one).
     *
     * @param tableName the table name
     * @param id the id
     * @param state the initial state for the fragment
     * @param context the persistence context to which the fragment is tied, or
     *            {@code null}
     */
    public Fragment(String tableName, Serializable id, State state,
            PersistenceContextByTable context) {
        this.tableName = tableName;
        this.id = id;
        this.state = state;
        this.context = context;
        switch (state) {
        case DETACHED:
            assert context == null;
            break;
        case ABSENT:
            context.newAbsent(this);
            break;
        case CREATED:
            context.newCreated(this);
            break;
        case PRISTINE:
            context.newPristine(this);
            break;
        case MODIFIED:
        case DELETED:
            throw new IllegalArgumentException(state.toString());
        }
    }

    /**
     * Gets the table name.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the id. This only used at most once to change a temporary id to the
     * persistent one.
     *
     * @param id the new persistent id
     */
    public void setId(Serializable id) {
        assert state == State.CREATED;
        assert id != null;
        this.id = id;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Serializable getId() {
        return id;
    }

    /**
     * Detaches the fragment from its persistence context.
     */
    protected void detach() {
        state = State.DETACHED;
        context = null;
    }

    /**
     * Marks the fragment pristine. Called after a save.
     */
    protected void markPristine() {
        switch (state) {
        case CREATED:
        case MODIFIED:
            state = State.PRISTINE;
            break;
        case ABSENT:
        case PRISTINE:
        case DELETED:
        case DETACHED:
            throw new IllegalStateException(this.toString());
        }
    }

    /**
     * Marks the fragment deleted. Called after a delete.
     */
    protected void markDeleted() {
        switch (state) {
        case ABSENT:
        case CREATED:
            state = State.DETACHED;
            break;
        case PRISTINE:
        case MODIFIED:
            state = State.DELETED;
            break;
        case DELETED:
        case DETACHED:
            throw new IllegalStateException(this.toString());
        }
    }

    /**
     * Marks the fragment modified. Called internally after a put/set.
     */
    protected void markModified() {
        switch (state) {
        case PRISTINE:
            state = State.MODIFIED;
            context.markPristineModified(this);
            break;
        case ABSENT:
            state = State.CREATED;
            context.markAbsentCreated(this);
            break;
        case CREATED:
        case MODIFIED:
        case DELETED:
        case DETACHED:
            break;
        }
    }

}

/**
 * A fragments map holds all {@link Fragment}s for non-main tables.
 */
class FragmentsMap extends HashMap<String, Fragment> {

    private static final long serialVersionUID = 1L;

}

/**
 * Utility class grouping a main {@link Fragment} with a related hierarchy
 * {@link Fragment} and additional fragments.
 * <p>
 * This is all the data needed to describe a {@link Node}.
 */
class FragmentGroup {

    public final SimpleFragment hier;

    public final SimpleFragment main;

    public final FragmentsMap fragments;

    public FragmentGroup(SimpleFragment main, SimpleFragment hier,
            FragmentsMap fragments) {
        this.main = main;
        this.hier = hier;
        this.fragments = fragments;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + hier + ", " + main + ", " +
                fragments + ')';
    }

}
