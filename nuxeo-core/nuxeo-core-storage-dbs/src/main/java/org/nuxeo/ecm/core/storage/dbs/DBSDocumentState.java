/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.dbs;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_BINARY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_JOBID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_SIMPLE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_SERIES_ID;

import java.io.Serializable;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.StateHelper;
import org.nuxeo.ecm.core.storage.State;

/**
 * Implementation of a {@link Document} state for Document-Based Storage.
 * <p>
 * It wraps a {@link State}, together with a dirty flag.
 *
 * @since 5.9.4
 */
public class DBSDocumentState {

    /**
     * The current state.
     */
    protected State state;

    /**
     * When non-null, the original state (otherwise the state hasn't been
     * modified).
     */
    protected State originalState;

    /**
     * Constructs an empty state.
     */
    public DBSDocumentState() {
        state = new State();
        originalState = null;
    }

    /**
     * Constructs a document state from the copy of an existing base state.
     */
    public DBSDocumentState(State base) {
        state = StateHelper.deepCopy(base);
        originalState = null;
    }

    /**
     * This must be called if we're about to directly change the internal state.
     */
    public void markDirty() {
        if (originalState == null) {
            originalState = StateHelper.deepCopy(state);
        }
    }

    /**
     * Checks if the document state has been changed since its construction or
     * the last call to {@link #setNotDirty}.
     */
    public boolean isDirty() {
        return originalState != null;
    }

    public boolean isDirtyIgnoringFulltext() {
        StateDiff diff = getStateChange();
        if (diff == null) {
            return false;
        }
        diff.remove(KEY_FULLTEXT_SIMPLE);
        diff.remove(KEY_FULLTEXT_BINARY);
        diff.remove(KEY_FULLTEXT_JOBID);
        return !diff.isEmpty();
    }

    public void setNotDirty() {
        originalState = null;
    }

    /**
     * Gets the state. If the caller changes the state, it must also call
     * {@link #dirty} to inform this object that the state is dirtied.
     */
    public State getState() {
        return state;
    }

    /**
     * Gets a diff of what changed since this document state was read from
     * database or saved.
     *
     * @return {@code null} if there was no change, or a {@link StateDiff}
     */
    public StateDiff getStateChange() {
        if (originalState == null) {
            return null;
        }
        StateDiff diff = StateHelper.diff(originalState, state);
        if (diff.isEmpty()) {
            return null;
        }
        return diff;
    }

    public Serializable get(String key) {
        return state.get(key);
    }

    public void put(String key, Serializable value) {
        markDirty();
        state.put(key, value);
    }

    public boolean containsKey(String key) {
        return state.get(key) != null;
    }

    public String getId() {
        return (String) get(KEY_ID);
    }

    public String getParentId() {
        return (String) get(KEY_PARENT_ID);
    }

    public String getName() {
        return (String) get(KEY_NAME);
    }

    public String getPrimaryType() {
        return (String) get(KEY_PRIMARY_TYPE);
    }

    public String getVersionSeriesId() {
        return (String) get(KEY_VERSION_SERIES_ID);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + (isDirty() ? "dirty," : "")
                + state.toString() + ')';
    }

}
