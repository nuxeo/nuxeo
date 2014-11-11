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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_GRANT;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_PERMISSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_USER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_BASE_VERSION_ID;
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
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.StateHelper;
import org.nuxeo.ecm.core.storage.DefaultFulltextParser;
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.FulltextParser;
import org.nuxeo.ecm.core.storage.FulltextUpdaterWork;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.FulltextUpdaterWork.IndexAndText;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;

/**
 * Transactional state for a session.
 * <p>
 * Until {@code save()} is called, data lives in the transient map.
 * <p>
 * Upon save, data is written to the repository, even though it has not yet been
 * committed (this means that other sessions can read uncommitted data). It's
 * also kept in an undo log in order for rollback to be possible.
 * <p>
 * On commit, the undo log is forgotten. On rollback, the undo log is replayed.
 *
 * @since 5.9.4
 */
public class DBSTransactionState {

    private static final Log log = LogFactory.getLog(DBSTransactionState.class);

    protected final DBSRepository repository;

    protected final DBSSession session;

    /** Retrieved and created document state. */
    protected Map<String, DBSDocumentState> transientStates = new HashMap<String, DBSDocumentState>();

    /** Ids of documents created but not yet saved. */
    protected Set<String> transientCreated = new LinkedHashSet<String>();

    /* TODO undo log */

    protected final Set<String> browsePermissions;

    public DBSTransactionState(DBSRepository repository, DBSSession session) {
        this.repository = repository;
        this.session = session;
        SecurityService securityService = Framework.getLocalService(SecurityService.class);
        browsePermissions = new HashSet<>(
                Arrays.asList(securityService.getPermissionsToCheck(BROWSE)));
    }

    protected FulltextConfiguration getFulltextConfiguration() {
        // TODO get from DBS repo service
        FulltextConfiguration fulltextConfiguration = new FulltextConfiguration();
        fulltextConfiguration.indexNames.add("default");
        fulltextConfiguration.indexesAllBinary.add("default");
        return fulltextConfiguration;
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
     * Returns a state and marks it as transient, because it's about to be
     * modified or returned to user code (where it may be modified).
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
     * Returns states and marks them transient, because they're about to be
     * returned to user code (where they may be modified).
     */
    public List<DBSDocumentState> getStatesForUpdate(List<String> ids) {
        // check which ones we have to fetch from repository
        List<String> idsToFetch = new LinkedList<String>();
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
        List<DBSDocumentState> docStates = new ArrayList<DBSDocumentState>(
                ids.size());
        for (String id : ids) {
            DBSDocumentState docState = transientStates.get(id);
            if (docState != null) {
                docStates.add(docState);
            } else {
                log.warn("Cannot fetch document with id: " + id, new Throwable(
                        "debug stack trace"));
            }
        }
        return docStates;
    }

    // XXX TODO for update or for read?
    public DBSDocumentState getChildState(String parentId, String name) {
        Set<String> seen = new HashSet<String>();
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            seen.add(docState.getId());
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            if (!name.equals(docState.getName())) {
                continue;
            }
            return docState;
        }
        // fetch from repository
        State state = repository.readChildState(parentId, name, seen);
        return newTransientState(state);
    }

