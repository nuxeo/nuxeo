/*
 * (C) Copyright 2016-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.dbs;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSTransactionState.ChangeTokenUpdater;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

/**
 * The DBS Cache layer used to cache some method call of real repository
 *
 * @since 11.1 (introduced in 8.10)
 */
public class DBSCachingConnection implements DBSConnection {

    private static final Log log = LogFactory.getLog(DBSCachingConnection.class);

    protected final DBSConnection connection;

    protected DBSCachingRepository crepository;

    public DBSCachingConnection(DBSConnection connection, DBSCachingRepository crepository) {
        this.connection = connection;
        this.crepository = crepository;
    }

    @Override
    public void close() {
        connection.close();
    }

    @Override
    public void begin() {
        connection.begin();
        processReceivedInvalidations();
    }

    @Override
    public void commit() {
        connection.commit();
        sendInvalidationsToOther();
        processReceivedInvalidations();
    }

    @Override
    public void rollback() {
        connection.rollback();
    }

    @Override
    public State readState(String id) {
        State state = cache.getIfPresent(id);
        if (state == null) {
            state = connection.readState(id);
            if (state != null) {
                putInCache(state);
            }
        }
        return state;
    }

    @Override
    public State readPartialState(String id, Collection<String> keys) {
        // bypass caches, as the goal of this method is to not trash caches for one-shot reads
        return connection.readPartialState(id, keys);
    }

    @Override
    public List<State> readStates(List<String> ids) {
        ImmutableMap<String, State> statesMap = cache.getAllPresent(ids);
        List<String> idsToRetrieve = new ArrayList<>(ids);
        idsToRetrieve.removeAll(statesMap.keySet());
        // Read missing states from repository
        List<State> states = connection.readStates(idsToRetrieve);
        // Cache them
        states.forEach(this::putInCache);
        // Add previous cached one
        states.addAll(statesMap.values());
        // Sort them
        states.sort(Comparator.comparing(state -> state.get(KEY_ID).toString(), Ordering.explicit(ids)));
        return states;
    }

    @Override
    public void createState(State state) {
        connection.createState(state);
        // don't cache new state, it is inefficient on mass import
    }

    @Override
    public void createStates(List<State> states) {
        connection.createStates(states);
        // don't cache new states, it is inefficient on mass import
    }

    @Override
    public void updateState(String id, StateDiff diff, ChangeTokenUpdater changeTokenUpdater) {
        connection.updateState(id, diff, changeTokenUpdater);
        invalidate(id);
    }

    @Override
    public void deleteStates(Set<String> ids) {
        connection.deleteStates(ids);
        invalidateAll(ids);
    }

    @Override
    public State readChildState(String parentId, String name, Set<String> ignored) {
        processReceivedInvalidations();

        String childCacheKey = computeChildCacheKey(parentId, name);
        String stateId = childCache.getIfPresent(childCacheKey);
        if (stateId != null) {
            State state = cache.getIfPresent(stateId);
            if (state != null) {
                // As we don't have invalidation for childCache we need to check if retrieved state is the right one
                // and not a previous document which was moved or renamed
                if (parentId.equals(state.get(KEY_PARENT_ID)) && name.equals(state.get(KEY_NAME))) {
                    return state;
                } else {
                    // We can invalidate the entry in cache as the document seemed to be moved or renamed
                    childCache.invalidate(childCacheKey);
                }
            }
        }
        State state = connection.readChildState(parentId, name, ignored);
        putInCache(state);
        return state;
    }

    protected void putInCache(State state) {
        if (state != null) {
            String stateId = state.get(KEY_ID).toString();
            cache.put(stateId, state);
            Object stateParentId = state.get(KEY_PARENT_ID);
            if (stateParentId != null) {
                childCache.put(computeChildCacheKey(stateParentId.toString(), state.get(KEY_NAME).toString()), stateId);
            }
        }
    }

    protected String computeChildCacheKey(String parentId, String name) {
        return parentId + '_' + name;
    }

    @Override
    public String getRootId() {
        return connection.getRootId();
    }

    @Override
    public String generateNewId() {
        return connection.generateNewId();
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        return connection.hasChild(parentId, name, ignored);
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        return connection.queryKeyValue(key, value, ignored);
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        return connection.queryKeyValue(key1, value1, key2, value2, ignored);
    }

    @Override
    public Stream<State> getDescendants(String id, Set<String> keys) {
        return connection.getDescendants(id, keys);
    }

    @Override
    public Stream<State> getDescendants(String id, Set<String> keys, int limit) {
        return connection.getDescendants(id, keys, limit);
    }

    @Override
    public boolean queryKeyValuePresence(String key, String value, Set<String> ignored) {
        return connection.queryKeyValuePresence(key, value, ignored);
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, boolean distinctDocuments, int limit, int offset, int countUpTo) {
        return connection.queryAndFetch(evaluator, orderByClause, distinctDocuments, limit, offset, countUpTo);
    }

    @Override
    public ScrollResult<String> scroll(DBSExpressionEvaluator evaluator, int batchSize, int keepAliveSeconds) {
        return connection.scroll(evaluator, batchSize, keepAliveSeconds);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return connection.scroll(scrollId);
    }

    @Override
    public Lock getLock(String id) {
        return connection.getLock(id);
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        return connection.setLock(id, lock);
    }

    @Override
    public Lock removeLock(String id, String owner) {
        return connection.removeLock(id, owner);
    }

}
