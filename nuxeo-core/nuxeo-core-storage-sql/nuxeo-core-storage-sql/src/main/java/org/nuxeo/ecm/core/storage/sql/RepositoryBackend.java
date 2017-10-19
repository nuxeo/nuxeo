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

import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;

/**
 * Interface for the backend-specific initialization code of a {@link Repository}.
 *
 * @see RepositoryImpl
 */
public interface RepositoryBackend {

    /**
     * Initializer.
     */
    void initialize(RepositoryImpl repository);

    /**
     * Initializes the {@link ModelSetup}. Called once lazily at repository initialization.
     */
    void initializeModelSetup(ModelSetup modelSetup);

    /**
     * Initializes the database, if needed, according to DDL mode.
     *
     * @since 9.3
     */
    void initializeDatabase(Model model);

    /**
     * Sets the cluster invalidator, to be used by future mappers created.
     *
     * @since 7.4
     */
    void setClusterInvalidator(ClusterInvalidator clusterInvalidator);

    /**
     * Creates a new instance a {@link Mapper}. Called once for every new session.
     *
     * @param model the model
     * @param pathResolver the path resolver
     * @param useInvalidations whether this mapper participates in invalidation propagation
     */
    Mapper newMapper(Model model, PathResolver pathResolver, boolean useInvalidations);

    /**
     * Shuts down the backend.
     */
    void shutdown();

}
