/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_VERSION;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.INITIAL_CHANGE_TOKEN;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.INITIAL_SYS_CHANGE_TOKEN;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_GRANT;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_PERMISSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_STATUS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_USER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_BLOB_KEYS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_CHANGE_TOKEN;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_JOBID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_PROXY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_POS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_TARGET_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_VERSION_SERIES_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_READ_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_SYS_CHANGE_TOKEN;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_SERIES_ID;
import static org.nuxeo.ecm.core.storage.dbs.action.UpdateReadAclsAction.UPDATE_READ_ACLS_ACTION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.BatchFinderWork;
import org.nuxeo.ecm.core.BatchProcessorWork;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.impl.DownloadBlobGuard;
import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.model.BaseSession;
import org.nuxeo.ecm.core.model.BaseSession.VersionAclMode;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.storage.BaseDocument;
import org.nuxeo.ecm.core.storage.FulltextExtractorWork;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.StateHelper;
import org.nuxeo.ecm.core.storage.dbs.DBSRepository.DBSQueryOperator;
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

    /** Keys used when computing Read ACLs. */
    protected static final Set<String> READ_ACL_RECURSION_KEYS = new HashSet<>(
            Arrays.asList(KEY_READ_ACL, KEY_ACP, KEY_IS_VERSION, KEY_VERSION_SERIES_ID, KEY_PARENT_ID));

    public static final String READ_ACL_ASYNC_ENABLED_PROPERTY = "nuxeo.core.readacl.async.enabled";

    public static final String READ_ACL_ASYNC_ENABLED_DEFAULT = "true";

    public static final String READ_ACL_ASYNC_THRESHOLD_PROPERTY = "nuxeo.core.readacl.async.threshold";

    public static final String READ_ACL_ASYNC_THRESHOLD_DEFAULT = "500";

    /**
     * Set this property to false to use worker implementation for read acls update.
     *
     * @since 10.10-HF57
     */
    public static final String UPDATE_READ_ACL_BAF_IMPL_PROPERTY = "nuxeo.core.readacl.impl.baf";

    protected final DBSRepository repository;

    protected final DBSSession session;

    /** Retrieved and created document state. */
    protected Map<String, DBSDocumentState> transientStates = new HashMap<>();

    /** Ids of documents created but not yet saved. */
    protected Set<String> transientCreated = new LinkedHashSet<>();

    /**
     * Document ids modified as "user changes", which means that a change token should be checked.
     *
     * @since 9.2
     */
    protected final Set<Serializable> userChangeIds = new HashSet<>();

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

    protected final VersionAclMode versionAclMode;

    protected final boolean disableReadVersionPermission;

    public DBSTransactionState(DBSRepository repository, DBSSession session) {
        this.repository = repository;
        this.session = session;
        SecurityService securityService = Framework.getService(SecurityService.class);
        browsePermissions = new HashSet<>(Arrays.asList(securityService.getPermissionsToCheck(BROWSE)));
        versionAclMode = VersionAclMode.getConfiguration();
        disableReadVersionPermission = BaseSession.isReadVersionPermissionDisabled();
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
     * Returns whether or not the document with given {@code id} exists in the storage.
     *
     * @since 2021.12
     */
    public boolean exists(String id) {
        return repository.queryKeyValuePresence(KEY_ID, id, Collections.emptySet());
    }

    /**
     * Returns states and marks them transient, because they're about to be returned to user code (where they may be
     * modified).
     */
    public List<DBSDocumentState> getStatesForUpdate(Collection<String> ids) {
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
        return getChildrenStates(parentId, false, false);
    }

    public List<DBSDocumentState> getChildrenStates(String parentId, boolean excludeSpecialChildren,
            boolean excludeRegularChildren) {
        List<DBSDocumentState> docStates = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        Set<String> specialChildrenTypes = Framework.getService(SchemaManager.class).getSpecialDocumentTypes();
        boolean excludeChildren = excludeSpecialChildren || excludeRegularChildren;
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            if (excludeChildren) {
                boolean specialChild = specialChildrenTypes.contains(docState.getPrimaryType());
                if (excludeSpecialChildren && specialChild || excludeRegularChildren && !specialChild) {
                    continue;
                }
            }
            docStates.add(docState);
            seen.add(docState.getId());
        }
        List<State> states;
        if (!excludeChildren) {
            states = repository.queryKeyValue(KEY_PARENT_ID, parentId, seen);
        } else {
            // fetch from repository
            DBSQueryOperator operator = excludeSpecialChildren ? DBSQueryOperator.NOT_IN : DBSQueryOperator.IN;
            states = repository.queryKeyValueWithOperator(KEY_PARENT_ID, parentId, KEY_PRIMARY_TYPE, operator,
                    specialChildrenTypes, seen);
        }
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
        return getChildrenIds(parentId, false, false);
    }

    public List<String> getChildrenIds(String parentId, boolean excludeSpecialChildren,
            boolean excludeRegularChildren) {
        List<String> children = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> specialChildrenTypes = Framework.getService(SchemaManager.class).getSpecialDocumentTypes();
        boolean excludeChildren = excludeSpecialChildren || excludeRegularChildren;
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            String id = docState.getId();
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            if (excludeChildren) {
                boolean specialChild = specialChildrenTypes.contains(docState.getPrimaryType());
                if (excludeSpecialChildren && specialChild || excludeRegularChildren && !specialChild) {
                    continue;
                }
            }
            seen.add(id);
            children.add(id);
        }
        List<State> states;
        if (!excludeChildren) {
            states = repository.queryKeyValue(KEY_PARENT_ID, parentId, seen);
        } else {
            // fetch from repository depending on the filter flags
            DBSQueryOperator operator = excludeSpecialChildren ? DBSQueryOperator.NOT_IN : DBSQueryOperator.IN;
            states = repository.queryKeyValueWithOperator(KEY_PARENT_ID, parentId, KEY_PRIMARY_TYPE, operator,
                    specialChildrenTypes, seen);
        }
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
        if (transientStates.containsKey(id)) {
            throw new ConcurrentUpdateException(id);
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
            docState.put(KEY_SYS_CHANGE_TOKEN, INITIAL_SYS_CHANGE_TOKEN);
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
        Set<String> ids = new HashSet<>();
        ids.add(id);
        try (Stream<State> states = getDescendants(id, Collections.emptySet(), 0)) {
            states.forEach(state -> ids.add((String) state.get(KEY_ID)));
        }
        // we collect all ids first to avoid reentrancy to the repository
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

    protected int getReadAclsAsyncThreshold() {
        boolean enabled = Boolean.parseBoolean(
                Framework.getProperty(READ_ACL_ASYNC_ENABLED_PROPERTY, READ_ACL_ASYNC_ENABLED_DEFAULT));
        if (enabled) {
            return Integer.parseInt(
                    Framework.getProperty(READ_ACL_ASYNC_THRESHOLD_PROPERTY, READ_ACL_ASYNC_THRESHOLD_DEFAULT));
        } else {
            return 0;
        }
    }

    /**
     * Updates the Read ACLs recursively on a document.
     */
    public void updateTreeReadAcls(String id) {
        save(); // flush everything to the database

        // update the doc itself
        updateDocumentReadAcls(id);
        session.getVersionsIds(id).forEach(this::updateDocumentReadAcls);

        // check if we have a small enough number of descendants that we can process them synchronously
        int limit = getReadAclsAsyncThreshold();
        Set<String> ids = new HashSet<>();
        try (Stream<State> states = getDescendants(id, Collections.emptySet(), limit)) {
            states.forEach(state -> ids.add((String) state.get(KEY_ID)));
        }
        // Only count versions if we are still under the threshold
        if (isUnderSyncLimit(limit, ids)) {
            repository.queryKeyValueWithOperator(KEY_IS_VERSION, true, KEY_VERSION_SERIES_ID, DBSQueryOperator.IN, ids,
                    new HashSet<>()).forEach(state -> ids.add((String) state.get(KEY_ID)));
        }
        if (isUnderSyncLimit(limit, ids)) {
            // update all descendants synchronously
            ids.forEach(this::updateDocumentReadAcls);
        } else {
            // update the direct children synchronously, the rest asynchronously

            // update the direct children (with a limit in case it's too big)
            String nxql = String.format("SELECT ecm:uuid FROM Document WHERE ecm:parentId = '%s'", id);
            NuxeoPrincipal principal = new SystemPrincipal(null);
            QueryFilter queryFilter = new QueryFilter(principal, null, null, null, Collections.emptyList(), limit, 0);
            PartialList<Map<String, Serializable>> pl = session.queryProjection(nxql, NXQL.NXQL, queryFilter, false, 0,
                    new Object[0]);
            for (Map<String, Serializable> map : pl) {
                String childId = (String) map.get(NXQL.ECM_UUID);
                updateDocumentReadAcls(childId);
            }

            nxql = String.format("SELECT ecm:uuid FROM Document WHERE ecm:ancestorId = '%s'", id);
            if (Framework.isBooleanPropertyFalse(UPDATE_READ_ACL_BAF_IMPL_PROPERTY)) {
                // asynchronous work to do the whole tree
                Work work = new FindReadAclsWork(repository.getName(), nxql, null);
                Framework.getService(WorkManager.class).schedule(work);
            } else {
                // bulk action to do the whole tree (default behavior if UPDATE_READ_ACL_BAF_IMPL_PROPERTY unset)
                BulkService service = Framework.getService(BulkService.class);
                BulkCommand command = new BulkCommand.Builder(UPDATE_READ_ACLS_ACTION, nxql).user(
                        SYSTEM_USERNAME).repository(session.getRepositoryName()).build();
                service.submitTransactional(command);
            }
        }
    }

    protected boolean isUnderSyncLimit(int limit, Set<String> ids) {
        return limit == 0 || ids.size() < limit;
    }

    /**
     * Work to find the ids of documents for which Read ACLs must be recomputed, and launch the needed update works.
     *
     * @since 9.10
     * @deprecated since 2021.11 use {@link UpdateReadAclsAction} instead
     */
    @Deprecated
    public static class FindReadAclsWork extends BatchFinderWork {

        private static final long serialVersionUID = 1L;

        public FindReadAclsWork(String repositoryName, String nxql, String originatingUsername) {
            super(repositoryName, nxql, originatingUsername);
        }

        @Override
        public String getTitle() {
            return "Find descendants for Read ACLs";
        }

        @Override
        public String getCategory() {
            return "security";
        }

        @Override
        public int getBatchSize() {
            return 500;
        }

        @Override
        public Work getBatchProcessorWork(List<String> docIds) {
            return new UpdateReadAclsWork(repositoryName, docIds, getOriginatingUsername());
        }
    }

    /**
     * Work to update the Read ACLs on a list of documents, without recursion.
     *
     * @since 9.10
     * @deprecated since 2021.11 use {@link UpdateReadAclsAction} instead
     */
    @Deprecated
    public static class UpdateReadAclsWork extends BatchProcessorWork {

        private static final long serialVersionUID = 1L;

        public UpdateReadAclsWork(String repositoryName, List<String> docIds, String originatingUsername) {
            super(repositoryName, docIds, originatingUsername);
        }

        @Override
        public String getTitle() {
            return "Update Read ACLs";
        }

        @Override
        public String getCategory() {
            return "security";
        }

        @Override
        public int getBatchSize() {
            return 50;
        }

        @Override
        public void processBatch(List<String> docIds) {
            session.updateReadACLs(docIds);
        }
    }

    /**
     * Updates the Read ACLs on a document (not recursively), bypassing transient space and caches for the document
     * itself (not the ancestors, needed for ACL inheritance and for which caching is useful).
     */
    public void updateReadACLs(Collection<String> docIds) {
        docIds.forEach(id -> updateDocumentReadAclsNoCache(id));
    }

    /**
     * Updates the Read ACLs on a document (not recursively)
     */
    protected void updateDocumentReadAcls(String id) {
        DBSDocumentState docState = getStateForUpdate(id);
        docState.put(KEY_READ_ACL, getReadACL(docState.getState()));
    }

    /**
     * Updates the Read ACLs on a document, without polluting caches.
     * <p>
     * When fetching parents recursively to compute inheritance, the regular transient space and repository caching are
     * used.
     */
    protected void updateDocumentReadAclsNoCache(String id) {
        // no transient for state read, and we don't want to trash caches
        // fetch from repository only the properties needed for Read ACL computation and recursion
        State state = repository.readPartialState(id, READ_ACL_RECURSION_KEYS);
        if (state == null) {
            return;
        }
        State oldState = new State(1);
        oldState.put(KEY_READ_ACL, state.get(KEY_READ_ACL));
        // compute new value
        State newState = new State(1);
        newState.put(KEY_READ_ACL, getReadACL(state));
        StateDiff diff = StateHelper.diff(oldState, newState);
        if (!diff.isEmpty()) {
            // no transient for state write, we write directly and just invalidate caches
            repository.updateState(id, diff, null);
        }
    }

    /**
     * Gets the Read ACL (flat list of users having browse permission, including inheritance) on a document.
     */
    protected String[] getReadACL(State state) {
        Set<String> racls = new HashSet<>();
        boolean replaceReadVersionPermission = false;
        if (TRUE.equals(state.get(KEY_IS_VERSION))) {
            replaceReadVersionPermission = !disableReadVersionPermission;
            if (versionAclMode == VersionAclMode.DISABLED) {
                String versionSeriesId = (String) state.get(KEY_VERSION_SERIES_ID);
                if (versionSeriesId == null || (state = getStateForRead(versionSeriesId)) == null) {
                    // version with no live doc
                    return new String[0];
                }
            }
        }
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
                        Long status = (Long) aceMap.get(KEY_ACE_STATUS);
                        if (replaceReadVersionPermission && READ_VERSION.equals(permission)) {
                            permission = READ;
                        }
                        if (TRUE.equals(granted) && browsePermissions.contains(permission)
                                && (status == null || status == 1)) {
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
            // get the parent; for a version the parent is the live document
            String parentKey;
            if (TRUE.equals(state.get(KEY_IS_VERSION))) {
                replaceReadVersionPermission = !disableReadVersionPermission;
                parentKey = KEY_VERSION_SERIES_ID;
            } else {
                parentKey = KEY_PARENT_ID;
            }
            String parentId = (String) state.get(parentKey);
            state = parentId == null ? null : getStateForRead(parentId);
        } while (state != null);

        // sort to have canonical order
        List<String> racl = new ArrayList<>(racls);
        Collections.sort(racl);
        return racl.toArray(new String[racl.size()]);
    }

    protected Stream<State> getDescendants(String id, Set<String> keys, int limit) {
        return repository.getDescendants(id, keys, limit);
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

    public void markUserChange(String id) {
        userChangeIds.add(id);
    }

    /**
     * Writes transient state to database.
     * <p>
     * An undo log is kept in order to rollback the transaction later if needed.
     */
    public void save() {
        updateProxies();
        updateDenormalizedState();
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
                try {
                    if (undoLog != null) {
                        if (!undoLog.containsKey(id)) {
                            undoLog.put(id, StateHelper.deepCopy(docState.getOriginalState()));
                        }
                        // else there's already a create or an update in the undo log so original info is enough
                    }
                    ConditionalUpdates conditionalUpdates = null;
                    if (session.changeTokenEnabled) {
                        // increment system change token
                        Long base = (Long) docState.get(KEY_SYS_CHANGE_TOKEN);
                        docState.put(KEY_SYS_CHANGE_TOKEN, DeltaLong.valueOf(base, 1));
                        diff.put(KEY_SYS_CHANGE_TOKEN, DeltaLong.valueOf(base, 1));
                        // update change token if applicable (user change)
                        if (userChangeIds.contains(id)) {
                            conditionalUpdates = getConditionalUpdateForChangeToken(docState);
                        }
                    }
                    repository.updateState(id, diff, conditionalUpdates);
                } finally {
                    docState.setNotDirty();
                }
            }
        }
        transientCreated.clear();
        userChangeIds.clear();
        scheduleWork(works);
    }

    protected ConditionalUpdates getConditionalUpdateForChangeToken(DBSDocumentState docState) {
        ConditionalUpdates conditionalUpdates = new ConditionalUpdates();
        // condition and update
        Long oldToken = (Long) docState.getOriginalState().get(KEY_CHANGE_TOKEN);
        Long newToken = updateChangeToken(oldToken);
        conditionalUpdates.put(Collections.singletonMap(KEY_CHANGE_TOKEN, oldToken),
                Collections.singletonMap(KEY_CHANGE_TOKEN, newToken));
        // callback
        Runnable callback = () -> {
            // also store the new token in the state (without marking dirty)
            Long ot = (Long) conditionalUpdates.getUpdates().get(KEY_CHANGE_TOKEN);
            docState.getState().put(KEY_CHANGE_TOKEN, ot);
            // new conditional update
            Long nt = updateChangeToken(ot);
            conditionalUpdates.put(Collections.singletonMap(KEY_CHANGE_TOKEN, ot),
                    Collections.singletonMap(KEY_CHANGE_TOKEN, nt));
        };
        conditionalUpdates.addCallback(callback);
        return conditionalUpdates;
    }

    protected static Long updateChangeToken(Long changeToken) {
        if (changeToken == null) {
            // document without change token, just created
            return INITIAL_CHANGE_TOKEN;
        } else {
            return BaseDocument.updateChangeToken(changeToken);
        }
    }

    /**
     * Logic to get the conditions to use to match and update a change token.
     * <p>
     * This may be called several times for a single DBS document update, because the low-level storage may need several
     * database updates for a single high-level update in some cases.
     *
     * @since 9.1
     */
    public static class ConditionalUpdates {

        protected final Map<String, Serializable> conditions = new HashMap<>();

        protected final Map<String, Serializable> updates = new HashMap<>();

        protected final List<Runnable> callbacks = new ArrayList<>();

        /**
         * Puts condition and update.
         */
        public void put(Map<String, Serializable> condition, Map<String, Serializable> update) {
            conditions.putAll(condition);
            updates.putAll(update);
        }

        /**
         * Adds a callback.
         * <p>
         * The callback is useful to 1. finish updating internal in-memory document state after an update is done, and
         * 2. prepare a new condition/update if the conditional update is used again.
         */
        public void addCallback(Runnable callback) {
            callbacks.add(callback);
        }

        /**
         * Gets the conditions to use to match a change token.
         */
        public Map<String, Serializable> getConditions() {
            return conditions;
        }

        /**
         * Gets the updates to make to write the updated change token.
         */
        public Map<String, Serializable> getUpdates() {
            return updates;
        }

        /**
         * To be called by the processor after each update.
         */
        public void finish() {
            callbacks.forEach(Runnable::run);
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
            log.debug("Proxy " + proxyId + " concurrently deleted");
            return;
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
        case KEY_SYS_CHANGE_TOKEN:
        case KEY_CHANGE_TOKEN:
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

    protected void updateDenormalizedState() {
        BlobKeysFinder blobKeysFinder = new BlobKeysFinder(repository.getBlobKeysPaths());
        for (DBSDocumentState docState : transientStates.values()) {
            computeBlobKeys(docState, blobKeysFinder);
        }
    }

    protected void computeBlobKeys(DBSDocumentState docState, BlobKeysFinder blobKeysFinder) {
        Set<String> blobKeys = blobKeysFinder.findBlobKeys(docState.getState());
        Object[] blobKeysArray;
        if (blobKeys.isEmpty()) {
            blobKeysArray = null;
        } else {
            blobKeysArray = blobKeys.toArray();
            Arrays.sort(blobKeysArray);
        }
        if (!Objects.deepEquals(blobKeysArray, docState.get(KEY_BLOB_KEYS))) {
            docState.put(KEY_BLOB_KEYS, blobKeysArray);
        }
    }

    /**
     * Iterates on a {@link State} to find the blob keys.
     */
    protected static class BlobKeysFinder {

        protected final List<List<String>> blobKeysPaths;

        protected final Set<String> blobKeys = new HashSet<>();

        public BlobKeysFinder(List<List<String>> blobKeysPaths) {
            this.blobKeysPaths = blobKeysPaths;
        }

        public Set<String> findBlobKeys(State state) {
            blobKeys.clear();
            blobKeysPaths.forEach(path -> findBlobKeys(state, path, 0));
            return blobKeys;
        }

        protected void findBlobKeys(State state, List<String> path, int start) {
            int size = path.size();
            for (int i = start; i < size; i++) {
                Serializable value = state.get(path.get(i));
                if (value == null) {
                    // fast path when the property doesn't exist
                    return;
                }
                if (value instanceof State) {
                    state = (State) value;
                    continue; // process next path element
                }
                if (i == size - 1) {
                    // end of path
                    if (value instanceof String) {
                        blobKeys.add((String) value);
                    } else if (value instanceof Object[]) {
                        // array of naked blob keys (no current use case)
                        for (Object v : (Object[]) value) {
                            if (v instanceof String) {
                                blobKeys.add((String) v);
                            }
                        }
                    }
                } else if (value instanceof List) {
                    // process all complex list elements
                    for (Object v : (List<?>) value) {
                        if (v instanceof State) {
                            findBlobKeys((State) v, path, i + 1);
                        }
                    }
                }
                return;
            }
        }
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
        DownloadBlobGuard.disable();
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
        DownloadBlobGuard.disable();
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
        if (repository.getFulltextConfiguration().fulltextSearchDisabled) {
            // We only need to update dirty simple strings if fulltext search is not disabled
            // because in that case Elasticsearch will do its own extraction/indexing.
            // We need to detect dirty binary strings in all cases, because Elasticsearch
            // will need them even if the repository itself doesn't use them for search.
            docsWithDirtyStrings = Collections.emptySet();
        }
        Set<String> dirtyIds = new HashSet<>();
        dirtyIds.addAll(docsWithDirtyStrings);
        dirtyIds.addAll(docsWithDirtyBinaries);
        if (dirtyIds.isEmpty()) {
            return Collections.emptyList();
        }
        markIndexingInProgress(dirtyIds);
        List<Work> works = new ArrayList<>(dirtyIds.size());
        boolean okToDownloadBlob = !DownloadBlobGuard.isEnable();
        for (String id : dirtyIds) {
            boolean updateSimpleText = docsWithDirtyStrings.contains(id);
            boolean updateBinaryText = okToDownloadBlob && docsWithDirtyBinaries.contains(id);
            if (updateSimpleText || updateBinaryText) {
                Work work = new FulltextExtractorWork(repository.getName(), id, updateSimpleText, updateBinaryText, true);
                works.add(work);
            }
        }
        return works;
    }

    protected void markIndexingInProgress(Set<String> ids) {
        FulltextConfiguration fulltextConfiguration = repository.getFulltextConfiguration();
        for (DBSDocumentState docState : getStatesForUpdate(ids)) {
            if (!fulltextConfiguration.isFulltextIndexable(docState.getPrimaryType())) {
                continue;
            }
            docState.put(KEY_FULLTEXT_JOBID, docState.getId());
        }
    }

    /**
     * Finds the documents having dirty text or dirty binaries that have to be reindexed as fulltext.
     *
     * @param docsWithDirtyStrings set of ids, updated by this method
     * @param docsWithDirtyBinaries set of ids, updated by this method
     */
    protected void findDirtyDocuments(Set<String> docsWithDirtyStrings, Set<String> docsWithDirtyBinaries) {
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
                docsWithDirtyBinaries.add(docState.getId());
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
            if (value.pull != null) {
                findDirtyPaths(value.pull, newPath);
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

    protected void scheduleWork(List<Work> works) {
        // do async fulltext indexing only if high-level sessions are available
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        if (repositoryManager != null && !works.isEmpty()) {
            WorkManager workManager = Framework.getService(WorkManager.class);
            for (Work work : works) {
                // schedule work post-commit
                // in non-tx mode, this may execute it nearly immediately
                workManager.schedule(work, Scheduling.IF_NOT_SCHEDULED, true);
            }
        }
    }

}
