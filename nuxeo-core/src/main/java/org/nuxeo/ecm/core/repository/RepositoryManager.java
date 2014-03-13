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
package org.nuxeo.ecm.core.repository;

import java.util.List;

import org.nuxeo.ecm.core.model.Repository;

/**
 * Holds instance of high-level repositories.
 *
 * @since 5.9.3
 */
public interface RepositoryManager {

    /**
     * Gets a repository given its name.
     * <p>
     * Null is returned if no repository with that name was registered.
     *
     * @param repositoryName the repository name
     * @return the repository instance or null if no repository with that name
     *         was registered
     */
    Repository getRepository(String repositoryName);

    /**
     * Gets the repository names.
     *
     * @return a list of repository names
     */
    List<String> getRepositoryNames();

}
