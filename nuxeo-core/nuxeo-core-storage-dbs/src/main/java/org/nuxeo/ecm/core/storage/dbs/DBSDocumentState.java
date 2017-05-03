/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.dbs;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_CHANGE_TOKEN;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_SYS_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_SERIES_ID;

import java.io.Serializable;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.StateHelper;
import org.nuxeo.ecm.core.storage.BaseDocument;
import org.nuxeo.ecm.core.storage.State;

/**
 * Implementation of a {@link Document} state for Document-Based Storage.
 * <p>
 * It wraps a {@link State}, together with a dirty flag.
 *
 * @since 5.9.4
 */
public class DBSDocumentState {

    private static final String UNDEFINED_PARENT_ID = "_undefined_";
    /**
     * The current state.
     */
    protected State state;

    /**
     * When non-null, the original state (otherwise the state hasn't been modified).
     */
    protected State originalState;

    private String id;

    private String parentId = UNDEFINED_PARENT_ID;

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
     * Checks if the document state has been changed since its construction or the last call to {@link #setNotDirty}.
     */
    public boolean isDirty() {
        return originalState != null;
    }

    public void setNotDirty() {
        originalState = null;
        StateHelper.resetDeltas(state);
    }

    /**
     * Gets the state. If the caller changes the state, it must also call {@link #dirty} to inform this object that the
     * state is dirtied.
     */
    public State getState() {
        return state;
    }

    /**
     * Gets a diff of what changed since this document state was read from database or saved.
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

    /**
     * Gets the original state for this, needed when creating an undo log.
     *
     * @return a state that must not be modified
     * @since 7.4
     */
    public State getOriginalState() {
        return originalState == null ? state : originalState;
    }

    public Serializable get(String key) {
        if (KEY_ID.equals(key)) {
            return getId();
        } else if (KEY_PARENT_ID.equals(key)) {
            return getParentId();
        }
        return state.get(key);
    }

    public void put(String key, Serializable value) {
        markDirty();

        if (KEY_ID.equals(key)) {
            id = (String) value;
        } else if (KEY_PARENT_ID.equals(key)) {
            parentId = (String) value;
        }
        state.put(key, value);
    }

    public boolean containsKey(String key) {
        return state.get(key) != null;
    }

    public String getId() {
        if (id == null) {
            id = (String) state.get(KEY_ID);
        }
        return id;
    }

    public String getParentId() {
        // use a marker because parentId can be null
        if (parentId == UNDEFINED_PARENT_ID) {
            parentId = (String) state.get(KEY_PARENT_ID);
        }
        return parentId;
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

    public Long getSysVersion() {
        return (Long) get(KEY_SYS_VERSION);
    }

    public Long getChangeToken() {
        return (Long) get(KEY_CHANGE_TOKEN);
    }

    public boolean validateChangeToken(String userChangeToken) {
        Long sysVersion = getSysVersion();
        Long changeToken = getChangeToken();
        return BaseDocument.validateUserChangeToken(sysVersion, changeToken, userChangeToken);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + (isDirty() ? "dirty," : "") + state.toString() + ')';
    }

}
