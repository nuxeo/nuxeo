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

package org.nuxeo.ecm.core.storage.dbs;

/**
 * Encapsulates cluster node DBS invalidations management.
 * <p>
 * There is one cluster invalidator per cluster node (repository).
 *
 * @since 8.10
 */
public interface DBSClusterInvalidator {

    /**
     * Initializes the cluster invalidator.
     *
     * @param nodeId the cluster node id
     * @param repositoryName the repository name
     */
    void initialize(String nodeId, String repositoryName);

    /**
     * Closes this cluster invalidator and releases resources.
     */
    void close();

    /**
     * Receives invalidations from other cluster nodes.
     */
    DBSInvalidations receiveInvalidations();

    /**
     * Sends invalidations to other cluster nodes.
     */
    void sendInvalidations(DBSInvalidations invalidations);

}
