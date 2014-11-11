/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A {@link Mapper} that uses a {@link CachingRowMapper} for row-related
 * operation, and delegates to the {@link Mapper} for others.
 */
public class CachingMapper extends CachingRowMapper implements Mapper {

    /**
     * The {@link Mapper} to which operations are delegated.
     */
    private final Mapper mapper;

    public CachingMapper(Mapper mapper,
            InvalidationsPropagator cachePropagator,
            InvalidationsPropagator eventPropagator,
            InvalidationsQueue repositoryEventQueue) {
        super(mapper, cachePropagator, eventPropagator, repositoryEventQueue);
        this.mapper = mapper;
    }

    @Override
    public Identification getIdentification() throws StorageException {
        return mapper.getIdentification();
    }

    @Override
    public void close() throws StorageException {
        super.close();
        mapper.close();
    }

    @Override
    public int getTableSize(String tableName) throws StorageException {
        return mapper.getTableSize(tableName);
    }

    @Override
    public void createDatabase() throws StorageException {
        mapper.createDatabase();
    }

    @Override
    public Serializable getRootId(Serializable repositoryId)
            throws StorageException {
        return mapper.getRootId(repositoryId);
    }

    @Override
    public void setRootId(Serializable repositoryId, Serializable id)
            throws StorageException {
        mapper.setRootId(repositoryId, id);
    }

    @Override
    public Serializable getVersionIdByLabel(Serializable versionSeriesId,
            String label) throws StorageException {
        return mapper.getVersionIdByLabel(versionSeriesId, label);
    }

    @Override
    public Serializable getLastVersionId(Serializable versionSeriesId)
            throws StorageException {
        return mapper.getLastVersionId(versionSeriesId);
    }

    @Override
    public PartialList<Serializable> query(String query,
            QueryFilter queryFilter, boolean countTotal)
            throws StorageException {
        return mapper.query(query, queryFilter, countTotal);
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object... params) throws StorageException {
        return mapper.queryAndFetch(query, queryType, queryFilter, params);
    }

    @Override
    public void updateReadAcls() throws StorageException {
        mapper.updateReadAcls();
    }

    @Override
    public void rebuildReadAcls() throws StorageException {
        mapper.rebuildReadAcls();
    }

    @Override
    public void createClusterNode() throws StorageException {
        mapper.createClusterNode();
    }

    @Override
    public void removeClusterNode() throws StorageException {
        mapper.removeClusterNode();
    }

    @Override
    public void insertClusterInvalidations(Invalidations invalidations)
            throws StorageException {
        mapper.insertClusterInvalidations(invalidations);
    }

    @Override
    public Invalidations getClusterInvalidations() throws StorageException {
        return mapper.getClusterInvalidations();
    }

    @Override
    public Lock getLock(Serializable id) throws StorageException {
        return mapper.getLock(id);
    }

    @Override
    public Lock setLock(Serializable id, Lock lock) throws StorageException {
        return mapper.setLock(id, lock);
    }

    @Override
    public Lock removeLock(Serializable id, String owner, boolean force)
            throws StorageException {
        return mapper.removeLock(id, owner, force);
    }

    @Override
    public void markReferencedBinaries(BinaryGarbageCollector gc) throws StorageException {
        mapper.markReferencedBinaries(gc);
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

}
