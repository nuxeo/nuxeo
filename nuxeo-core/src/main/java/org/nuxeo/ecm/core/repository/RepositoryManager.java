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
     * @param name the repository name
     * @return the repository instance or null if no repository with that name
     *         was registered
     */
    Repository getRepository(String name);

    /**
     * Gets the repository names.
     *
     * @return an array of repository names
     */
    String[] getRepositoryNames();

    /**
     * Gets the descriptor for the repository of a given name.
     *
     * @param name the repository name
     * @return the repository descriptor, or {@code null} if unknown
     */
    RepositoryDescriptor getDescriptor(String name);

}
