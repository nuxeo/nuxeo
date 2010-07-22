/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;

/**
 * Interface for the backend-specific initialization code of a
 * {@link Repository}.
 *
 * @see RepositoryImpl
 */
public interface RepositoryBackend {

    /**
     * Initializer.
     */
    void initialize(RepositoryImpl repository) throws StorageException;

    /**
     * Initializes the {@link ModelSetup}. Called once lazily at repository
     * initialization.
     */
    void initializeModelSetup(ModelSetup modelSetup) throws StorageException;

    /**
     * Initializes what's needed after the {@link Model} has been created.
     * Called once lazily at repository initialization.
     */
    void initializeModel(Model model) throws StorageException;

    /**
     * Creates a new instance a {@link Mapper}. Called once for every new
     * session.
     */
    Mapper newMapper(Model model, PathResolver pathResolver)
            throws StorageException;

    /**
     * Shuts down the backend.
     */
    void shutdown() throws StorageException;

}
