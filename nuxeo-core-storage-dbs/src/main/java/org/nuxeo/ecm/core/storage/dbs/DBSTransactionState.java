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
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_POS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;

/**
 * Transactional state for a session: data that has been modified in the
 * session, but is not yet committed and therefore should not be seen by other
 * sessions.
 * <p>
 * There are two kinds of data persistence, that have different interactions
 * with searches:
 * <ul>
 * <li>the "transient" data is data modified by saveDocument and createDocument,
 * which is not yet visible to a search,
 * <li>the "saved" data is all the transient data flushed by save, move, copy,
 * remove, checkIn, and is visible to a search.
 * </ul>
 * On commit, the saved state state is written to the repository. On rollback,
 * it's forgotten.
 *
 * @since 5.9.4
 */
public class DBSTransactionState {

    private static final Log log = LogFactory.getLog(DBSTransactionState.class);

    protected final DBSRepository repository;

    /** Retrieved and created document state. */
    protected Map<String, DBSDocumentState> transientStates = new HashMap<String, DBSDocumentState>();

    /** Ids of documents created but not yet saved. */
    protected Set<String> transientCreated = new LinkedHashSet<String>();

    /** Saved documents. */
    protected Map<String, DBSDocumentState> savedStates = new HashMap<String, DBSDocumentState>();

    /** Ids of documents created. */
    protected Set<String> savedCreated = new LinkedHashSet<String>();

    /** Ids of documents deleted. */
    protected Set<String> savedDeleted = new HashSet<String>();

    public DBSTransactionState(DBSRepository repository) {
        this.repository = repository;
    }

    protected DBSDocumentState returnTransient(DBSDocumentState state) {
        state = new DBSDocumentState(state);
        transientStates.put(state.getId(), state);
        return state;
    }

    protected DBSDocumentState returnTransient(Map<String, Serializable> map) {
        DBSDocumentState state = new DBSDocumentState(map);
        transientStates.put(state.getId(), state);
        return state;
    }

    public void removeState(String id) {
        transientStates.remove(id);
        if (transientCreated.remove(id)) {
            return;
        }
        savedStates.remove(id);
        if (savedCreated.remove(id)) {
            return;
        }
        savedDeleted.add(id);
    }

    public DBSDocumentState getState(String id) {
        // check transient state
        DBSDocumentState state = transientStates.get(id);
        if (state != null) {
            return state;
        }
        // check saved state
        state = savedStates.get(id);
        if (state != null) {
            return returnTransient(state);
        }
        // fetch from repository
        Map<String, Serializable> map = repository.readState(id);
        if (map != null) {
            return returnTransient(map);
        }
        return null;
    }

    public List<DBSDocumentState> getStates(List<String> ids) {
        // check which ones we have to fetch from repository
        List<String> idsToFetch = new LinkedList<String>();
        for (String id : ids) {
            // check transient state
            DBSDocumentState state = transientStates.get(id);
            if (state != null) {
                continue;
            }
            // check saved state
            state = savedStates.get(id);
            if (state != null) {
                returnTransient(state);
                continue;
            }
            // will have to fetch it
            idsToFetch.add(id);
        }
        if (!idsToFetch.isEmpty()) {
            List<Map<String, Serializable>> maps = repository.readStates(idsToFetch);
            for (Map<String, Serializable> map : maps) {
                if (map != null) {
                    returnTransient(map);
                }
            }
        }
        // everything now fetched in transient
        List<DBSDocumentState> states = new ArrayList<DBSDocumentState>(
                ids.size());
        for (String id : ids) {
            DBSDocumentState state = transientStates.get(id);
            if (state != null) {
                states.add(state);
            } else {
                log.warn("Cannot fetch document with id: " + id, new Throwable(
                        "debug stack trace"));
            }
        }
        return states;
    }

    public DBSDocumentState getChildState(String parentId, String name) {
        if (savedDeleted.contains(parentId)) {
            return null;
        }
        Set<String> seen = new HashSet<String>();
        for (DBSDocumentState state : transientStates.values()) {
            seen.add(state.getId());
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            if (!name.equals(state.getName())) {
                continue;
            }
            return state;
        }
        for (DBSDocumentState state : savedStates.values()) {
            if (!seen.add(state.getId())) {
                // already seen
                continue;
            }
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            if (!name.equals(state.getName())) {
                continue;
            }
            return returnTransient(state);
        }
        Map<String, Serializable> map = repository.readChildState(parentId,
                name, seen);
        if (map != null) {
            return returnTransient(map);
        }
        return null;
    }

    public boolean hasChild(String parentId, String name) {
        if (savedDeleted.contains(parentId)) {
            return false;
        }
        Set<String> seen = new HashSet<String>();
        for (DBSDocumentState state : transientStates.values()) {
            seen.add(state.getId());
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            if (!name.equals(state.getName())) {
                continue;
            }
            return true;
        }
        for (DBSDocumentState state : savedStates.values()) {
            if (!seen.add(state.getId())) {
                // already seen
                continue;
            }
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            if (!name.equals(state.getName())) {
                continue;
            }
            return true;
        }
        return repository.hasChild(parentId, name, seen);
    }

