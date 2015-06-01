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

package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.Types;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.ConnectionResetException;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.InvalidationsPropagator;
import org.nuxeo.ecm.core.storage.sql.InvalidationsQueue;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;

/**
 * Encapsulates cluster node operations.
 * <p>
 * There is one cluster node handler per cluster node (repository).
 */
public class ClusterNodeHandler {

    private static final Log log = LogFactory.getLog(ClusterNodeHandler.class);

    protected static final Random RANDOM = new Random();

    /** Cluster node mapper. Used synchronized. */
    private final Mapper clusterNodeMapper;

    private final long clusteringDelay;

    // modified only under clusterMapper synchronization
    private long clusterNodeLastInvalidationTimeMillis;

    /** Propagator of invalidations to the cluster node's mappers. */
    private final InvalidationsPropagator propagator;

    /** Cluster node id. */
    private Serializable nodeId;

    public ClusterNodeHandler(Mapper clusterNodeMapper, RepositoryDescriptor repositoryDescriptor)
            throws StorageException {
        this.clusterNodeMapper = clusterNodeMapper;

        String nodeIdString = repositoryDescriptor.getClusterNodeId();
        if (StringUtils.isBlank(nodeIdString)) {
            // need a smallish int because of SQL Server legacy node ids
            nodeIdString = String.valueOf(RANDOM.nextInt(32768));
            log.warn(
                    "Missing cluster node id configuration, please define it explicitly (usually through repository.clustering.id). "
                            + "Using random cluster node id instead: " + nodeIdString);
        }
        int type = clusterNodeMapper.getClusterNodeIdType(); // sql type
        if (type == Types.VARCHAR) {
            nodeId = nodeIdString.trim();
        } else {
            nodeId = Long.valueOf(nodeIdString);
        }
        log.info("Initializing cluster node: " + nodeId);
        clusterNodeMapper.createClusterNode(nodeId);
        clusteringDelay = repositoryDescriptor.getClusteringDelay();
        processClusterInvalidationsNext();
        propagator = new InvalidationsPropagator("cluster-" + this);
    }

    public JDBCConnection getConnection() {
        return (JDBCConnection) clusterNodeMapper;
    }

    public void close() throws StorageException {
        synchronized (clusterNodeMapper) {
            try {
                clusterNodeMapper.removeClusterNode(nodeId);
            } catch (StorageException e) {
                log.error(e.getMessage(), e);
            }
            clusterNodeMapper.close();
        }
    }

    public void connectionWasReset() throws StorageException {
        synchronized (clusterNodeMapper) {
            // TODO needed?
            // all invalidations queued for us have been lost
            // so reset all
            propagator.propagateInvalidations(new Invalidations(true), null);
        }
    }

    // TODO should be called by RepositoryManagement
    public void processClusterInvalidationsNext() {
        clusterNodeLastInvalidationTimeMillis = System.currentTimeMillis() - clusteringDelay - 1;
    }

    /**
     * Adds an invalidation queue to this cluster node.
     */
    public void addQueue(InvalidationsQueue queue) {
        propagator.addQueue(queue);
    }

    /**
     * Removes an invalidation queue from this cluster node.
     */
    public void removeQueue(InvalidationsQueue queue) {
        propagator.removeQueue(queue);
    }

    /**
     * Propagates invalidations to all the queues of this cluster node.
     */
    public void propagateInvalidations(Invalidations invalidations, InvalidationsQueue skipQueue) {
        propagator.propagateInvalidations(invalidations, null);
    }

    /**
     * Receives cluster invalidations from other cluster nodes.
     */
    public Invalidations receiveClusterInvalidations() throws StorageException {
        synchronized (clusterNodeMapper) {
            long remaining = clusterNodeLastInvalidationTimeMillis + clusteringDelay - System.currentTimeMillis();
            if (remaining > 0) {
                // delay hasn't expired
                log.trace("Not fetching invalidations, remaining time: " + remaining + "ms");
                return null;
            }
            Invalidations invalidations;
            try {
                invalidations = clusterNodeMapper.getClusterInvalidations(nodeId);
            } catch (ConnectionResetException e) {
                // retry once
                invalidations = clusterNodeMapper.getClusterInvalidations(nodeId);
            }
            clusterNodeLastInvalidationTimeMillis = System.currentTimeMillis();
            return invalidations;
        }
    }

    /**
     * Sends cluster invalidations to other cluster nodes.
     */
    public void sendClusterInvalidations(Invalidations invalidations) throws StorageException {
        if (invalidations == null || invalidations.isEmpty()) {
            return;
        }
        synchronized (clusterNodeMapper) {
            clusterNodeMapper.insertClusterInvalidations(nodeId, invalidations);
        }
    }

}
