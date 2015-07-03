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
