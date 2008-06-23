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

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;

import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ConnectionSpecImpl;
import org.nuxeo.ecm.core.storage.sql.Repository;
import org.nuxeo.ecm.core.storage.sql.Session;

/**
 * The connection factory delegates connection requests to the application
 * server {@link ConnectionManager}.
 *
 * @author Florent Guillaume
 */
public class ConnectionFactoryImpl implements Repository {

    private static final long serialVersionUID = 1L;

    private final ManagedConnectionFactoryImpl managedConnectionFactory;

    private final ConnectionManager connectionManager;

    private Reference reference;

    /**
     * This is {@code true} if the connectionManager comes from an application
     * server, or {@code false} if the {@link ConnectionFactoryImpl} was
     * constructed by application code and passed our manual
     * {@link ConnectionManagerImpl}.
     */
    private boolean managed;

    public ConnectionFactoryImpl(
            ManagedConnectionFactoryImpl managedConnectionFactory,
            ConnectionManager connectionManager) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
        managed = !(connectionManager instanceof ConnectionManagerImpl);
    }

    /*
     * ----- javax.resource.cci.ConnectionFactory -----
     */

    /**
     * Gets a new connection, with no credentials.
     *
     * @return the connection
     */
    public Session getConnection() throws StorageException {
        ConnectionRequestInfo connectionRequestInfo = new ConnectionRequestInfoImpl();
        try {
            return (Session) connectionManager.allocateConnection(
                    managedConnectionFactory, connectionRequestInfo);
        } catch (StorageException e) {
            throw e;
        } catch (ResourceException e) {
            throw new StorageException(e);
        }
    }

    /**
     * Gets a new connection.
     *
     * @param connectionSpec the connection spec, containing credentials
     * @return the connection
     */
    public Session getConnection(ConnectionSpec connectionSpec)
            throws StorageException {
        assert connectionSpec instanceof ConnectionSpecImpl;
        // encapsulate connectionSpec into internal connectionRequestInfo
        ConnectionRequestInfo connectionRequestInfo = new ConnectionRequestInfoImpl(
                (ConnectionSpecImpl) connectionSpec);
        try {
            return (Session) connectionManager.allocateConnection(
                    managedConnectionFactory, connectionRequestInfo);
        } catch (StorageException e) {
            throw e;
        } catch (ResourceException e) {
            throw new StorageException(e);
        }
    }

    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public RecordFactory getRecordFactory() throws ResourceException {
        throw new RuntimeException("Not implemented");
    }

    /*
     * ----- javax.naming.Referenceable -----
     */

    public Reference getReference() {
        return reference;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    /*
     * ----- Repository -----
     */

    public void close() throws StorageException {
        throw new RuntimeException("Not implemented");
    }

    public long getNextTemporaryId() {
        throw new RuntimeException("Not implemented");
    }

}
