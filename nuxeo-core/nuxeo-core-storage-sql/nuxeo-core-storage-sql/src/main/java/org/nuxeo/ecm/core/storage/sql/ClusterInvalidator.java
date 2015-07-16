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

/**
 * Encapsulates cluster node VCS invalidations management.
 * <p>
 * There is one cluster invalidator per cluster node (repository).
 *
 * @since 7.4
 */
public interface ClusterInvalidator {

    /**
     * Initializes the cluster invalidator.
     *
     * @param nodeId the cluster node id
     * @param repository the repository
     */
    void initialize(String nodeId, RepositoryImpl repository);

    /**
     * Closes this cluster invalidator and releases resources.
     */
    void close();

    /**
     * Receives invalidations from other cluster nodes.
     */
    Invalidations receiveInvalidations();

    /**
     * Sends invalidations to other cluster nodes.
     */
    void sendInvalidations(Invalidations invalidations);

}
