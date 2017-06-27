/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.INITIAL_CHANGE_TOKEN;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_GRANT;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_PERMISSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_USER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_CHANGE_TOKEN;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_JOBID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_PROXY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_MIXIN_TYPES;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_POS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PREFIX;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_TARGET_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_VERSION_SERIES_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_READ_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_SYS_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_SERIES_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.storage.BaseDocument;
import org.nuxeo.ecm.core.storage.DefaultFulltextParser;
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.FulltextParser;
import org.nuxeo.ecm.core.storage.FulltextUpdaterWork;
import org.nuxeo.ecm.core.storage.FulltextUpdaterWork.IndexAndText;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.StateHelper;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;

/**
 * Transactional state for a session.
 * <p>
 * Until {@code save()} is called, data lives in the transient map.
 * <p>
 * Upon save, data is written to the repository, even though it has not yet been committed (this means that other
 * sessions can read uncommitted data). It's also kept in an undo log in order for rollback to be possible.
 * <p>
 * On commit, the undo log is forgotten. On rollback, the undo log is replayed.
 *
 * @since 5.9.4
 */
public class DBSTransactionState {

    private static final Log log = LogFactory.getLog(DBSTransactionState.class);

    private static final String KEY_UNDOLOG_CREATE = "__UNDOLOG_CREATE__\0\0";

    protected final DBSRepository repository;

    protected final DBSSession session;

    /** Retrieved and created document state. */
    protected Map<String, DBSDocumentState> transientStates = new HashMap<>();

    /** Ids of documents created but not yet saved. */
    protected Set<String> transientCreated = new LinkedHashSet<>();

    /**
     * Undo log.
     * <p>
     * A map of document ids to null or State. The value is null when the document has to be deleted when applying the
     * undo log. Otherwise the value is a State. If the State contains the key {@link #KEY_UNDOLOG_CREATE} then the
     * state must be re-created completely when applying the undo log, otherwise just applied as an update.
     * <p>
     * Null when there is no active transaction.
     */
    protected Map<String, State> undoLog;

    protected final Set<String> browsePermissions;

    public DBSTransactionState(DBSRepository repository, DBSSession session) {
        this.repository = repository;
        this.session = session;
        SecurityService securityService = Framework.getLocalService(SecurityService.class);
        browsePermissions = new HashSet<>(Arrays.asList(securityService.getPermissionsToCheck(BROWSE)));
    }

    /**
     * New transient state for something just read from the repository.
     */
    protected DBSDocumentState newTransientState(State state) {
        if (state == null) {
            return null;
        }
        String id = (String) state.get(KEY_ID);
        if (transientStates.containsKey(id)) {
            throw new IllegalStateException("Already transient: " + id);
        }
        DBSDocumentState docState = new DBSDocumentState(state); // copy
        transientStates.put(id, docState);
        return docState;
    }

    /**
     * Returns a state and marks it as transient, because it's about to be modified or returned to user code (where it
     * may be modified).
     */
    public DBSDocumentState getStateForUpdate(String id) {
        // check transient state
        DBSDocumentState docState = transientStates.get(id);
        if (docState != null) {
            return docState;
        }
        // fetch from repository
        State state = repository.readState(id);
        return newTransientState(state);
    }

    /**
     * Returns a state which won't be modified.
     */
    // TODO in some cases it's good to have this kept in memory instead of
    // rereading from database every time
    // XXX getStateForReadOneShot
    public State getStateForRead(String id) {
        // check transient state
        DBSDocumentState docState = transientStates.get(id);
        if (docState != null) {
            return docState.getState();
        }
        // fetch from repository
        return repository.readState(id);
    }

    /**
     * Returns states and marks them transient, because they're about to be returned to user code (where they may be
     * modified).
     */
    public List<DBSDocumentState> getStatesForUpdate(List<String> ids) {
        // check which ones we have to fetch from repository
        List<String> idsToFetch = new LinkedList<>();
        for (String id : ids) {
            // check transient state
            DBSDocumentState docState = transientStates.get(id);
            if (docState != null) {
                continue;
            }
            // will have to fetch it
            idsToFetch.add(id);
        }
        if (!idsToFetch.isEmpty()) {
            List<State> states = repository.readStates(idsToFetch);
            for (State state : states) {
                newTransientState(state);
            }
        }
        // everything now fetched in transient
        List<DBSDocumentState> docStates = new ArrayList<>(ids.size());
        for (String id : ids) {
            DBSDocumentState docState = transientStates.get(id);
            if (docState == null) {
                if (log.isTraceEnabled()) {
                    log.trace("Cannot fetch document with id: " + id, new Throwable("debug stack trace"));
                }
                continue;
            }
            docStates.add(docState);
        }
        return docStates;
    }

