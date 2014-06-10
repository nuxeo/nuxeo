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
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.storage.CopyHelper;
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

    public DBSTransactionState(DBSRepository repository) {
        this.repository = repository;
        SecurityService securityService = Framework.getLocalService(SecurityService.class);
        browsePermissions = new HashSet<>(
                Arrays.asList(securityService.getPermissionsToCheck(BROWSE)));
    }

    protected DBSDocumentState makeTransient(DBSDocumentState state) {
        String id = state.getId();
        if (transientStates.containsKey(id)) {
            throw new IllegalStateException("Already transient: " + id);
        }
        state = new DBSDocumentState(state); // copy
        transientStates.put(id, state);
        return state;
    }

    protected DBSDocumentState returnTransient(Map<String, Serializable> map) {
        String id = (String) map.get(KEY_ID);
        if (transientStates.containsKey(id)) {
            throw new IllegalStateException("Already transient: " + id);
        }
        DBSDocumentState state = new DBSDocumentState(map); // copy
        transientStates.put(id, state);
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

    /**
     * Returns a state and marks it as transient, because it's about to be
     * modified or returned to user code (where it may be modified).
     */
    public DBSDocumentState getStateForUpdate(String id) {
        // check transient state
        DBSDocumentState state = transientStates.get(id);
        if (state != null) {
            return state;
        }
        // check saved state
        if (savedDeleted.contains(id)) {
            return null;
        }
        state = savedStates.get(id);
        if (state != null) {
            return makeTransient(state);
        }
        // fetch from repository
        Map<String, Serializable> map = repository.readState(id);
        if (map != null) {
            return returnTransient(map);
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
        DBSDocumentState state = savedStates.get(id);
        if (state != null) {
            return state;
        }
        // fetch from repository
        Map<String, Serializable> map = repository.readState(id);
        if (map != null) {
            // keep as saved state, as we'll want to flush it after updates
            state = new DBSDocumentState(map);
            savedStates.put(id, state);
            return state;
        }
        return null;
    }

    /**
     * Returns a state which won't be modified.
     */
    // TODO in some cases it's good to have this kept in memory instead of
    // rereading from database every time
    // XXX getStateForReadOneShot
    public Map<String, Serializable> getStateForRead(String id) {
        // check transient state
        DBSDocumentState state = transientStates.get(id);
        if (state != null) {
            return state.getMap();
        }
        // check saved state
        if (savedDeleted.contains(id)) {
            return null;
        }
        state = savedStates.get(id);
        if (state != null) {
            return state.getMap();
        }
        // fetch from repository
        Map<String, Serializable> map = repository.readState(id);
        if (map != null) {
            return map;
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
            DBSDocumentState state = transientStates.get(id);
            if (state != null) {
                continue;
            }
            // check saved state
            if (savedDeleted.contains(id)) {
                continue;
            }
            state = savedStates.get(id);
            if (state != null) {
                makeTransient(state);
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

    // XXX TODO for update or for read?
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
            return makeTransient(state);
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
        Set<String> seen = new HashSet<String>();
        for (DBSDocumentState state : transientStates.values()) {
            seen.add(state.getId());
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            children.add(state);
        }
        for (DBSDocumentState state : savedStates.values()) {
            if (!seen.add(state.getId())) {
                // already seen
                continue;
            }
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            state = makeTransient(state);
            children.add(state);
        }
        List<Map<String, Serializable>> maps = repository.queryKeyValue(
                KEY_PARENT_ID, parentId, seen);
        for (Map<String, Serializable> map : maps) {
            DBSDocumentState state = returnTransient(map);
            children.add(state);
        }
        return children;
    }

    public List<String> getChildrenIds(String parentId) {
        if (savedDeleted.contains(parentId)) {
            return Collections.emptyList();
        }
        List<String> children = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        for (DBSDocumentState state : transientStates.values()) {
            String id = state.getId();
            seen.add(id);
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            children.add(id);
        }
        for (DBSDocumentState state : savedStates.values()) {
            String id = state.getId();
            if (!seen.add(id)) {
                // already seen
                continue;
            }
            if (!parentId.equals(state.getParentId())) {
                continue;
            }
            children.add(id);
        }
        List<Map<String, Serializable>> maps = repository.queryKeyValue(
                KEY_PARENT_ID, parentId, seen);
        for (Map<String, Serializable> map : maps) {
            children.add((String) map.get(KEY_ID));
        }
        return new ArrayList<String>(children);
    }

    public boolean hasChildren(String parentId) {
        if (savedDeleted.contains(parentId)) {
            return false;
        }
        Set<String> seen = new HashSet<String>();
        for (DBSDocumentState state : transientStates.values()) {
            seen.add(state.getId());
            if (!parentId.equals(state.getParentId())) {
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
            return true;
        }
        return repository.queryKeyValuePresence(KEY_PARENT_ID, parentId, seen);
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
        state.put(KEY_ANCESTOR_IDS, getAncestorIds(parentId));
        state.put(KEY_NAME, name);
        state.put(KEY_POS, pos);
        state.put(KEY_PRIMARY_TYPE, typeName);
        return state;
    }

    /** Gets ancestors including id itself. */
    protected Object[] getAncestorIds(String id) {
        if (id == null) {
            return null;
        }
        Map<String, Serializable> state = getStateForRead(id);
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
            DBSDocumentState state = getStateForUpdate(cid);
            Object[] ancestors = (Object[]) state.get(KEY_ANCESTOR_IDS);
            Object[] newAncestors;
            if (ancestors == null) {
                newAncestors = ancestorIds.clone();
            } else {
                newAncestors = new Object[ancestors.length - ndel + nadd];
                System.arraycopy(ancestorIds, 0, newAncestors, 0, nadd);
                System.arraycopy(ancestors, ndel, newAncestors, nadd,
                        ancestors.length - ndel);
            }
            state.put(KEY_ANCESTOR_IDS, newAncestors);
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
            DBSDocumentState state = getStateForUpdate(cid);
            state.put(KEY_READ_ACL, getReadACL(state));
        }
    }

    /**
     * Gets the Read ACL (flat list of users having browse permission, including
     * inheritance) on a document.
     */
    protected String[] getReadACL(DBSDocumentState state) {
        Set<String> racls = new HashSet<>();
        Map<String, Serializable> map = state.getMap();
        LOOP: do {
            @SuppressWarnings("unchecked")
            List<Serializable> aclList = (List<Serializable>) map.get(KEY_ACP);
            if (aclList != null) {
                for (Serializable aclSer : aclList) {
                    @SuppressWarnings("unchecked")
                    Map<String, Serializable> aclMap = (Map<String, Serializable>) aclSer;
                    @SuppressWarnings("unchecked")
                    List<Serializable> aceList = (List<Serializable>) aclMap.get(KEY_ACL);
                    for (Serializable aceSer : aceList) {
                        @SuppressWarnings("unchecked")
                        Map<String, Serializable> aceMap = (Map<String, Serializable>) aceSer;
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
            if (TRUE.equals(map.get(KEY_IS_VERSION))) {
                String versionSeriesId = (String) map.get(KEY_VERSION_SERIES_ID);
                map = versionSeriesId == null ? null
                        : getStateForRead(versionSeriesId);
            } else {
                String parentId = (String) map.get(KEY_PARENT_ID);
                map = parentId == null ? null : getStateForRead(parentId);
            }
        } while (map != null);

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
        Set<String> seen = new HashSet<String>();
        DOC: //
        for (DBSDocumentState state : savedStates.values()) {
            String oid = state.getId();
            seen.add(oid);
            Object[] ancestors = (Object[]) state.get(KEY_ANCESTOR_IDS);
            if (ancestors != null) {
                for (Object aid : ancestors) {
                    if (id.equals(aid)) {
                        ids.add(oid);
                        if (proxyTargets != null
                                && TRUE.equals(state.get(KEY_IS_PROXY))) {
                            String targetId = (String) state.get(KEY_PROXY_TARGET_ID);
                            proxyTargets.put(oid, targetId);
                        }
                        if (targetProxies != null) {
                            Object[] proxyIds = (Object[]) state.get(KEY_PROXY_IDS);
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
        List<DBSDocumentState> states = new LinkedList<DBSDocumentState>();
        Set<String> seen = new HashSet<String>();
        for (DBSDocumentState state : transientStates.values()) {
            seen.add(state.getId());
            if (!value.equals(state.get(key))) {
                continue;
            }
            states.add(state);
        }
        for (DBSDocumentState state : savedStates.values()) {
            if (!seen.add(state.getId())) {
                // already seen
                continue;
            }
            if (!value.equals(state.get(key))) {
                continue;
            }
            state = makeTransient(state);
            states.add(state);
        }
        List<Map<String, Serializable>> maps = repository.queryKeyValue(key,
                value, seen);
        for (Map<String, Serializable> map : maps) {
            DBSDocumentState state = returnTransient(map);
            states.add(state);
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
        updateProxies();
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
     * Checks if the changed documents are proxy targets, and updates the
     * proxies if that's the case.
     */
    protected void updateProxies() {
        for (String id : transientCreated) { // ordered
            DBSDocumentState state = transientStates.get(id);
            Object[] proxyIds = (Object[]) state.get(KEY_PROXY_IDS);
            if (proxyIds != null) {
                for (Object proxyId : proxyIds) {
                    updateProxy(state, (String) proxyId);
                }
            }
        }
        // copy as we may modify proxies
        for (String id : transientStates.keySet().toArray(new String[0])) {
            DBSDocumentState state = transientStates.get(id);
            if (transientCreated.contains(id)) {
                continue; // already done
            }
            if (state.isDirty()) {
                Object[] proxyIds = (Object[]) state.get(KEY_PROXY_IDS);
                if (proxyIds != null) {
                    for (Object proxyId : proxyIds) {
                        updateProxy(state, (String) proxyId);
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
        for (String key : proxy.getMap().keySet().toArray(new String[0])) {
            if (!proxySpecific(key)) {
                proxy.put(key, null);
            }
        }
        // copy from target
        for (Entry<String, Serializable> en : target.getMap().entrySet()) {
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
