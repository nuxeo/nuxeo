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

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_SERIES_ID;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.storage.CopyHelper;
import org.nuxeo.ecm.core.storage.State;

/**
 * Implementation of a {@link Document} state for Document-Based Storage.
 * <p>
 * It wraps a {@link State}, together with a dirty flag.
 *
 * @since 5.9.4
 */
public class DBSDocumentState {

    protected State state;

    // dirty, but not including fulltext
    protected AtomicBoolean dirty = new AtomicBoolean(false);

    protected boolean dirtyFulltext;

    /**
     * Constructs an empty state.
     */
    public DBSDocumentState() {
        state = new State();
    }

    /**
     * Constructs a state from an existing base map.
     */
    public DBSDocumentState(State base) {
        state = CopyHelper.deepCopy(base);
    }

    /**
     * Copy constructor.
     */
    public DBSDocumentState(DBSDocumentState docState) {
        this(docState.state);
    }

    public AtomicBoolean getDirty() {
        return dirty;
    }

    public boolean isDirty() {
        return dirty.get() || dirtyFulltext;
    }

    public boolean isDirtyIgnoringFulltext() {
        return dirty.get();
    }

    public void setNotDirty() {
        dirty.set(false);
        dirtyFulltext = false;
    }

    public State getState() {
        return state;
    }

    public Serializable get(String key) {
        return state.get(key);
    }

    public void put(String key, Serializable value) {
        state.put(key, value);
        switch (key) {
        case DBSDocument.KEY_FULLTEXT_SIMPLE:
        case DBSDocument.KEY_FULLTEXT_BINARY:
        case DBSDocument.KEY_FULLTEXT_JOBID:
            dirtyFulltext = true;
            break;
        default:
            dirty.set(true);
        }
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
        return getClass().getSimpleName() + "(dirty=" + dirty + ','
                + state.toString() + ')';
    }

}