    public boolean hasChild(String parentId, String name) {
        Set<String> seen = new HashSet<String>();
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            seen.add(docState.getId());
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            if (!name.equals(docState.getName())) {
                continue;
            }
            return true;
        }
        // check repository
        return repository.hasChild(parentId, name, seen);
    }

    public List<DBSDocumentState> getChildrenStates(String parentId) {
        List<DBSDocumentState> docStates = new LinkedList<DBSDocumentState>();
        Set<String> seen = new HashSet<String>();
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            seen.add(docState.getId());
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            docStates.add(docState);
        }
        // fetch from repository
        List<State> states = repository.queryKeyValue(KEY_PARENT_ID, parentId,
                seen);
        for (State state : states) {
            docStates.add(newTransientState(state));
        }
        return docStates;
    }

    public List<String> getChildrenIds(String parentId) {
        List<String> children = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            String id = docState.getId();
            seen.add(id);
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            children.add(id);
        }
        // fetch from repository
        List<State> states = repository.queryKeyValue(KEY_PARENT_ID, parentId,
                seen);
        for (State state : states) {
            children.add((String) state.get(KEY_ID));
        }
        return new ArrayList<String>(children);
    }

    public boolean hasChildren(String parentId) {
        Set<String> seen = new HashSet<String>();
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            seen.add(docState.getId());
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            return true;
        }
        // check repository
        return repository.queryKeyValuePresence(KEY_PARENT_ID, parentId, seen);
    }

    public DBSDocumentState createChild(String id, String parentId,
            String name, Long pos, String typeName) {
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
        // update read acls for new doc
        updateReadAcls(id);
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
        // other fields updated by the caller
        transientStates.put(copyId, copyState);
        transientCreated.add(copyId);
        return copyState;
    }

    /**
     * Updates ancestors recursively after a move.
     * <p>
     * Recursing from given doc, replace the first ndel ancestors with those
     * passed.
     * <p>
     * Doesn't check transient (assumes save is done). The modifications are
     * automatically saved.
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
                System.arraycopy(ancestors, ndel, newAncestors, nadd,
                        ancestors.length - ndel);
            }
            docState.put(KEY_ANCESTOR_IDS, newAncestors);
        }
    }

    /**
     * Updates the Read ACLs recursively on a document.
     */
    public void updateReadAcls(String id) {
        // versions too XXX TODO
        Set<String> ids = getSubTree(id, null, null);
        ids.add(id);
        for (String cid : ids) {
            // XXX TODO oneShot update, don't pollute transient space
            DBSDocumentState docState = getStateForUpdate(cid);
            docState.put(KEY_READ_ACL, getReadACL(docState));
        }
    }

    /**
     * Gets the Read ACL (flat list of users having browse permission, including
     * inheritance) on a document.
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
                        if (TRUE.equals(granted)
                                && browsePermissions.contains(permission)) {
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
                state = versionSeriesId == null ? null
                        : getStateForRead(versionSeriesId);
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
     * @param proxyTargets returns a map of proxy to target among the documents
     *            found
     * @param targetProxies returns a map of target to proxies among the
     *            document found
     */
    protected Set<String> getSubTree(String id,
            Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies) {
        Set<String> ids = new HashSet<String>();
        // check repository
        repository.queryKeyValueArray(KEY_ANCESTOR_IDS, id, ids, proxyTargets,
                targetProxies);
        return ids;
    }

    public List<DBSDocumentState> getKeyValuedStates(String key, String value) {
        List<DBSDocumentState> docStates = new LinkedList<DBSDocumentState>();
        Set<String> seen = new HashSet<String>();
        // check transient state
        for (DBSDocumentState docState : transientStates.values()) {
            seen.add(docState.getId());
            if (!value.equals(docState.get(key))) {
                continue;
            }
            docStates.add(docState);
        }
        // fetch from repository
        List<State> states = repository.queryKeyValue(key, value, seen);
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
    public void removeStates(Set<String> ids) throws DocumentException {
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
    public void save() throws DocumentException {
        updateProxies();
        // TODO getting fulltext already does a getStateChange
        List<Work> works = getFulltextWorks();
        for (String id : transientCreated) { // ordered
            DBSDocumentState docState = transientStates.get(id);
            docState.setNotDirty();
            repository.createState(docState.getState());
            // TODO undo log
        }
        for (DBSDocumentState docState : transientStates.values()) {
            String id = docState.getId();
            if (transientCreated.contains(id)) {
                continue; // already done
            }
            StateDiff diff = docState.getStateChange();
            docState.setNotDirty();
            if (diff != null) {
                repository.updateState(id, diff);
                // TODO undo log
            }
        }
        transientCreated.clear();
        scheduleWork(works);
    }

    /**
     * Checks if the changed documents are proxy targets, and updates the
     * proxies if that's the case.
     */
    protected void updateProxies() {
        for (String id : transientCreated) { // ordered
            DBSDocumentState docState = transientStates.get(id);
            Object[] proxyIds = (Object[]) docState.get(KEY_PROXY_IDS);
            if (proxyIds != null) {
                for (Object proxyId : proxyIds) {
                    updateProxy(docState, (String) proxyId);
                }
            }
        }
        // copy as we may modify proxies
        for (String id : transientStates.keySet().toArray(new String[0])) {
            DBSDocumentState docState = transientStates.get(id);
            if (transientCreated.contains(id)) {
                continue; // already done
            }
            if (docState.isDirty()) {
                Object[] proxyIds = (Object[]) docState.get(KEY_PROXY_IDS);
                if (proxyIds != null) {
                    for (Object proxyId : proxyIds) {
                        updateProxy(docState, (String) proxyId);
                    }
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
            throw new NullPointerException(proxyId);
        }
        // clear all proxy data
        for (String key : proxy.getState().keySet().toArray(new String[0])) {
            if (!isProxySpecific(key)) {
                proxy.put(key, null);
            }
        }
        // copy from target
        for (Entry<String, Serializable> en : target.getState().entrySet()) {
            String key = en.getKey();
            if (!isProxySpecific(key)) {
                proxy.put(key, StateHelper.deepCopy(en.getValue()));
            }
        }
    }

    /**
     * Things that we don't touch on a proxy when updating it.
     */
    protected boolean isProxySpecific(String key) {
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
        case KEY_BASE_VERSION_ID:
        case KEY_VERSION_SERIES_ID:
        case KEY_PROXY_IDS:
            return true;
        }
        return false;
    }

    /**
     * Saves and flushes to database.
     */
    public void commit() throws DocumentException {
        save();
        commitSave();
    }

    /**
     * Commits the saved state to the database.
     */
    protected void commitSave() throws DocumentException {
        // clear transient, this means that after this references to states
        // will be stale
        // TODO mark states as invalid
        clearTransient();
        // TODO clear undo log
    }

    /**
     * Rolls back the save state by applying the undo log.
     */
    public void rollback() {
        clearTransient();
        // TODO apply undo log
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
        Set<String> docsWithDirtyStrings = new HashSet<String>();
        Set<String> docsWithDirtyBinaries = new HashSet<String>();
        findDirtyDocuments(docsWithDirtyStrings, docsWithDirtyBinaries);
        if (docsWithDirtyStrings.isEmpty() && docsWithDirtyBinaries.isEmpty()) {
            return Collections.emptyList();
        }
        List<Work> works = new LinkedList<Work>();
        getFulltextSimpleWorks(works, docsWithDirtyStrings);
        getFulltextBinariesWorks(works, docsWithDirtyBinaries);
        return works;
    }

    /**
     * Finds the documents having dirty text or dirty binaries that have to be
     * reindexed as fulltext.
     *
     * @param docsWithDirtyStrings set of ids, updated by this method
     * @param docWithDirtyBinaries set of ids, updated by this method
     */
    protected void findDirtyDocuments(Set<String> docsWithDirtyStrings,
            Set<String> docWithDirtyBinaries) {
        for (String id : transientCreated) {
            docsWithDirtyStrings.add(id);
            docWithDirtyBinaries.add(id);
        }
        for (DBSDocumentState docState : transientStates.values()) {
            // TODO finer-grained dirty state
            if (docState.isDirtyIgnoringFulltext()) {
                String id = docState.getId();
                docsWithDirtyStrings.add(id);
                docWithDirtyBinaries.add(id);
            }
        }
    }

    protected void getFulltextSimpleWorks(List<Work> works,
            Set<String> docsWithDirtyStrings) {
        // TODO XXX make configurable, see also FulltextExtractorWork
        FulltextParser fulltextParser = new DefaultFulltextParser();
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

            FulltextConfiguration config = getFulltextConfiguration();
            if (!config.isFulltextIndexable(documentType)) {
                continue;
            }
            docState.put(KEY_FULLTEXT_JOBID, docState.getId());
            FulltextFinder fulltextFinder = new FulltextFinder(fulltextParser,
                    docState, session);
            List<IndexAndText> indexesAndText = new LinkedList<IndexAndText>();
            for (String indexName : config.indexNames) {
                // TODO paths from config
                String text = fulltextFinder.findFulltext(indexName);
                indexesAndText.add(new IndexAndText(indexName, text));
            }
            if (!indexesAndText.isEmpty()) {
                Work work = new FulltextUpdaterWork(repository.getName(), id,
                        true, false, indexesAndText);
                works.add(work);
            }
        }
    }

    protected void getFulltextBinariesWorks(List<Work> works,
            Set<String> docWithDirtyBinaries) {
        if (docWithDirtyBinaries.isEmpty()) {
            return;
        }

        // TODO get from extension point, see also FulltextExtractorWork
        // XXX hardcoded config for now
        FulltextConfiguration config = getFulltextConfiguration();

        // mark indexing in progress, so that future copies (including versions)
        // will be indexed as well
        for (String id : docWithDirtyBinaries) {
            DBSDocumentState docState = getStateForUpdate(id);
            if (docState == null) {
                // cannot happen
                continue;
            }
            if (!config.isFulltextIndexable(docState.getPrimaryType())) {
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
        public FulltextFinder(FulltextParser fulltextParser,
                DBSDocumentState document, DBSSession session) {
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
            List<String> strings = new ArrayList<String>();
            findFulltext(indexName, document.getState(), strings);
            return StringUtils.join(strings, ' ');
        }

        protected void findFulltext(String indexName, State state,
                List<String> strings) {
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
