/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.repository;

import java.util.Collection;
import java.util.List;

/**
 * High-level service to get to a {@link org.nuxeo.ecm.core.api.repository.Repository Repository} and from there to
 * {@link org.nuxeo.ecm.core.api.CoreSession CoreSession} objects.
 */
public interface RepositoryManager {

    /**
     * Gets all registered repositories.
     *
     * @return a read-only collection of repositories or an empty one if no repositories was defined
     */
    Collection<Repository> getRepositories();

    /**
     * Gets the names of registered repositories.
     *
     * @since 5.9.3
     * @return a list of repository names
     */
    List<String> getRepositoryNames();

    /**
     * Gets a repository by its name.
     *
     * @param name the repository name
     * @return the repository or null if not found
     */
    Repository getRepository(String name);

    /**
     * Registers a new repository.
     *
     * @param repository the repository to register
     */
    void addRepository(Repository repository);

    /**
     * Removes a registered repository.
     * <p>
     * Do nothing if the repository is not registered.
     *
     * @param name the repository name to unregister
     */
    void removeRepository(String name);

    /**
     * Gets the default repository.
     * <p>
     * If there is not a default repository returns the first registered. repository
     * <p>
     * This is a convenient method to get the repository for application having a single repository.
     *
     * @return the default repository
     */
    Repository getDefaultRepository();

    /**
     * Gets the name of the default repository.
     * <p>
     * If there is not a default repository, returns the name of the first registered repository.
     *
     * @return the default repository name
     */
    String getDefaultRepositoryName();

}
