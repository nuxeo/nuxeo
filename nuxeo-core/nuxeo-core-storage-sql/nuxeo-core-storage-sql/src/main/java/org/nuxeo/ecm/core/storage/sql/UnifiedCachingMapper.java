/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.query.QueryFilter;

/**
 * A {@link Mapper} that uses a {@link UnifiedCachingRowMapper} for row-related operation, and delegates to the
 * {@link Mapper} for others.
 */
public class UnifiedCachingMapper extends UnifiedCachingRowMapper implements CachingMapper {

    /**
     * The {@link Mapper} to which operations are delegated.
     */
    public Mapper mapper;

    @Override
    public void initialize(String repositoryName, Model model, Mapper mapper,
            VCSInvalidationsPropagator invalidationsPropagator, Map<String, String> properties) {
        super.initialize(repositoryName, model, mapper, invalidationsPropagator, properties);
        this.mapper = mapper;
    }

    @Override
    public ScrollResult scroll(String query, int batchSize, int keepAliveSeconds) {
        return mapper.scroll(query, batchSize, keepAliveSeconds);
    }

    @Override
    public ScrollResult scroll(String query, QueryFilter queryFilter, int batchSize, int keepAliveSeconds) {
        return mapper.scroll(query, queryFilter, batchSize, keepAliveSeconds);
    }

    @Override
    public ScrollResult scroll(String scrollId) {
        return mapper.scroll(scrollId);
    }

    @Override
    public Identification getIdentification() {
        return mapper.getIdentification();
    }

    @Override
    public void close() {
        super.close();
        mapper.close();
    }

    @Override
    public int getTableSize(String tableName) {
        return mapper.getTableSize(tableName);
    }

    @Override
    public void createDatabase(String ddlMode) {
        mapper.createDatabase(ddlMode);
    }

    @Override
    public Serializable getRootId(String repositoryId) {
        return mapper.getRootId(repositoryId);
    }

    @Override
    public void setRootId(Serializable repositoryId, Serializable id) {
        mapper.setRootId(repositoryId, id);
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter,
            boolean countTotal) {
        return mapper.query(query, queryType, queryFilter, countTotal);
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter, long countUpTo) {
        return mapper.query(query, queryType, queryFilter, countUpTo);
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            boolean distinctDocuments, Object... params) {
        return mapper.queryAndFetch(query, queryType, queryFilter, distinctDocuments, params);
    }

    @Override
    public PartialList<Map<String, Serializable>> queryProjection(String query, String queryType,
            QueryFilter queryFilter, boolean distinctDocuments, long countUpTo, Object... params) {
        return mapper.queryProjection(query, queryType, queryFilter, distinctDocuments, countUpTo, params);
    }

    @Override
    public Set<Serializable> getAncestorsIds(Collection<Serializable> ids) {
        return mapper.getAncestorsIds(ids);
    }

    @Override
    public void updateReadAcls() {
        mapper.updateReadAcls();
    }

    @Override
    public void rebuildReadAcls() {
        mapper.rebuildReadAcls();
    }

    @Override
    public int getClusterNodeIdType() {
        return mapper.getClusterNodeIdType();
    }

    @Override
    public void createClusterNode(Serializable nodeId) {
        mapper.createClusterNode(nodeId);
    }

    @Override
    public void removeClusterNode(Serializable nodeId) {
        mapper.removeClusterNode(nodeId);
    }

    @Override
    public void insertClusterInvalidations(Serializable nodeId, VCSInvalidations invalidations) {
        mapper.insertClusterInvalidations(nodeId, invalidations);
    }

    @Override
    public VCSInvalidations getClusterInvalidations(Serializable nodeId) {
        return mapper.getClusterInvalidations(nodeId);
    }

    @Override
    public Lock getLock(Serializable id) {
        return mapper.getLock(id);
    }

    @Override
    public Lock setLock(Serializable id, Lock lock) {
        return mapper.setLock(id, lock);
    }

    @Override
    public Lock removeLock(Serializable id, String owner, boolean force) {
        return mapper.removeLock(id, owner, force);
    }

    @Override
    public void markReferencedBinaries() {
        mapper.markReferencedBinaries();
    }

    @Override
    public int cleanupDeletedRows(int max, Calendar beforeTime) {
        return mapper.cleanupDeletedRows(max, beforeTime);
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        mapper.start(xid, flags);
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        mapper.end(xid, flags);

    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return mapper.prepare(xid);
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        mapper.commit(xid, onePhase);
    }

    // rollback interacts with caches so is in RowMapper

    @Override
    public void forget(Xid xid) throws XAException {
        mapper.forget(xid);
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return mapper.recover(flag);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return mapper.setTransactionTimeout(seconds);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return mapper.getTransactionTimeout();
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        return mapper.isSameRM(xares);
    }

    @Override
    public boolean isConnected() {
        return mapper.isConnected();
    }

    @Override
    public void connect(boolean noSharing) {
        mapper.connect(noSharing);
    }

    @Override
    public void disconnect() {
        mapper.disconnect();
    }
}
