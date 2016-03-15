/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.storage.marklogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.ConnectionManager;

import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;

/**
 * MarkLogic implementation of a {@link Repository}.
 *
 * @since 8.3
 */
public class MarkLogicRepository extends DBSRepositoryBase {

    public MarkLogicRepository(ConnectionManager cm, MarkLogicRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor.getFulltextDescriptor());
    }

    @Override
    protected void initBlobsPaths() {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public String generateNewId() {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public State readState(String id) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public List<State> readStates(List<String> ids) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void createState(State state) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void updateState(String id, StateDiff diff) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void deleteStates(Set<String> ids) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public State readChildState(String parentId, String name, Set<String> ignored) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void queryKeyValueArray(String key, Object value, Set<String> ids, Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public boolean queryKeyValuePresence(String key, String value, Set<String> ignored) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, boolean distinctDocuments, int limit, int offset, int countUpTo) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public Lock getLock(String id) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public Lock removeLock(String id, String owner) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void closeLockManager() {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void clearLockManagerCaches() {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void markReferencedBinaries() {
        throw new IllegalStateException("Not implemented yet");
    }

}