    public List<DBSDocumentState> getChildrenStates(String parentId) {
        if (savedDeleted.contains(parentId)) {
            return Collections.emptyList();
        }
        List<DBSDocumentState> children = new LinkedList<DBSDocumentState>();
        Set<String> ids = new HashSet<String>();
        for (DBSDocumentState state : transientStates.values()) {
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            children.add(state);
            ids.add(state.getId());
        }
        for (DBSDocumentState state : savedStates.values()) {
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            String id = state.getId();
            if (!ids.contains(id)) {
                state = returnTransient(state);
                children.add(state);
                ids.add(id);
            }
        }
        List<Map<String, Serializable>> list = repository.readKeyValuedStates(
                KEY_PARENT_ID, parentId);
        for (Map<String, Serializable> map : list) {
            String id = (String) map.get(KEY_ID);
            if (!ids.contains(id)) {
                DBSDocumentState state = returnTransient(map);
                children.add(state);
                ids.add(id);
            }
        }
        return children;
    }

    public List<String> getChildrenIds(String parentId) {
        if (savedDeleted.contains(parentId)) {
            return Collections.emptyList();
        }
        Set<String> children = new LinkedHashSet<String>();
        for (DBSDocumentState state : transientStates.values()) {
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            children.add(state.getId());
        }
        for (DBSDocumentState state : savedStates.values()) {
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            children.add(state.getId());
        }
        List<Map<String, Serializable>> list = repository.readKeyValuedStates(
                KEY_PARENT_ID, parentId);
        for (Map<String, Serializable> map : list) {
            children.add((String) map.get(KEY_ID));
        }
        return new ArrayList<String>(children);
    }

    public boolean hasChildren(String parentId) {
        if (savedDeleted.contains(parentId)) {
            return false;
        }
        for (DBSDocumentState state : transientStates.values()) {
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            return true;
        }
        for (DBSDocumentState state : savedStates.values()) {
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            return true;
        }
        List<Map<String, Serializable>> list = repository.readKeyValuedStates(
                KEY_PARENT_ID, parentId);
        if (!list.isEmpty()) {
            return true;
        }
        return false;
    }

    // id may be not-null for import
    public DBSDocumentState createChild(String id, String parentId,
            String name, Long pos, String typeName) {
        id = generateNewId(id);
        transientCreated.add(id);
        DBSDocumentState state = new DBSDocumentState();
        transientStates.put(id, state);
        state.put(KEY_ID, id);
        state.put(KEY_PARENT_ID, parentId);
        state.put(KEY_NAME, name);
        state.put(KEY_POS, pos);
        state.put(KEY_PRIMARY_TYPE, typeName);
        return state;
    }

    /**
     * Copies the document into a new just-created object.
     */
    public DBSDocumentState getCopy(String id) {
        DBSDocumentState copyState = new DBSDocumentState(getState(id));
        String copyId = generateNewId(null);
        copyState.put(KEY_ID, copyId);
        transientStates.put(copyId, copyState);
        transientCreated.add(copyId);
        return copyState;
    }

    protected String generateNewId(String id) {
        if (id == null) {
            id = repository.generateNewId();
        }
        return id;
    }

    public List<DBSDocumentState> getKeyValuedStates(String key, String value) {
        List<DBSDocumentState> states = new LinkedList<DBSDocumentState>();
        Set<String> ids = new HashSet<String>();
        for (DBSDocumentState state : transientStates.values()) {
            if (!value.equals(state.get(key))) {
                continue;
            }
            states.add(state);
            ids.add(state.getId());
        }
        for (DBSDocumentState state : savedStates.values()) {
            if (!value.equals(state.get(key))) {
                continue;
            }
            String id = state.getId();
            if (!ids.contains(id)) {
                state = returnTransient(state);
                states.add(state);
                ids.add(id);
            }
        }
        List<Map<String, Serializable>> list = repository.readKeyValuedStates(
                key, value);
        for (Map<String, Serializable> map : list) {
            String id = (String) map.get(KEY_ID);
            if (!ids.contains(id)) {
                DBSDocumentState state = returnTransient(map);
                states.add(state);
                ids.add(id);
            }
        }
        return states;
    }

    /**
     * Saves transient state to saved state.
     * <p>
     * Dirty states are saved, all states are kept in transient because there
     * may be references to them.
     */
    public void save() {
        for (String id : transientCreated) { // ordered
            DBSDocumentState state = transientStates.get(id);
            state.setNotDirty();
            savedStates.put(id, new DBSDocumentState(state)); // copy
            savedCreated.add(id);
        }
        for (DBSDocumentState state : transientStates.values()) {
            String id = state.getId();
            if (transientCreated.contains(id)) {
                continue; // already done
            }
            if (state.isDirty()) {
                state.setNotDirty();
                savedStates.put(id, new DBSDocumentState(state)); // copy
            }
        }
        transientCreated.clear();
    }

    /**
     * Flushes saved state to database.
     */
    protected void flush() throws DocumentException {
        for (String id : savedCreated) { // ordered
            DBSDocumentState state = savedStates.get(id);
            repository.createState(state.map);
        }
        for (DBSDocumentState state : savedStates.values()) {
            if (savedCreated.contains(state.getId())) {
                continue; // already done
            }
            repository.updateState(state.map);
        }
        for (String id : savedDeleted) {
            repository.deleteState(id);
        }
        // clear transient, this means that after this references to states
        // will be stale TODO mark state as invalid
        clearTransient();
        clearSaved();
    }

    /**
     * Saves and flushes to database.
     */
    public void commit() throws DocumentException {
        save();
        flush();
    }

    /**
     * Forgets all changes.
     */
    public void rollback() {
        clearTransient();
        clearSaved();
    }

    protected void clearTransient() {
        transientStates.clear();
        transientCreated.clear();
    }

    protected void clearSaved() {
        savedStates.clear();
        savedCreated.clear();
        savedDeleted.clear();
    }

}
