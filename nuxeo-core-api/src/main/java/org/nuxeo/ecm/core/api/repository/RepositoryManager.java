/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.repository;

import java.util.Collection;
import java.util.List;

/**
 * High-level service to get to a
 * {@link org.nuxeo.ecm.core.api.repository.Repository Repository} and from
 * there to {@link org.nuxeo.ecm.core.api.CoreSession CoreSession} objects.
 */
public interface RepositoryManager {

    /**
     * Gets all registered repositories.
     *
     * @return a read-only collection of repositories or an empty one if no
     *         repositories was defined
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
     * If there is not a default repository returns the first registered.
     * repository
     * <p>
     * This is a convenient method to get the repository for application having
     * a single repository.
     *
     * @return the default repository
     */
    Repository getDefaultRepository();

}
