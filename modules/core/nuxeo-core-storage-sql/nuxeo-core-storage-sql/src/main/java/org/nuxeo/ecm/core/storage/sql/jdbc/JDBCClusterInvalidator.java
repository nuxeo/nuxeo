/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.Types;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.storage.sql.VCSClusterInvalidator;
import org.nuxeo.ecm.core.storage.sql.VCSInvalidations;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;

/**
 * Implementation of {@link VCSClusterInvalidator} that uses the JDBC Mapper to read/write invalidations.
 */
public class JDBCClusterInvalidator implements VCSClusterInvalidator {

    private static final Log log = LogFactory.getLog(JDBCClusterInvalidator.class);

    /** Cluster node id. */
    private Serializable nodeId;

    /** Cluster node mapper. Used synchronized. */
    private Mapper mapper;

    private long clusteringDelay;

    // modified only under clusterMapper synchronization
    private long clusterNodeLastInvalidationTimeMillis;

    @Override
    public void initialize(String nodeId, RepositoryImpl repository) {
        RepositoryDescriptor repositoryDescriptor = repository.getRepositoryDescriptor();
        clusteringDelay = repositoryDescriptor.getClusteringDelay();
        processClusterInvalidationsNext();
        // create mapper
        mapper = repository.newMapper(null, false);
        Serializable nodeIdSer;
        if (mapper.getClusterNodeIdType() == Types.VARCHAR) { // sql type
            nodeIdSer = nodeId;
        } else {
            try {
                nodeIdSer = Long.valueOf(nodeId);
            } catch (NumberFormatException e) {
                throw new NuxeoException("Cluster node id must be an integer", e);
            }
        }
        this.nodeId = nodeIdSer;
        try {
            mapper.createClusterNode(nodeIdSer);
        } catch (ConcurrentUpdateException e) {
            e.addInfo("Failed to initialize clustering for repository: " + repository.getName());
            throw e;
        }
        log.info("Clustering enabled for repository: " + repository.getName() + " with " + clusteringDelay
                + " ms delay " + " and cluster node id: " + nodeId);
    }

    @Override
    public void close() {
        synchronized (mapper) {
            mapper.removeClusterNode(nodeId);
            mapper.close();
        }
    }

    @Override
    public boolean requiresClusterSQL() {
        // we need specific tables to be set up
        return true;
    }

    // TODO should be called by RepositoryManagement
    protected void processClusterInvalidationsNext() {
        clusterNodeLastInvalidationTimeMillis = System.currentTimeMillis() - clusteringDelay - 1;
    }

    @Override
    public VCSInvalidations receiveInvalidations() {
        synchronized (mapper) {
            long remaining = clusterNodeLastInvalidationTimeMillis + clusteringDelay - System.currentTimeMillis();
            if (remaining > 0) {
                // delay hasn't expired
                log.trace("Not fetching invalidations, remaining time: " + remaining + "ms");
                return null;
            }
            VCSInvalidations invalidations = mapper.getClusterInvalidations(nodeId);
            clusterNodeLastInvalidationTimeMillis = System.currentTimeMillis();
            return invalidations;
        }
    }

    @Override
    public void sendInvalidations(VCSInvalidations invalidations) {
        if (invalidations == null || invalidations.isEmpty()) {
            return;
        }
        synchronized (mapper) {
            mapper.insertClusterInvalidations(nodeId, invalidations);
        }
    }

}
