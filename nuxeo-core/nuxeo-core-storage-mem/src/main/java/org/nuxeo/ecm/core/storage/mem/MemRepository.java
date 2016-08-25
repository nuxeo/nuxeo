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
package org.nuxeo.ecm.core.storage.mem;

import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.core.storage.State.NOP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_BLOB_DATA;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_PROXY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_CREATED;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_OWNER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_TARGET_ID;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.resource.spi.ConnectionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.ScrollResultImpl;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.StateHelper;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.ecm.core.storage.dbs.DBSSession.OrderByComparator;
import org.nuxeo.runtime.api.Framework;

/**
 * In-memory implementation of a {@link Repository}.
 * <p>
 * Internally, the repository is a map from id to document object.
 * <p>
 * A document object is a JSON-like document stored as a Map recursively containing the data, see {@link DBSDocument}
 * for the description of the document.
 *
 * @since 5.9.4
 */
public class MemRepository extends DBSRepositoryBase {

    private static final Log log = LogFactory.getLog(MemRepository.class);

    protected static final String NOSCROLL_ID = "noscroll";

    // for debug
    private final AtomicLong temporaryIdCounter = new AtomicLong(0);

    /**
     * The content of the repository, a map of document id -> object.
     */
    protected Map<String, State> states;

    public MemRepository(ConnectionManager cm, MemRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor);
        initRepository();
    }

    @Override
    public List<IdType> getAllowedIdTypes() {
        return Collections.singletonList(IdType.varchar);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        states = null;
    }

    protected void initRepository() {
        states = new ConcurrentHashMap<>();
        initRoot();
    }

    @Override
    public String generateNewId() {
        if (DEBUG_UUIDS) {
            return "UUID_" + temporaryIdCounter.incrementAndGet();
        } else {
            return UUID.randomUUID().toString();
        }
    }

    @Override
    public State readState(String id) {
        State state = states.get(id);
        if (state != null) {
            if (log.isTraceEnabled()) {
                log.trace("Mem: READ  " + id + ": " + state);
            }
        }
        return state;
    }

    @Override
    public List<State> readStates(List<String> ids) {
        List<State> list = new ArrayList<>();
        for (String id : ids) {
            list.add(readState(id));
        }
        return list;
    }

    @Override
    public void createState(State state) {
        String id = (String) state.get(KEY_ID);
        if (log.isTraceEnabled()) {
            log.trace("Mem: CREATE " + id + ": " + state);
        }
        if (states.containsKey(id)) {
            throw new NuxeoException("Already exists: " + id);
        }
        state = StateHelper.deepCopy(state, true); // thread-safe
        StateHelper.resetDeltas(state);
        states.put(id, state);
    }

    @Override
    public void updateState(String id, StateDiff diff) {
        if (log.isTraceEnabled()) {
            log.trace("Mem: UPDATE " + id + ": " + diff);
        }
        State state = states.get(id);
        if (state == null) {
            throw new ConcurrentUpdateException("Missing: " + id);
        }
        applyDiff(state, diff);
    }

    @Override
    public void deleteStates(Set<String> ids) {
        if (log.isTraceEnabled()) {
            log.trace("Mem: REMOVE " + ids);
        }
        for (String id : ids) {
            if (states.remove(id) == null) {
                log.debug("Missing on remove: " + id);
            }
        }
    }

    @Override
    public State readChildState(String parentId, String name, Set<String> ignored) {
        // TODO optimize by maintaining a parent/child index
        for (State state : states.values()) {
            if (ignored.contains(state.get(KEY_ID))) {
                continue;
            }
            if (!parentId.equals(state.get(KEY_PARENT_ID))) {
                continue;
            }
            if (!name.equals(state.get(KEY_NAME))) {
                continue;
            }
            return state;
        }
        return null;
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        return readChildState(parentId, name, ignored) != null;
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        if (log.isTraceEnabled()) {
            log.trace("Mem: QUERY " + key + " = " + value);
        }
        List<State> list = new ArrayList<>();
        for (State state : states.values()) {
            String id = (String) state.get(KEY_ID);
            if (ignored.contains(id)) {
                continue;
            }
            if (!value.equals(state.get(key))) {
                continue;
            }
            list.add(state);
        }
        if (log.isTraceEnabled() && !list.isEmpty()) {
            log.trace("Mem:    -> " + list.size());
        }
        return list;
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        if (log.isTraceEnabled()) {
            log.trace("Mem: QUERY " + key1 + " = " + value1 + " AND " + key2 + " = " + value2);
        }
        List<State> list = new ArrayList<>();
        for (State state : states.values()) {
            String id = (String) state.get(KEY_ID);
            if (ignored.contains(id)) {
                continue;
            }
            if (!(value1.equals(state.get(key1)) && value2.equals(state.get(key2)))) {
                continue;
            }
            list.add(state);
        }
        if (log.isTraceEnabled() && !list.isEmpty()) {
            log.trace("Mem:    -> " + list.size());
        }
        return list;
    }

    @Override
    public void queryKeyValueArray(String key, Object value, Set<String> ids, Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies) {
        if (log.isTraceEnabled()) {
            log.trace("Mem: QUERY " + key + " = " + value);
        }
        STATE: for (State state : states.values()) {
            Object[] array = (Object[]) state.get(key);
            String id = (String) state.get(KEY_ID);
            if (array != null) {
                for (Object v : array) {
                    if (value.equals(v)) {
                        ids.add(id);
                        if (proxyTargets != null && TRUE.equals(state.get(KEY_IS_PROXY))) {
                            String targetId = (String) state.get(KEY_PROXY_TARGET_ID);
                            proxyTargets.put(id, targetId);
                        }
                        if (targetProxies != null) {
                            Object[] proxyIds = (Object[]) state.get(KEY_PROXY_IDS);
                            if (proxyIds != null) {
                                targetProxies.put(id, proxyIds);
                            }
                        }
                        continue STATE;
                    }
                }
            }
        }
        if (log.isTraceEnabled() && !ids.isEmpty()) {
            log.trace("Mem:    -> " + ids.size());
        }
    }

    @Override
    public boolean queryKeyValuePresence(String key, String value, Set<String> ignored) {
        if (log.isTraceEnabled()) {
            log.trace("Mem: QUERY " + key + " = " + value);
        }
        for (State state : states.values()) {
            String id = (String) state.get(KEY_ID);
            if (ignored.contains(id)) {
                continue;
            }
            if (value.equals(state.get(key))) {
                if (log.isTraceEnabled()) {
                    log.trace("Mem:    -> present");
                }
                return true;
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("Mem:    -> absent");
        }
        return false;
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, boolean distinctDocuments, int limit, int offset, int countUpTo) {
        if (log.isTraceEnabled()) {
            log.trace("Mem: QUERY " + evaluator + " OFFSET " + offset + " LIMIT " + limit);
        }
        evaluator.parse();
        List<Map<String, Serializable>> projections = new ArrayList<>();
        for (State state : states.values()) {
            List<Map<String, Serializable>> matches = evaluator.matches(state);
            if (!matches.isEmpty()) {
                if (distinctDocuments) {
                    projections.add(matches.get(0));
                } else {
                    projections.addAll(matches);
                }
            }
        }
        // ORDER BY
        // orderByClause may be null and different from evaluator.getOrderByClause() in case we want to post-filter
        if (orderByClause != null) {
            Collections.sort(projections, new OrderByComparator(orderByClause));
        }
        // LIMIT / OFFSET
        int totalSize = projections.size();
        if (countUpTo == -1) {
            // count full size
        } else if (countUpTo == 0) {
            // no count
            totalSize = -1; // not counted
        } else {
            // count only if less than countUpTo
            if (totalSize > countUpTo) {
                totalSize = -2; // truncated
            }
        }
        if (limit != 0) {
            int size = projections.size();
            projections.subList(0, offset > size ? size : offset).clear();
            size = projections.size();
            if (limit < size) {
                projections.subList(limit, size).clear();
            }
        }
        // TODO DISTINCT

        if (log.isTraceEnabled() && !projections.isEmpty()) {
            log.trace("Mem:    -> " + projections.size());
        }
        return new PartialList<>(projections, totalSize);
    }

    @Override
    public ScrollResult scroll(DBSExpressionEvaluator evaluator, int batchSize, int keepAliveSeconds) {
        if (log.isTraceEnabled()) {
            log.trace("Mem: QUERY " + evaluator);
        }
        evaluator.parse();
        List<String> ids = new ArrayList<>();
        for (State state : states.values()) {
            List<Map<String, Serializable>> matches = evaluator.matches(state);
            if (!matches.isEmpty()) {
                String id = matches.get(0).get(ECM_UUID).toString();
                ids.add(id);
            }
        }
        return new ScrollResultImpl(NOSCROLL_ID, ids);
    }

    @Override
    public ScrollResult scroll(String scrollId) {
        if (NOSCROLL_ID.equals(scrollId)) {
            // Id are already in memory, they are returned as a single batch
            return ScrollResultImpl.emptyResult();
        }
        throw new NuxeoException("Unknown or timed out scrollId");
    }

    /**
     * Applies a {@link StateDiff} in-place onto a base {@link State}.
     * <p>
     * Uses thread-safe datastructures.
     */
    public static void applyDiff(State state, StateDiff stateDiff) {
        for (Entry<String, Serializable> en : stateDiff.entrySet()) {
            String key = en.getKey();
            Serializable diffElem = en.getValue();
            if (diffElem instanceof StateDiff) {
                Serializable old = state.get(key);
                if (old == null) {
                    old = new State(true); // thread-safe
                    state.put(key, old);
                    // enter the next if
                }
                if (!(old instanceof State)) {
                    throw new UnsupportedOperationException("Cannot apply StateDiff on non-State: " + old);
                }
                applyDiff((State) old, (StateDiff) diffElem);
            } else if (diffElem instanceof ListDiff) {
                state.put(key, applyDiff(state.get(key), (ListDiff) diffElem));
            } else if (diffElem instanceof Delta) {
                Delta delta = (Delta) diffElem;
                Number oldValue = (Number) state.get(key);
                Number value;
                if (oldValue == null) {
                    value = delta.getFullValue();
                } else {
                    value = delta.add(oldValue);
                }
                state.put(key, value);
            } else {
                state.put(key, StateHelper.deepCopy(diffElem, true)); // thread-safe
            }
        }
    }

    /**
     * Applies a {@link ListDiff} onto an array or {@link List}, and returns the resulting value.
     * <p>
     * Uses thread-safe datastructures.
     */
    public static Serializable applyDiff(Serializable value, ListDiff listDiff) {
        // internally work on a list
        // TODO this is costly, use a separate code path for arrays
        Class<?> arrayComponentType = null;
        if (listDiff.isArray && value != null) {
            if (!(value instanceof Object[])) {
                throw new UnsupportedOperationException("Cannot apply ListDiff on non-array: " + value);
            }
            arrayComponentType = ((Object[]) value).getClass().getComponentType();
            value = new CopyOnWriteArrayList<>(Arrays.asList((Object[]) value));
        }
        if (value == null) {
            value = new CopyOnWriteArrayList<>();
        }
        if (!(value instanceof List)) {
            throw new UnsupportedOperationException("Cannot apply ListDiff on non-List: " + value);
        }
        @SuppressWarnings("unchecked")
        List<Serializable> list = (List<Serializable>) value;
        if (listDiff.diff != null) {
            int i = 0;
            for (Object diffElem : listDiff.diff) {
                if (i >= list.size()) {
                    // TODO log error applying diff to shorter list
                    break;
                }
                if (diffElem instanceof StateDiff) {
                    applyDiff((State) list.get(i), (StateDiff) diffElem);
                } else if (diffElem != NOP) {
                    list.set(i, StateHelper.deepCopy(diffElem, true)); // thread-safe
                }
                i++;
            }
        }
        if (listDiff.rpush != null) {
            // deepCopy of what we'll add
            List<Serializable> add = new ArrayList<>(listDiff.rpush.size());
            for (Object v : listDiff.rpush) {
                add.add(StateHelper.deepCopy(v, true)); // thread-safe
            }
            // update CopyOnWriteArrayList in one step
            list.addAll(add);
        }
        // convert back to array if needed
        if (listDiff.isArray) {
            return list.isEmpty() ? null : list.toArray((Object[]) Array.newInstance(arrayComponentType, list.size()));
        } else {
            return list.isEmpty() ? null : (Serializable) list;
        }
    }

    /* synchronized */
    @Override
    public synchronized Lock getLock(String id) {
        State state = states.get(id);
        if (state == null) {
            // document not found
            throw new DocumentNotFoundException(id);
        }
        String owner = (String) state.get(KEY_LOCK_OWNER);
        if (owner == null) {
            return null;
        }
        Calendar created = (Calendar) state.get(KEY_LOCK_CREATED);
        return new Lock(owner, created);
    }

    /* synchronized */
    @Override
    public synchronized Lock setLock(String id, Lock lock) {
        State state = states.get(id);
        if (state == null) {
            // document not found
            throw new DocumentNotFoundException(id);
        }
        String owner = (String) state.get(KEY_LOCK_OWNER);
        if (owner != null) {
            // return old lock
            Calendar created = (Calendar) state.get(KEY_LOCK_CREATED);
            return new Lock(owner, created);
        }
        state.put(KEY_LOCK_OWNER, lock.getOwner());
        state.put(KEY_LOCK_CREATED, lock.getCreated());
        return null;
    }

    /* synchronized */
    @Override
    public synchronized Lock removeLock(String id, String owner) {
        State state = states.get(id);
        if (state == null) {
            // document not found
            throw new DocumentNotFoundException(id);
        }
        String oldOwner = (String) state.get(KEY_LOCK_OWNER);
        if (oldOwner == null) {
            // no previous lock
            return null;
        }
        Calendar oldCreated = (Calendar) state.get(KEY_LOCK_CREATED);
        if (!LockManager.canLockBeRemoved(oldOwner, owner)) {
            // existing mismatched lock, flag failure
            return new Lock(oldOwner, oldCreated, true);
        }
        // remove lock
        state.put(KEY_LOCK_OWNER, null);
        state.put(KEY_LOCK_CREATED, null);
        // return old lock
        return new Lock(oldOwner, oldCreated);
    }

    @Override
    public void closeLockManager() {
    }

    @Override
    public void clearLockManagerCaches() {
    }

    protected List<List<String>> binaryPaths;

    @Override
    protected void initBlobsPaths() {
        MemBlobFinder finder = new MemBlobFinder();
        finder.visit();
        binaryPaths = finder.binaryPaths;
    }

    protected static class MemBlobFinder extends BlobFinder {
        protected List<List<String>> binaryPaths = new ArrayList<>();

        @Override
        protected void recordBlobPath() {
            binaryPaths.add(new ArrayList<>(path));
        }
    }

    @Override
    public void markReferencedBinaries() {
        BlobManager blobManager = Framework.getService(BlobManager.class);
        for (State state : states.values()) {
            for (List<String> path : binaryPaths) {
                markReferencedBinaries(state, path, 0, blobManager);
            }
        }
    }

    protected void markReferencedBinaries(State state, List<String> path, int start, BlobManager blobManager) {
        for (int i = start; i < path.size(); i++) {
            String name = path.get(i);
            Serializable value = state.get(name);
            if (value instanceof State) {
                state = (State) value;
            } else {
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) value;
                    for (Object v : list) {
                        if (v instanceof State) {
                            markReferencedBinaries((State) v, path, i + 1, blobManager);
                        } else {
                            markReferencedBinary(v, blobManager);
                        }
                    }
                }
                state = null;
                break;
            }
        }
        if (state != null) {
            Serializable data = state.get(KEY_BLOB_DATA);
            markReferencedBinary(data, blobManager);
        }
    }

    protected void markReferencedBinary(Object value, BlobManager blobManager) {
        if (!(value instanceof String)) {
            return;
        }
        String key = (String) value;
        blobManager.markReferencedBinary(key, repositoryName);
    }

}
