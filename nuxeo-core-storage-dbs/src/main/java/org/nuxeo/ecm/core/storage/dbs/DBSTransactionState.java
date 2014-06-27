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
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_POS;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.storage.CopyHelper;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.dbs.FulltextUpdaterWork.IndexAndText;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;

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

    protected final DBSSession session;

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

    protected final Set<String> browsePermissions;

    public DBSTransactionState(DBSRepository repository, DBSSession session) {
        this.repository = repository;
        this.session = session;
        SecurityService securityService = Framework.getLocalService(SecurityService.class);
        browsePermissions = new HashSet<>(
                Arrays.asList(securityService.getPermissionsToCheck(BROWSE)));
    }

    protected DBSDocumentState makeTransient(DBSDocumentState docState) {
        String id = docState.getId();
        if (transientStates.containsKey(id)) {
            throw new IllegalStateException("Already transient: " + id);
        }
        docState = new DBSDocumentState(docState); // copy
        transientStates.put(id, docState);
        return docState;
    }

    protected DBSDocumentState returnTransient(State state) {
        String id = (String) state.get(KEY_ID);
        if (transientStates.containsKey(id)) {
            throw new IllegalStateException("Already transient: " + id);
        }
        DBSDocumentState docState = new DBSDocumentState(state); // copy
        transientStates.put(id, docState);
        return docState;
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
        // check saved state
        if (savedDeleted.contains(id)) {
            return null;
        }
        docState = savedStates.get(id);
        if (docState != null) {
            return makeTransient(docState);
        }
        // fetch from repository
        State state = repository.readState(id);
        if (state != null) {
            return returnTransient(state);
        }
        return null;
    }

    /**
     * Returns a saved state.
     */
    public DBSDocumentState getStateForNonTransientUpdate(String id) {
        // check saved state
        if (savedDeleted.contains(id)) {
            return null;
        }
        DBSDocumentState docState = savedStates.get(id);
        if (docState != null) {
            return docState;
        }
        // fetch from repository
        State state = repository.readState(id);
        if (state != null) {
            // keep as saved state, as we'll want to flush it after updates
            docState = new DBSDocumentState(state);
            savedStates.put(id, docState);
            return docState;
        }
        return null;
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
        // check saved state
        if (savedDeleted.contains(id)) {
            return null;
        }
        docState = savedStates.get(id);
        if (docState != null) {
            return docState.getState();
        }
        // fetch from repository
        State state = repository.readState(id);
        if (state != null) {
            return state;
        }
        return null;
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
            // check saved state
            if (savedDeleted.contains(id)) {
                continue;
            }
            docState = savedStates.get(id);
            if (docState != null) {
                makeTransient(docState);
                continue;
            }
            // will have to fetch it
            idsToFetch.add(id);
        }
        if (!idsToFetch.isEmpty()) {
            List<State> states = repository.readStates(idsToFetch);
            for (State state : states) {
                if (state != null) {
                    returnTransient(state);
                }
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
        if (savedDeleted.contains(parentId)) {
            return null;
        }
        Set<String> seen = new HashSet<String>();
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
        for (DBSDocumentState docState : savedStates.values()) {
            if (!seen.add(docState.getId())) {
                // already seen
                continue;
            }
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            if (!name.equals(docState.getName())) {
                continue;
            }
            return makeTransient(docState);
        }
        State state = repository.readChildState(parentId, name, seen);
        if (state != null) {
            return returnTransient(state);
        }
        return null;
    }

    public boolean hasChild(String parentId, String name) {
        if (savedDeleted.contains(parentId)) {
            return false;
        }
        Set<String> seen = new HashSet<String>();
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
        for (DBSDocumentState docState : savedStates.values()) {
            if (!seen.add(docState.getId())) {
                // already seen
                continue;
            }
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            if (!name.equals(docState.getName())) {
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
        Set<String> seen = new HashSet<String>();
        for (DBSDocumentState docState : transientStates.values()) {
            seen.add(docState.getId());
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            children.add(docState);
        }
        for (DBSDocumentState docState : savedStates.values()) {
            if (!seen.add(docState.getId())) {
                // already seen
                continue;
            }
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            docState = makeTransient(docState);
            children.add(docState);
        }
        List<State> states = repository.queryKeyValue(KEY_PARENT_ID, parentId,
                seen);
        for (State state : states) {
            DBSDocumentState docState = returnTransient(state);
            children.add(docState);
        }
        return children;
    }

    public List<String> getChildrenIds(String parentId) {
        if (savedDeleted.contains(parentId)) {
            return Collections.emptyList();
        }
        List<String> children = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        for (DBSDocumentState docState : transientStates.values()) {
            String id = docState.getId();
            seen.add(id);
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            children.add(id);
        }
        for (DBSDocumentState docState : savedStates.values()) {
            String id = docState.getId();
            if (!seen.add(id)) {
                // already seen
                continue;
            }
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            children.add(id);
        }
        List<State> states = repository.queryKeyValue(KEY_PARENT_ID, parentId,
                seen);
        for (State state : states) {
            children.add((String) state.get(KEY_ID));
        }
        return new ArrayList<String>(children);
    }

    public boolean hasChildren(String parentId) {
        if (savedDeleted.contains(parentId)) {
            return false;
        }
        Set<String> seen = new HashSet<String>();
        for (DBSDocumentState docState : transientStates.values()) {
            seen.add(docState.getId());
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            return true;
        }
        for (DBSDocumentState docState : savedStates.values()) {
            if (!seen.add(docState.getId())) {
                // already seen
                continue;
            }
            if (!parentId.equals(docState.getParentId())) {
                continue;
            }
            return true;
        }
        return repository.queryKeyValuePresence(KEY_PARENT_ID, parentId, seen);
    }

    // id may be not-null for import
    public DBSDocumentState createChild(String id, String parentId,
            String name, Long pos, String typeName) {
        id = generateNewId(id);
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
     * Doesn't check transient (assumes save is done). The copy is automatically
     * saved.
     */
    public DBSDocumentState copy(String id) {
        DBSDocumentState copyState = new DBSDocumentState(getStateForRead(id));
        String copyId = generateNewId(null);
        copyState.put(KEY_ID, copyId);
        // other fields updated by the caller
        savedStates.put(copyId, copyState);
        savedCreated.add(copyId);
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
        Set<String> seen = new HashSet<String>(savedDeleted);
        DOC: //
        for (DBSDocumentState docState : savedStates.values()) {
            String oid = docState.getId();
            seen.add(oid);
            Object[] ancestors = (Object[]) docState.get(KEY_ANCESTOR_IDS);
            if (ancestors != null) {
                for (Object aid : ancestors) {
                    if (id.equals(aid)) {
                        ids.add(oid);
                        if (proxyTargets != null
                                && TRUE.equals(docState.get(KEY_IS_PROXY))) {
                            String targetId = (String) docState.get(KEY_PROXY_TARGET_ID);
                            proxyTargets.put(oid, targetId);
                        }
                        if (targetProxies != null) {
                            Object[] proxyIds = (Object[]) docState.get(KEY_PROXY_IDS);
                            if (proxyIds != null) {
                                targetProxies.put(oid, proxyIds);
                            }
                        }
                        continue DOC;
                    }
                }
            }
        }
        // check repository as well
        repository.queryKeyValueArray(KEY_ANCESTOR_IDS, id, ids, proxyTargets,
                targetProxies, seen);
        return ids;
    }

    protected String generateNewId(String id) {
        if (id == null) {
            id = repository.generateNewId();
        }
        return id;
    }

    public List<DBSDocumentState> getKeyValuedStates(String key, String value) {
        List<DBSDocumentState> docStates = new LinkedList<DBSDocumentState>();
        Set<String> seen = new HashSet<String>();
        for (DBSDocumentState docState : transientStates.values()) {
            seen.add(docState.getId());
            if (!value.equals(docState.get(key))) {
                continue;
            }
            docStates.add(docState);
        }
        for (DBSDocumentState docState : savedStates.values()) {
            if (!seen.add(docState.getId())) {
                // already seen
                continue;
            }
            if (!value.equals(docState.get(key))) {
                continue;
            }
            docState = makeTransient(docState);
            docStates.add(docState);
        }
        List<State> states = repository.queryKeyValue(key, value, seen);
        for (State state : states) {
            DBSDocumentState docState = returnTransient(state);
            docStates.add(docState);
        }
        return docStates;
    }

    /**
     * Saves transient state to saved state.
     * <p>
     * Dirty states are saved, all states are kept in transient because there
     * may be references to them.
     */
    public void save() {
        List<Work> works = getFulltextWorks();
        updateProxies();
        for (String id : transientCreated) { // ordered
            DBSDocumentState docState = transientStates.get(id);
            docState.setNotDirty();
            savedStates.put(id, new DBSDocumentState(docState)); // copy
            savedCreated.add(id);
        }
        for (DBSDocumentState docState : transientStates.values()) {
            String id = docState.getId();
            if (transientCreated.contains(id)) {
                continue; // already done
            }
            if (docState.isDirty()) {
                docState.setNotDirty();
                savedStates.put(id, new DBSDocumentState(docState)); // copy
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
            throw new NullPointerException();
        }
        // clear all proxy data
        for (String key : proxy.getState().keySet().toArray(new String[0])) {
            if (!proxySpecific(key)) {
                proxy.put(key, null);
            }
        }
        // copy from target
        for (Entry<String, Serializable> en : target.getState().entrySet()) {
            String key = en.getKey();
            if (!proxySpecific(key)) {
                proxy.put(key, CopyHelper.deepCopy(en.getValue()));
            }
        }
    }

    /**
     * Things that we don't touch on a proxy.
     */
    protected boolean proxySpecific(String key) {
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
     * Flushes saved state to database.
     */
    protected void flush() throws DocumentException {
        for (String id : savedCreated) { // ordered
            DBSDocumentState docState = savedStates.get(id);
            repository.createState(docState.getState());
        }
        for (DBSDocumentState docState : savedStates.values()) {
            if (savedCreated.contains(docState.getId())) {
                continue; // already done
            }
            repository.updateState(docState.getState());
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
        FulltextParser fulltextParser = new FulltextParser();
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

            // TODO get from extension point, see also FulltextExtractorWork
            // XXX hardcoded config for now
            FulltextConfiguration config = new FulltextConfiguration();
            if (!config.isFulltextIndexable(documentType)) {
                continue;
            }
            docState.put(KEY_FULLTEXT_JOBID, docState.getId());
            fulltextParser.setDocument(docState, session);
            try {
                List<IndexAndText> indexesAndText = new LinkedList<IndexAndText>();
                for (String indexName : config.indexNames) {
                    // TODO paths from config
                    String text = fulltextParser.findFulltext(indexName);
                    indexesAndText.add(new IndexAndText(indexName, text));
                }
                if (!indexesAndText.isEmpty()) {
                    Work work = new FulltextUpdaterWork(repository.getName(),
                            id, true, false, indexesAndText);
                    works.add(work);
                }
            } finally {
                fulltextParser.setDocument(null, session);
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
        FulltextConfiguration config = new FulltextConfiguration();

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
            Work work = new FulltextExtractorWork(repository.getName(), id);
            works.add(work);
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
