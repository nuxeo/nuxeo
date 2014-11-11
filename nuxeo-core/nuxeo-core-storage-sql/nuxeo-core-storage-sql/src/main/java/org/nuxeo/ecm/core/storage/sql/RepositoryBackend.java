/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.storage.Credentials;
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
     *
     * @param model the model
     * @param pathResolver the path resolver
     * @param credentials the core session credentials
     * @param create {@code true} if the database has to be created
     *            (initialization)
     */
    Mapper newMapper(Model model, PathResolver pathResolver, Credentials credentials, boolean create)
            throws StorageException;

    /**
     * Shuts down the backend.
     */
    void shutdown() throws StorageException;

}