    // XXX TODO for update or for read?
    public DBSDocumentState getChildState(String parentId, String name) {
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            if (!name.equals(docState.getName())) {
                continue;
            }
            return docState;
        }
        // fetch from repository
        State state = repository.readChildState(parentId, name, Collections.emptySet());
        if (state == null) {
            return null;
        }
        String id = (String) state.get(KEY_ID);
        if (transientStates.containsKey(id)) {
            // found transient, even though we already checked
            // that means that in-memory it's not a child, but in-database it's a child (was moved)
            // -> ignore the database state
            return null;
        }
        return newTransientState(state);
    }

    public boolean hasChild(String parentId, String name) {
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            if (!name.equals(docState.getName())) {
                continue;
            }
            return true;
        }
        // check repository
        return repository.hasChild(parentId, name, Collections.emptySet());
    }

    public List<DBSDocumentState> getChildrenStates(String parentId) {
        List<DBSDocumentState> docStates = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            docStates.add(docState);
            seen.add(docState.getId());
        }
        // fetch from repository
        List<State> states = repository.queryKeyValue(KEY_PARENT_ID, parentId, seen);
        for (State state : states) {
            String id = (String) state.get(KEY_ID);
            if (transientStates.containsKey(id)) {
                // found transient, even though we passed an exclusion list for known children
                // that means that in-memory it's not a child, but in-database it's a child (was moved)
                // -> ignore the database state
                continue;
            }
            docStates.add(newTransientState(state));
        }
        return docStates;
    }

    public List<String> getChildrenIds(String parentId) {
        List<String> children = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            String id = docState.getId();
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            seen.add(id);
            children.add(id);
        }
        // fetch from repository
        List<State> states = repository.queryKeyValue(KEY_PARENT_ID, parentId, seen);
        for (State state : states) {
            String id = (String) state.get(KEY_ID);
            if (transientStates.containsKey(id)) {
                // found transient, even though we passed an exclusion list for known children
                // that means that in-memory it's not a child, but in-database it's a child (was moved)
                // -> ignore the database state
                continue;
            }
            children.add(id);
        }
        return new ArrayList<>(children);
    }

    public boolean hasChildren(String parentId) {
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            return true;
        }
        // check repository
        return repository.queryKeyValuePresence(KEY_PARENT_ID, parentId, Collections.emptySet());
    }

    public DBSDocumentState createChild(String id, String parentId, String name, Long pos, String typeName) {
        // id may be not-null for import
        if (id == null) {
            id = repository.generateNewId();
        }
        transientCreated.add(id);
        DBSDocumentState docState = new DBSDocumentState();
        transientStates.put(id, docState);
        docState.put(KEY_ID, id);
        docState.put(KEY_PARENT_ID, parentId);
        docState.put(KEY_ANCESTOR_IDS, getAncestorIds(parentId));
        docState.put(KEY_NAME, name);
        docState.put(KEY_POS, pos);
        docState.put(KEY_PRIMARY_TYPE, typeName);
        if (session.changeTokenEnabled) {
            docState.put(KEY_SYS_VERSION, Long.valueOf(0));
        }
        // update read acls for new doc
        updateDocumentReadAcls(id);
        return docState;
    }

    /** Gets ancestors including id itself. */
    protected Object[] getAncestorIds(String id) {
        if (id == null) {
            return null;
        }
        State state = getStateForRead(id);
        if (state == null) {
            throw new RuntimeException("No such id: " + id);
        }
        Object[] ancestors = (Object[]) state.get(KEY_ANCESTOR_IDS);
        if (ancestors == null) {
            return new Object[] { id };
        } else {
            Object[] newAncestors = new Object[ancestors.length + 1];
            System.arraycopy(ancestors, 0, newAncestors, 0, ancestors.length);
            newAncestors[ancestors.length] = id;
            return newAncestors;
        }
    }

    /**
     * Copies the document into a newly-created object.
     * <p>
     * The copy is automatically saved.
     */
    public DBSDocumentState copy(String id) {
        DBSDocumentState copyState = new DBSDocumentState(getStateForRead(id));
        String copyId = repository.generateNewId();
        copyState.put(KEY_ID, copyId);
        copyState.put(KEY_PROXY_IDS, null); // no proxies to this new doc
        // other fields updated by the caller
        transientStates.put(copyId, copyState);
        transientCreated.add(copyId);
        return copyState;
    }

    /**
     * Updates ancestors recursively after a move.
     * <p>
     * Recursing from given doc, replace the first ndel ancestors with those passed.
     * <p>
     * Doesn't check transient (assumes save is done). The modifications are automatically saved.
     */
    public void updateAncestors(String id, int ndel, Object[] ancestorIds) {
        int nadd = ancestorIds.length;
        Set<String> ids = getSubTree(id, null, null);
        ids.add(id);
        for (String cid : ids) {
            // XXX TODO oneShot update, don't pollute transient space
            DBSDocumentState docState = getStateForUpdate(cid);
            Object[] ancestors = (Object[]) docState.get(KEY_ANCESTOR_IDS);
            Object[] newAncestors;
            if (ancestors == null) {
                newAncestors = ancestorIds.clone();
            } else {
                newAncestors = new Object[ancestors.length - ndel + nadd];
                System.arraycopy(ancestorIds, 0, newAncestors, 0, nadd);
                System.arraycopy(ancestors, ndel, newAncestors, nadd, ancestors.length - ndel);
            }
            docState.put(KEY_ANCESTOR_IDS, newAncestors);
        }
    }

    /**
     * Updates the Read ACLs recursively on a document.
     */
    public void updateTreeReadAcls(String id) {
        // versions too XXX TODO
        Set<String> ids = getSubTree(id, null, null);
        ids.add(id);
        ids.forEach(this::updateDocumentReadAcls);
    }

    /**
     * Updates the Read ACLs on a document (not recursively)
     */
    protected void updateDocumentReadAcls(String id) {
        // XXX TODO oneShot update, don't pollute transient space
        DBSDocumentState docState = getStateForUpdate(id);
        docState.put(KEY_READ_ACL, getReadACL(docState));
    }

    /**
     * Gets the Read ACL (flat list of users having browse permission, including inheritance) on a document.
     */
    protected String[] getReadACL(DBSDocumentState docState) {
        Set<String> racls = new HashSet<>();
        State state = docState.getState();
        LOOP: do {
            @SuppressWarnings("unchecked")
            List<Serializable> aclList = (List<Serializable>) state.get(KEY_ACP);
            if (aclList != null) {
                for (Serializable aclSer : aclList) {
                    State aclMap = (State) aclSer;
                    @SuppressWarnings("unchecked")
                    List<Serializable> aceList = (List<Serializable>) aclMap.get(KEY_ACL);
                    for (Serializable aceSer : aceList) {
                        State aceMap = (State) aceSer;
                        String username = (String) aceMap.get(KEY_ACE_USER);
                        String permission = (String) aceMap.get(KEY_ACE_PERMISSION);
                        Boolean granted = (Boolean) aceMap.get(KEY_ACE_GRANT);
                        if (TRUE.equals(granted) && browsePermissions.contains(permission)) {
                            racls.add(username);
                        }
                        if (FALSE.equals(granted)) {
                            if (!EVERYONE.equals(username)) {
                                // TODO log
                                racls.add(UNSUPPORTED_ACL);
                            }
                            break LOOP;
                        }
                    }
                }
            }
            // get parent
            if (TRUE.equals(state.get(KEY_IS_VERSION))) {
                String versionSeriesId = (String) state.get(KEY_VERSION_SERIES_ID);
                state = versionSeriesId == null ? null : getStateForRead(versionSeriesId);
            } else {
                String parentId = (String) state.get(KEY_PARENT_ID);
                state = parentId == null ? null : getStateForRead(parentId);
            }
        } while (state != null);

        // sort to have canonical order
        List<String> racl = new ArrayList<>(racls);
        Collections.sort(racl);
        return racl.toArray(new String[racl.size()]);
    }

    /**
     * Gets all the ids under a given one, recursively.
     * <p>
     * Doesn't check transient (assumes save is done).
     *
     * @param id the root of the tree (not included in results)
     * @param proxyTargets returns a map of proxy to target among the documents found
     * @param targetProxies returns a map of target to proxies among the document found
     */
    protected Set<String> getSubTree(String id, Map<String, String> proxyTargets, Map<String, Object[]> targetProxies) {
        Set<String> ids = new HashSet<>();
        // check repository
        repository.queryKeyValueArray(KEY_ANCESTOR_IDS, id, ids, proxyTargets, targetProxies);
        return ids;
    }

    public List<DBSDocumentState> getKeyValuedStates(String key, Object value) {
        List<DBSDocumentState> docStates = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            if (!value.equals(docState.get(key))) {
                continue;
            }
            docStates.add(docState);
            seen.add(docState.getId());
        }
        // fetch from repository
        List<State> states = repository.queryKeyValue(key, value, seen);
        for (State state : states) {
            docStates.add(newTransientState(state));
        }
        return docStates;
    }

    public List<DBSDocumentState> getKeyValuedStates(String key1, Object value1, String key2, Object value2) {
        List<DBSDocumentState> docStates = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            seen.add(docState.getId());
            if (!(value1.equals(docState.get(key1)) && value2.equals(docState.get(key2)))) {
                continue;
            }
            docStates.add(docState);
        }
        // fetch from repository
        List<State> states = repository.queryKeyValue(key1, value1, key2, value2, seen);
        for (State state : states) {
            docStates.add(newTransientState(state));
        }
        return docStates;
    }

    /**
     * Removes a list of documents.
     * <p>
     * Called after a {@link #save} has been done.
     */
    public void removeStates(Set<String> ids) {
        if (undoLog != null) {
            for (String id : ids) {
                if (undoLog.containsKey(id)) {
                    // there's already a create or an update in the undo log
                    State oldUndo = undoLog.get(id);
                    if (oldUndo == null) {
                        // create + delete -> forget
                        undoLog.remove(id);
                    } else {
                        // update + delete -> original old state to re-create
                        oldUndo.put(KEY_UNDOLOG_CREATE, TRUE);
                    }
                } else {
                    // just delete -> store old state to re-create
                    State oldState = StateHelper.deepCopy(getStateForRead(id));
                    oldState.put(KEY_UNDOLOG_CREATE, TRUE);
                    undoLog.put(id, oldState);
                }
            }
        }
        for (String id : ids) {
            transientStates.remove(id);
        }
        repository.deleteStates(ids);
    }

    /**
     * Writes transient state to database.
     * <p>
     * An undo log is kept in order to rollback the transaction later if needed.
     */
    public void save() {
        updateProxies();
        List<Work> works;
        if (!repository.isFulltextDisabled()) {
            // TODO getting fulltext already does a getStateChange
            works = getFulltextWorks();
        } else {
            works = Collections.emptyList();
        }
        List<State> statesToCreate = new ArrayList<>();
        for (String id : transientCreated) { // ordered
            DBSDocumentState docState = transientStates.get(id);
            docState.setNotDirty();
            if (undoLog != null) {
                undoLog.put(id, null); // marker to denote create
            }
            State state = docState.getState();
            state.put(KEY_CHANGE_TOKEN, INITIAL_CHANGE_TOKEN);
            statesToCreate.add(state);
        }
        if (!statesToCreate.isEmpty()) {
            repository.createStates(statesToCreate);
        }
        for (DBSDocumentState docState : transientStates.values()) {
            String id = docState.getId();
            if (transientCreated.contains(id)) {
                continue; // already done
            }
            StateDiff diff = docState.getStateChange();
            if (diff != null) {
                if (undoLog != null) {
                    if (!undoLog.containsKey(id)) {
                        undoLog.put(id, StateHelper.deepCopy(docState.getOriginalState()));
                    }
                    // else there's already a create or an update in the undo log so original info is enough
                }
                ChangeTokenUpdater changeTokenUpdater;
                if (session.changeTokenEnabled) {
                    // increment system version
                    Long base = (Long) docState.get(KEY_SYS_VERSION);
                    docState.put(KEY_SYS_VERSION, DeltaLong.valueOf(base, 1));
                    // update change token
                    changeTokenUpdater = new ChangeTokenUpdater(docState);
                } else {
                    changeTokenUpdater = null;
                }
                repository.updateState(id, diff, changeTokenUpdater);
            }
            docState.setNotDirty();
        }
        transientCreated.clear();
        scheduleWork(works);
    }

    /**
     * Logic to get the conditions to use to match and update a change token.
     * <p>
     * This may be called several times for a single DBS document update, because the low-level storage may need several
     * database updates for a single high-level update in some cases.
     *
     * @since 9.1
     */
    public static class ChangeTokenUpdater {

        protected final DBSDocumentState docState;

        protected Long oldToken;

        public ChangeTokenUpdater(DBSDocumentState docState) {
            this.docState = docState;
            oldToken = (Long) docState.getOriginalState().get(KEY_CHANGE_TOKEN);
        }

        /**
         * Gets the conditions to use to match a change token.
         */
        public Map<String, Serializable> getConditions() {
            return Collections.singletonMap(KEY_CHANGE_TOKEN, oldToken);
        }

        /**
         * Gets the updates to make to write the updated change token.
         */
        public Map<String, Serializable> getUpdates() {
            Long newToken;
            if (oldToken == null) {
                // document without change token, just created
                newToken = INITIAL_CHANGE_TOKEN;
            } else {
                newToken = BaseDocument.updateChangeToken(oldToken);
            }
            // also store the new token in the state (without marking dirty), for the next update
            docState.getState().put(KEY_CHANGE_TOKEN, newToken);
            oldToken = newToken;
            return Collections.singletonMap(KEY_CHANGE_TOKEN, newToken);
        }
    }

    protected void applyUndoLog() {
        Set<String> deletes = new HashSet<>();
        for (Entry<String, State> es : undoLog.entrySet()) {
            String id = es.getKey();
            State state = es.getValue();
            if (state == null) {
                deletes.add(id);
            } else {
                boolean recreate = state.remove(KEY_UNDOLOG_CREATE) != null;
                if (recreate) {
                    repository.createState(state);
                } else {
                    // undo update
                    State currentState = repository.readState(id);
                    if (currentState != null) {
                        StateDiff diff = StateHelper.diff(currentState, state);
                        if (!diff.isEmpty()) {
                            repository.updateState(id, diff, null);
                        }
                    }
                    // else we expected to read a current state but it was concurrently deleted...
                    // in that case leave it deleted
                }
            }
        }
        if (!deletes.isEmpty()) {
            repository.deleteStates(deletes);
        }
    }

    /**
     * Checks if the changed documents are proxy targets, and updates the proxies if that's the case.
     */
    protected void updateProxies() {
        for (String id : transientCreated) { // ordered
            DBSDocumentState docState = transientStates.get(id);
            updateProxies(docState);
        }
        // copy as we may modify proxies
        for (String id : transientStates.keySet().toArray(new String[0])) {
            DBSDocumentState docState = transientStates.get(id);
            if (transientCreated.contains(id)) {
                continue; // already done
            }
            if (docState.isDirty()) {
                updateProxies(docState);
            }
        }
    }

    protected void updateProxies(DBSDocumentState target) {
        Object[] proxyIds = (Object[]) target.get(KEY_PROXY_IDS);
        if (proxyIds != null) {
            for (Object proxyId : proxyIds) {
                try {
                    updateProxy(target, (String) proxyId);
                } catch (ConcurrentUpdateException e) {
                    e.addInfo("On doc " + target.getId());
                    log.error(e, e);
                    // do not throw, this avoids crashing the session
                }
            }
        }
    }

    /**
     * Updates the state of a proxy based on its target.
     */
    protected void updateProxy(DBSDocumentState target, String proxyId) {
        DBSDocumentState proxy = getStateForUpdate(proxyId);
        if (proxy == null) {
            throw new ConcurrentUpdateException("Proxy " + proxyId + " concurrently deleted");
        }
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        // clear all proxy data
        for (String key : proxy.getState().keyArray()) {
            if (!isProxySpecific(key, schemaManager)) {
                proxy.put(key, null);
            }
        }
        // copy from target
        for (Entry<String, Serializable> en : target.getState().entrySet()) {
            String key = en.getKey();
            if (!isProxySpecific(key, schemaManager)) {
                proxy.put(key, StateHelper.deepCopy(en.getValue()));
            }
        }
    }

    /**
     * Things that we don't touch on a proxy when updating it.
     */
    protected boolean isProxySpecific(String key, SchemaManager schemaManager) {
        switch (key) {
        // these are placeful stuff
        case KEY_ID:
        case KEY_PARENT_ID:
        case KEY_ANCESTOR_IDS:
        case KEY_NAME:
        case KEY_POS:
        case KEY_ACP:
        case KEY_READ_ACL:
            // these are proxy-specific
        case KEY_IS_PROXY:
        case KEY_PROXY_TARGET_ID:
        case KEY_PROXY_VERSION_SERIES_ID:
        case KEY_IS_VERSION:
        case KEY_PROXY_IDS:
            return true;
        }
        int p = key.indexOf(':');
        if (p == -1) {
            // no prefix, assume not proxy-specific
            return false;
        }
        String prefix = key.substring(0, p);
        Schema schema = schemaManager.getSchemaFromPrefix(prefix);
        if (schema == null) {
            schema = schemaManager.getSchema(prefix);
            if (schema == null) {
                // unknown prefix, assume not proxy-specific
                return false;
            }
        }
        return schemaManager.isProxySchema(schema.getName(), null); // type unused
    }

    /**
     * Called when created in a transaction.
     *
     * @since 7.4
     */
    public void begin() {
        undoLog = new HashMap<>();
        repository.begin();
    }

    /**
     * Saves and flushes to database.
     */
    public void commit() {
        save();
        commitSave();
        repository.commit();
    }

    /**
     * Commits the saved state to the database.
     */
    protected void commitSave() {
        // clear transient, this means that after this references to states will be stale
        // TODO mark states as invalid
        clearTransient();
        // the transaction ended, the proxied DBSSession will disappear and cannot be reused anyway
        undoLog = null;
    }

    /**
     * Rolls back the save state by applying the undo log.
     */
    public void rollback() {
        clearTransient();
        applyUndoLog();
        // the transaction ended, the proxied DBSSession will disappear and cannot be reused anyway
        undoLog = null;
        repository.rollback();
    }

    protected void clearTransient() {
        transientStates.clear();
        transientCreated.clear();
    }

    /**
     * Gets the fulltext updates to do. Called at save() time.
     *
     * @return a list of {@link Work} instances to schedule post-commit.
     */
    protected List<Work> getFulltextWorks() {
        Set<String> docsWithDirtyStrings = new HashSet<>();
        Set<String> docsWithDirtyBinaries = new HashSet<>();
        findDirtyDocuments(docsWithDirtyStrings, docsWithDirtyBinaries);
        if (docsWithDirtyStrings.isEmpty() && docsWithDirtyBinaries.isEmpty()) {
            return Collections.emptyList();
        }
        List<Work> works = new LinkedList<>();
        getFulltextSimpleWorks(works, docsWithDirtyStrings);
        getFulltextBinariesWorks(works, docsWithDirtyBinaries);
        return works;
    }

    /**
     * Finds the documents having dirty text or dirty binaries that have to be reindexed as fulltext.
     *
     * @param docsWithDirtyStrings set of ids, updated by this method
     * @param docWithDirtyBinaries set of ids, updated by this method
     */
    protected void findDirtyDocuments(Set<String> docsWithDirtyStrings, Set<String> docWithDirtyBinaries) {
        for (DBSDocumentState docState : transientStates.values()) {
            State originalState = docState.getOriginalState();
            State state = docState.getState();
            if (originalState == state) {
                continue;
            }
            StateDiff diff = StateHelper.diff(originalState, state);
            if (diff.isEmpty()) {
                continue;
            }
            StateDiff rdiff = StateHelper.diff(state, originalState);
            // we do diffs in both directions to capture removal of complex list elements,
            // for instance for {foo: [{bar: baz}] -> {foo: []}
            // diff paths = foo and rdiff paths = foo/*/bar
            Set<String> paths = new HashSet<>();
            DirtyPathsFinder dirtyPathsFinder = new DirtyPathsFinder(paths);
            dirtyPathsFinder.findDirtyPaths(diff);
            dirtyPathsFinder.findDirtyPaths(rdiff);
            FulltextConfiguration fulltextConfiguration = repository.getFulltextConfiguration();
            boolean dirtyStrings = false;
            boolean dirtyBinaries = false;
            for (String path : paths) {
                Set<String> indexesSimple = fulltextConfiguration.indexesByPropPathSimple.get(path);
                if (indexesSimple != null && !indexesSimple.isEmpty()) {
                    dirtyStrings = true;
                    if (dirtyBinaries) {
                        break;
                    }
                }
                Set<String> indexesBinary = fulltextConfiguration.indexesByPropPathBinary.get(path);
                if (indexesBinary != null && !indexesBinary.isEmpty()) {
                    dirtyBinaries = true;
                    if (dirtyStrings) {
                        break;
                    }
                }
            }
            if (dirtyStrings) {
                docsWithDirtyStrings.add(docState.getId());
            }
            if (dirtyBinaries) {
                docWithDirtyBinaries.add(docState.getId());
            }
        }
    }

    /**
     * Iterates on a state diff to find the paths corresponding to dirty values.
     *
     * @since 7.10-HF04, 8.1
     */
    protected static class DirtyPathsFinder {

        protected Set<String> paths;

        public DirtyPathsFinder(Set<String> paths) {
            this.paths = paths;
        }

        public void findDirtyPaths(StateDiff value) {
            findDirtyPaths(value, null);
        }

        protected void findDirtyPaths(Object value, String path) {
            if (value instanceof Object[]) {
                findDirtyPaths((Object[]) value, path);
            } else if (value instanceof List) {
                findDirtyPaths((List<?>) value, path);
            } else if (value instanceof ListDiff) {
                findDirtyPaths((ListDiff) value, path);
            } else if (value instanceof State) {
                findDirtyPaths((State) value, path);
            } else {
                paths.add(path);
            }
        }

        protected void findDirtyPaths(Object[] value, String path) {
            String newPath = path + "/*";
            for (Object v : value) {
                findDirtyPaths(v, newPath);
            }
        }

        protected void findDirtyPaths(List<?> value, String path) {
            String newPath = path + "/*";
            for (Object v : value) {
                findDirtyPaths(v, newPath);
            }
        }

        protected void findDirtyPaths(ListDiff value, String path) {
            String newPath = path + "/*";
            if (value.diff != null) {
                findDirtyPaths(value.diff, newPath);
            }
            if (value.rpush != null) {
                findDirtyPaths(value.rpush, newPath);
            }
        }

        protected void findDirtyPaths(State value, String path) {
            for (Entry<String, Serializable> es : value.entrySet()) {
                String key = es.getKey();
                Serializable v = es.getValue();
                String newPath = path == null ? key : path + "/" + key;
                findDirtyPaths(v, newPath);
            }
        }
    }

    protected void getFulltextSimpleWorks(List<Work> works, Set<String> docsWithDirtyStrings) {
        // TODO XXX make configurable, see also FulltextExtractorWork
        FulltextParser fulltextParser = new DefaultFulltextParser();
        FulltextConfiguration fulltextConfiguration = repository.getFulltextConfiguration();
        if (fulltextConfiguration.fulltextSearchDisabled) {
            return;
        }
        // update simpletext on documents with dirty strings
        for (String id : docsWithDirtyStrings) {
            if (id == null) {
                // cannot happen, but has been observed :(
                log.error("Got null doc id in fulltext update, cannot happen");
                continue;
            }
            DBSDocumentState docState = getStateForUpdate(id);
            if (docState == null) {
                // cannot happen
                continue;
            }
            String documentType = docState.getPrimaryType();
            // Object[] mixinTypes = (Object[]) docState.get(KEY_MIXIN_TYPES);

            if (!fulltextConfiguration.isFulltextIndexable(documentType)) {
                continue;
            }
            docState.put(KEY_FULLTEXT_JOBID, docState.getId());
            FulltextFinder fulltextFinder = new FulltextFinder(fulltextParser, docState, session);
            List<IndexAndText> indexesAndText = new LinkedList<>();
            for (String indexName : fulltextConfiguration.indexNames) {
                // TODO paths from config
                String text = fulltextFinder.findFulltext(indexName);
                indexesAndText.add(new IndexAndText(indexName, text));
            }
            if (!indexesAndText.isEmpty()) {
                Work work = new FulltextUpdaterWork(repository.getName(), id, true, false, indexesAndText);
                works.add(work);
            }
        }
    }

    protected void getFulltextBinariesWorks(List<Work> works, Set<String> docWithDirtyBinaries) {
        if (docWithDirtyBinaries.isEmpty()) {
            return;
        }

        FulltextConfiguration fulltextConfiguration = repository.getFulltextConfiguration();

        // mark indexing in progress, so that future copies (including versions)
        // will be indexed as well
        for (String id : docWithDirtyBinaries) {
            DBSDocumentState docState = getStateForUpdate(id);
            if (docState == null) {
                // cannot happen
                continue;
            }
            if (!fulltextConfiguration.isFulltextIndexable(docState.getPrimaryType())) {
                continue;
            }
            docState.put(KEY_FULLTEXT_JOBID, docState.getId());
        }

        // FulltextExtractorWork does fulltext extraction using converters
        // and then schedules a FulltextUpdaterWork to write the results
        // single-threaded
        for (String id : docWithDirtyBinaries) {
            // don't exclude proxies
            Work work = new DBSFulltextExtractorWork(repository.getName(), id);
            works.add(work);
        }
    }

    protected static class FulltextFinder {

        protected final FulltextParser fulltextParser;

        protected final DBSDocumentState document;

        protected final DBSSession session;

        protected final String documentType;

        protected final Object[] mixinTypes;

        /**
         * Prepares parsing for one document.
         */
        public FulltextFinder(FulltextParser fulltextParser, DBSDocumentState document, DBSSession session) {
            this.fulltextParser = fulltextParser;
            this.document = document;
            this.session = session;
            if (document == null) {
                documentType = null;
                mixinTypes = null;
            } else { // null in tests
                documentType = document.getPrimaryType();
                mixinTypes = (Object[]) document.get(KEY_MIXIN_TYPES);
            }
        }

        /**
         * Parses the document for one index.
         */
        public String findFulltext(String indexName) {
            // TODO indexName
            // TODO paths
            List<String> strings = new ArrayList<>();
            findFulltext(indexName, document.getState(), strings);
            return StringUtils.join(strings, ' ');
        }

        protected void findFulltext(String indexName, State state, List<String> strings) {
            for (Entry<String, Serializable> en : state.entrySet()) {
                String key = en.getKey();
                if (key.startsWith(KEY_PREFIX)) {
                    switch (key) {
                    // allow indexing of this:
                    case DBSDocument.KEY_NAME:
                        break;
                    default:
                        continue;
                    }
                }
                Serializable value = en.getValue();
                if (value instanceof State) {
                    State s = (State) value;
                    findFulltext(indexName, s, strings);
                } else if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<State> v = (List<State>) value;
                    for (State s : v) {
                        findFulltext(indexName, s, strings);
                    }
                } else if (value instanceof Object[]) {
                    Object[] ar = (Object[]) value;
                    for (Object v : ar) {
                        if (v instanceof String) {
                            fulltextParser.parse((String) v, null, strings);
                        } else {
                            // arrays are homogeneous, no need to continue
                            break;
                        }
                    }
                } else {
                    if (value instanceof String) {
                        fulltextParser.parse((String) value, null, strings);
                    }
                }
            }
        }
    }

    protected void scheduleWork(List<Work> works) {
        // do async fulltext indexing only if high-level sessions are available
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        if (repositoryManager != null && !works.isEmpty()) {
            WorkManager workManager = Framework.getLocalService(WorkManager.class);
            for (Work work : works) {
                // schedule work post-commit
                // in non-tx mode, this may execute it nearly immediately
                workManager.schedule(work, Scheduling.IF_NOT_SCHEDULED, true);
            }
        }
    }

}
