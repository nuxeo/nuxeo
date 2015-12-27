/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * This implementation of {@link ConnectionManager} is used in non-managed scenarios when there is no application server
 * to provide one.
 * <p>
 * It receives connection requests from the {@link ConnectionFactory} and passes them to the application server.
 *
 * @author Florent Guillaume
 */
public class ConnectionManagerImpl implements ConnectionManager {

    private static final long serialVersionUID = 1L;

    /*
     * This method is called by the RA's connection factory.
     */
    @Override
    public Object allocateConnection(ManagedConnectionFactory managedConnectionFactory,
            ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        // connectionRequestInfo unused
        ManagedConnection managedConnection = managedConnectionFactory.createManagedConnection(null, null);
        return managedConnection.getConnection(null, null);
    }
}
