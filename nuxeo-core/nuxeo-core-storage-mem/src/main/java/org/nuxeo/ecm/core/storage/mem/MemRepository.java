/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mem;

import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.storage.State.NOP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_PROXY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_TARGET_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ConcurrentUpdateDocumentException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.StateHelper;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator.OrderByComparator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;

/**
 * In-memory implementation of a {@link Repository}.
 * <p>
 * Internally, the repository is a map from id to document object.
 * <p>
 * A document object is a JSON-like document stored as a Map recursively
 * containing the data, see {@link DBSDocument} for the description of the
 * document.
 *
 * @since 5.9.4
 */
public class MemRepository extends DBSRepositoryBase {

    private static final Log log = LogFactory.getLog(MemRepository.class);

    // for debug
    private final AtomicLong temporaryIdCounter = new AtomicLong(0);

    /**
     * The content of the repository, a map of document id -> object.
     */
    protected Map<String, State> states;

    public MemRepository(String repositoryName) {
        super(repositoryName);
        initRepository();
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
                log.trace("read   " + id + ": " + state);
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
    public void createState(State state) throws DocumentException {
        String id = (String) state.get(KEY_ID);
        if (log.isTraceEnabled()) {
            log.trace("create " + id + ": " + state);
        }
        if (states.containsKey(id)) {
            throw new DocumentException("Already exists: " + id);
        }
        state = StateHelper.deepCopy(state, true); // thread-safe
        StateHelper.resetDeltas(state);
        states.put(id, state);
    }

    @Override
    public void updateState(String id, StateDiff diff) throws DocumentException {
        if (log.isTraceEnabled()) {
            log.trace("update " + id + ": " + diff);
        }
        State state = states.get(id);
        if (state == null) {
            throw new ConcurrentUpdateDocumentException("Missing: " + id);
        }
        applyDiff(state, diff);
    }

    @Override
    public void deleteStates(Set<String> ids) throws DocumentException {
        if (log.isTraceEnabled()) {
            log.trace("delete " + ids);
        }
        for (String id : ids) {
            if (states.remove(id) == null) {
                log.debug("Missing on remove: " + id);
            }
        }
    }

    @Override
    public State readChildState(String parentId, String name,
            Set<String> ignored) {
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
    public List<State> queryKeyValue(String key, String value,
            Set<String> ignored) {
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
        return list;
    }

    @Override
    public void queryKeyValueArray(String key, Object value, Set<String> ids,
            Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies) {
        STATE: for (State state : states.values()) {
            Object[] array = (Object[]) state.get(key);
            String id = (String) state.get(KEY_ID);
            if (array != null) {
                for (Object v : array) {
                    if (value.equals(v)) {
                        ids.add(id);
                        if (proxyTargets != null
                                && TRUE.equals(state.get(KEY_IS_PROXY))) {
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
    }

    @Override
    public boolean queryKeyValuePresence(String key, String value,
            Set<String> ignored) {
        for (State state : states.values()) {
            String id = (String) state.get(KEY_ID);
            if (ignored.contains(id)) {
                continue;
            }
            if (value.equals(state.get(key))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PartialList<State> queryAndFetch(Expression expression,
            DBSExpressionEvaluator evaluator, OrderByClause orderByClause,
            int limit, int offset, int countUpTo, boolean deepCopy,
            boolean fulltextScore) {
        List<State> maps = new ArrayList<>();
        for (State state : states.values()) {
            if (evaluator.matches(state)) {
                if (deepCopy) {
                    state = StateHelper.deepCopy(state);
                }
                maps.add(state);
            }
        }
        // ORDER BY
        if (orderByClause != null) {
            Collections.sort(maps, new OrderByComparator(orderByClause,
                    evaluator));
        }
        // LIMIT / OFFSET
        int totalSize = maps.size();
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
            int size = maps.size();
            maps.subList(0, offset > size ? size : offset).clear();
            size = maps.size();
            if (limit < size) {
                maps.subList(limit, size).clear();
            }
        }
        // TODO DISTINCT

        return new PartialList<>(maps, totalSize);
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
                    throw new UnsupportedOperationException(
                            "Cannot apply StateDiff on non-State: " + old);
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
                state.put(key, diffElem);
            }
        }
    }

    /**
     * Applies a {@link ListDiff} onto an array or {@link List}, and returns the
     * resulting value.
     * <p>
     * Uses thread-safe datastructures.
     */
    public static Serializable applyDiff(Serializable value, ListDiff listDiff) {
        // internally work on a list
        // TODO this is costly, use a separate code path for arrays
        if (listDiff.isArray && value != null) {
            if (!(value instanceof Object[])) {
                throw new UnsupportedOperationException(
                        "Cannot apply ListDiff on non-array: " + value);
            }
            value = new CopyOnWriteArrayList<>(Arrays.asList((Object[]) value));
        }
        if (value == null) {
            value = new CopyOnWriteArrayList<>();
        }
        if (!(value instanceof List)) {
            throw new UnsupportedOperationException(
                    "Cannot apply ListDiff on non-List: " + value);
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
            return list.isEmpty() ? null : list.toArray(new Object[0]);
        } else {
            return list.isEmpty() ? null : (Serializable) list;
        }
    }

}
