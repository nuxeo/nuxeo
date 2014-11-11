/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.repository;

import java.util.Collection;

/**
 * Manage repositories.
 *
 * TODO: This should be merged with the RepositoryManager from the core module
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
     * Unregisters all repositories.
     */
    void clear();

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
