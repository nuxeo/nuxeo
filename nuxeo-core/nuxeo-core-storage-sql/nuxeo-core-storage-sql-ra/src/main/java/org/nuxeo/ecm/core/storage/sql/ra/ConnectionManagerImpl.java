/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.ra;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

/**
 * This implementation of {@link ConnectionManager} is used in non-managed
 * scenarios when there is no application server to provide one.
 * <p>
 * It receives connection requests from the {@link ConnectionFactory} and passes
 * them to the application server.
 *
 * @author Florent Guillaume
 */
public class ConnectionManagerImpl implements ConnectionManager {

    private static final long serialVersionUID = 1L;

    /*
     * This method is called by the RA's connection factory.
     */
    @Override
    public Object allocateConnection(
            ManagedConnectionFactory managedConnectionFactory,
            ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        ManagedConnection managedConnection = managedConnectionFactory.createManagedConnection(
                null, connectionRequestInfo);
        return managedConnection.getConnection(null, connectionRequestInfo);
    }
}
