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

package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.storage.ClusterInvalidator;

/**
 * Encapsulates cluster node VCS invalidations management.
 * <p>
 * There is one cluster invalidator per cluster node (repository).
 *
 * @since 7.4
 */
public interface VCSClusterInvalidator extends ClusterInvalidator<VCSInvalidations> {

    /**
     * Initializes the cluster invalidator.
     *
     * @param nodeId the cluster node id
     * @param repository the repository
     */
    void initialize(String nodeId, RepositoryImpl repository);

    /**
     * Checks if this invalidator requires specific database-level structures.
     *
     * @since 11.1
     */
    default boolean requiresClusterSQL() {
        return false;
    }

}
