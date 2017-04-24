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
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.transaction.xa.XAResource;

import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.query.QueryFilter;

/**
 * A {@link Mapper} maps {@link Row}s to and from the database.
 */
public interface Mapper extends RowMapper, XAResource {

    /**
     * Identifiers assigned by a server to identify a client mapper and its repository.
     */
    public static final class Identification implements Serializable {
        private static final long serialVersionUID = 1L;

        public final String repositoryId;

        public final String mapperId;

        public Identification(String repositoryId, String mapperId) {
            this.repositoryId = repositoryId;
            this.mapperId = mapperId;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + repositoryId + ',' + mapperId + ')';
        }
    }

    /**
     * Returns the repository id and mapper id assigned.
     * <p>
     * This is used in remote stateless mode to be able to identify to which mapper an incoming connection is targeted,
     * and from which repository instance.
     *
     * @return the repository and mapper identification
     */
    Identification getIdentification();

    // used for reflection
    String GET_IDENTIFICATION = "getIdentification";

    void close();

    // used for reflection
    String CLOSE = "close";

    // TODO
    int getTableSize(String tableName);

    /**
     * Creates the necessary structures in the database.
     *
     * @param ddlMode the DDL execution mode
     */
    void createDatabase(String ddlMode);

    /*
     * ========== Methods returning non-Rows ==========
     */

    /*
     * ----- Root -----
     */

    /**
     * Gets the root id for a given repository, if registered.
     *
     * @param repositoryId the repository id
     * @return the root id, or null if not found
     */
    Serializable getRootId(String repositoryId);

    /**
     * Records the newly generated root id for a given repository.
     *
     * @param repositoryId the repository id, usually 0
     * @param id the root id
     */
    void setRootId(Serializable repositoryId, Serializable id);

    /*
     * ----- Query -----
     */

    /**
     * Makes a NXQL query to the database.
     *
     * @param query the query
     * @param query the query type
     * @param queryFilter the query filter
     * @param countTotal if {@code true}, count the total size without limit/offset
     * @return the list of matching document ids
     */
    PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter, boolean countTotal);

    /**
     * Makes a NXQL query to the database.
     *
     * @param query the query
     * @param query the query type
     * @param queryFilter the query filter
     * @param countUpTo if {@code -1}, count the total size without offset/limit.<br>
     *            If {@code 0}, don't count the total size.<br>
     *            If {@code n}, count the total number if there are less than n documents otherwise set the size to
     *            {@code -1}.
     * @return the list of matching document ids
     * @Since 5.6
     */
    PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter, long countUpTo);

    /**
     * Makes a query to the database and returns an iterable (which must be closed when done).
     *
     * @param query the query
     * @param queryType the query type
     * @param queryFilter the query filter
     * @param distinctDocuments if {@code true} then a maximum of one row per document will be returned
     * @param params optional query-type-dependent parameters
     * @return an iterable, which <b>must</b> be closed when done
     */
    // queryFilter used for principals and permissions
    IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            boolean distinctDocuments, Object... params);

    /**
     * Makes a query to the database.
     *
     * @param query the query
     * @param queryType the query type
     * @param queryFilter the query filter
     * @param distinctDocuments if {@code true} then a maximum of one row per document will be returned
     * @param countUpTo if {@code -1}, also count the total size without offset/limit.<br>
     *            If {@code 0}, don't count the total size.<br>
     *            If {@code n}, count the total number if there are less than n documents otherwise set the size to
     *            {@code -1}.
     * @param params optional query-type-dependent parameters
     * @return a projection
     * @since 7.10-HF-25, 8.10-HF06, 9.2
     */
    PartialList<Map<String,Serializable>> queryProjection(String query, String queryType, QueryFilter queryFilter, boolean distinctDocuments,
            long countUpTo, Object... params);

    /**
     * Gets the ids for all the ancestors of the given row ids.
     *
     * @param ids the ids
     * @return the set of ancestor ids
     */
    Set<Serializable> getAncestorsIds(Collection<Serializable> ids);

    /*
     * ----- ACLs -----
     */

    void updateReadAcls();

    void rebuildReadAcls();

    /*
     * ----- Clustering -----
     */

    int getClusterNodeIdType();

    /**
     * Informs the cluster that this node exists.
     */
    void createClusterNode(Serializable nodeId);

    /**
     * Removes this node from the cluster.
     */
    void removeClusterNode(Serializable nodeId);

    /**
     * Inserts the invalidation rows for the other cluster nodes.
     */
    void insertClusterInvalidations(Serializable nodeId, Invalidations invalidations);

    /**
     * Gets the invalidations from other cluster nodes.
     */
    Invalidations getClusterInvalidations(Serializable nodeId);

    /*
     * ----- Locking -----
     */

    /**
     * Gets the lock state of a document.
     * <p>
     * If the document does not exist, {@code null} is returned.
     *
     * @param id the document id
     * @return the existing lock, or {@code null} when there is no lock
     */
    Lock getLock(Serializable id);

    /**
     * Sets a lock on a document.
     * <p>
     * If the document is already locked, returns its existing lock status (there is no re-locking, {@link #removeLock}
     * must be called first).
     *
     * @param id the document id
     * @param lock the lock object to set
     * @return {@code null} if locking succeeded, or the existing lock if locking failed, or a
     */
    Lock setLock(Serializable id, Lock lock);

    /**
     * Removes a lock from a document.
     * <p>
     * The previous lock is returned.
     * <p>
     * If {@code owner} is {@code null} then the lock is unconditionally removed.
     * <p>
     * If {@code owner} is not {@code null}, it must match the existing lock owner for the lock to be removed. If it
     * doesn't match, the returned lock will return {@code true} for {@link Lock#getFailed}.
     *
     * @param id the document id
     * @param the owner to check, or {@code null} for no check
     * @param force {@code true} to just do the remove and not return the previous lock
     * @return the previous lock
     */
    Lock removeLock(Serializable id, String owner, boolean force);

    /**
     * Marks the binaries in use by passing them to the binary manager(s)'s GC mark() method.
     */
    void markReferencedBinaries();

    /**
     * Cleans up (hard-delete) any rows that have been soft-deleted in the database.
     *
     * @param max the maximum number of rows to delete at a time
     * @param beforeTime the maximum deletion time of the rows to delete
     * @return the number of rows deleted
     */
    int cleanupDeletedRows(int max, Calendar beforeTime);

    /**
     * @since 5.9.3
     */
    boolean isConnected();

    /**
     * @since 5.9.3
     */
    void connect();

    /**
     * @since 5.9.3
     */
    void disconnect();

}
