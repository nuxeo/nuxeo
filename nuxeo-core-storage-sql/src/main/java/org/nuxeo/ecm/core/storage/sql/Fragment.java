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
 * A rich value corresponding to one or more rows in a table.
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

    /** The fragment is not attached to a persistence context. */
    protected static final int STATE_DETACHED = 0;

    /** The fragment is in the created map in the persistence context. */
    protected static final int STATE_CREATED = 1;

    /** The fragment is in the pristine map in the persistence context. */
    protected static final int STATE_PRISTINE = 2;

    /** The fragment is in the modified map in the persistence context. */
    protected static final int STATE_MODIFIED = 3;

    /** The fragment is in the deleted map in the persistence context. */
    protected static final int STATE_DELETED = 4;

    private transient int state; // defaults to STATE_DETACHED = 0

    private transient PersistenceContextByTable context;

    /**
     * Constructs an empty {@link Fragment} of the given table with the given id
     * (which may be a temporary one).
     *
     * @param tableName the table name
     * @param id the id
     * @param context the persistence context to which the fragment is tied, or
     *            {@code null}
     * @param creation {@code true} if this fragment has just been created
     */
    public Fragment(String tableName, Serializable id,
            PersistenceContextByTable context, boolean creation) {
        assert tableName != null;
        this.tableName = tableName;
        this.id = id;
        this.context = context;
        if (context == null) {
            state = STATE_DETACHED;
        } else if (creation) {
            state = STATE_CREATED;
        } else {
            state = STATE_PRISTINE;
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
        assert state == STATE_CREATED;
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
        state = STATE_DETACHED;
        context = null;
    }

    /**
     * Marks the fragment pristine, called after a save.
     */
    protected void markPristine() {
        assert state == STATE_CREATED || state == STATE_MODIFIED;
        state = STATE_PRISTINE;
    }

    /**
     * Marks the fragment deleted, called after a delete.
     */
    protected void markDeleted() {
        if (state == STATE_PRISTINE || state == STATE_MODIFIED) {
            state = STATE_DELETED;
        } else if (state == STATE_CREATED) {
            state = STATE_DETACHED;
            context = null;
        }
        // it may be possible to delete detached fragments, so don't fail
    }

    /**
     * Marks the fragment modified, called internally after a put.
     */
    protected void markModified() {
        if (state == STATE_PRISTINE) {
            state = STATE_MODIFIED;
            context.markModified(this);
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

    public final SingleRow hier;

    public final SingleRow main;

    public final FragmentsMap fragments;

    public FragmentGroup(SingleRow main, SingleRow hier, FragmentsMap fragments) {
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
