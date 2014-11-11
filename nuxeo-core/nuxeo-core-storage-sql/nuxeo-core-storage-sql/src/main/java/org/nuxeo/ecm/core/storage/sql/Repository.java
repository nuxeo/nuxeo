/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * @author Florent Guillaume
 */
public interface Repository extends ConnectionFactory, RepositoryManagement {

    /**
     * Gets a new connection by logging in to the repository with default
     * credentials.
     *
     * @return the session
     * @throws StorageException
     */
    @Override
    Session getConnection() throws StorageException;

    /**
     * Gets a new connection by logging in to the repository with given
     * connection information (credentials).
     *
     * @param connectionSpec the parameters to use to connnect
     * @return the session
     * @throws StorageException
     */
    @Override
    Session getConnection(ConnectionSpec connectionSpec)
            throws StorageException;

    /**
     * Closes the repository and release all resources.
     *
     * @throws StorageException
     */
    void close() throws StorageException;

}
