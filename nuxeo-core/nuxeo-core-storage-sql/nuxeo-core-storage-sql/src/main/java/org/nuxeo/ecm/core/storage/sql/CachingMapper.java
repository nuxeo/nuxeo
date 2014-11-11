/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    public CachingMapper(Mapper mapper) {
        super(mapper);
        this.mapper = mapper;
    }

    public String getMapperId() throws StorageException {
        return mapper.getMapperId();
    }

    public void close() throws StorageException {
        mapper.close();
    }

    public int getTableSize(String tableName) throws StorageException {
        return mapper.getTableSize(tableName);
    }

    public void createDatabase() throws StorageException {
        mapper.createDatabase();
    }

    public Serializable getRootId(Serializable repositoryId)
            throws StorageException {
        return mapper.getRootId(repositoryId);
    }

    public void setRootId(Serializable repositoryId, Serializable id)
            throws StorageException {
        mapper.setRootId(repositoryId, id);
    }

    public Serializable getVersionIdByLabel(Serializable versionableId,
            String label) throws StorageException {
        return mapper.getVersionIdByLabel(versionableId, label);
    }

    public Serializable getLastVersionId(Serializable versionableId)
            throws StorageException {
        return mapper.getLastVersionId(versionableId);
    }

    public PartialList<Serializable> query(String query,
            QueryFilter queryFilter, boolean countTotal)
            throws StorageException {
        return mapper.query(query, queryFilter, countTotal);
    }

    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object... params) throws StorageException {
        return mapper.queryAndFetch(query, queryType, queryFilter, params);
    }

    public void updateReadAcls() throws StorageException {
        mapper.updateReadAcls();
    }

    public void rebuildReadAcls() throws StorageException {
        mapper.rebuildReadAcls();
    }

    public void createClusterNode() throws StorageException {
        mapper.createClusterNode();
    }

    public void removeClusterNode() throws StorageException {
        mapper.removeClusterNode();
    }

    public void insertClusterInvalidations(Invalidations invalidations)
            throws StorageException {
        mapper.insertClusterInvalidations(invalidations);
    }

    public Invalidations getClusterInvalidations() throws StorageException {
        return mapper.getClusterInvalidations();
    }

    public void start(Xid xid, int flags) throws XAException {
        mapper.start(xid, flags);
    }

    public void end(Xid xid, int flags) throws XAException {
        mapper.end(xid, flags);

    }

    public int prepare(Xid xid) throws XAException {
        return mapper.prepare(xid);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        mapper.commit(xid, onePhase);
    }

    // rollback interacts with caches so is in RowMapper

    public void forget(Xid xid) throws XAException {
        mapper.forget(xid);
    }

    public Xid[] recover(int flag) throws XAException {
        return mapper.recover(flag);
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        return mapper.setTransactionTimeout(seconds);
    }

    public int getTransactionTimeout() throws XAException {
        return mapper.getTransactionTimeout();
    }

    public boolean isSameRM(XAResource xares) throws XAException {
        return mapper.isSameRM(xares);
    }

}
