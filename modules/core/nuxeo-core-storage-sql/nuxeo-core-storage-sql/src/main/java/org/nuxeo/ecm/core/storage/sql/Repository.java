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

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

/**
 * Interface for the low-level VCS repository.
 */
public interface Repository extends ConnectionFactory, RepositoryManagement {

    /**
     * Gets a new connection by logging in to the repository with default credentials.
     *
     * @return the session
     */
    @Override
    Session getConnection();

    /**
     * Gets a new connection by logging in to the repository with given connection information (credentials).
     *
     * @param connectionSpec the parameters to use to connnect
     * @return the session
     */
    @Override
    Session getConnection(ConnectionSpec connectionSpec);

    /**
     * Closes the repository and release all resources.
     */
    void close();

}
