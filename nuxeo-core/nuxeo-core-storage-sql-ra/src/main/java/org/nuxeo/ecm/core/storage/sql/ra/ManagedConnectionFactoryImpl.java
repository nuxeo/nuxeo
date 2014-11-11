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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ConnectionSpecImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * The managed connection factory receives requests from the application server
 * to create new {@link ManagedConnection} (the physical connection).
 * <p>
 * It also is a factory for {@link ConnectionFactory}s.
 *
 * @author Florent Guillaume
 */
public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory,
        ResourceAdapterAssociation, RepositoryManagement {

    private static final Log log = LogFactory.getLog(ManagedConnectionFactoryImpl.class);

    private static final long serialVersionUID = 1L;

    private final RepositoryDescriptor repositoryDescriptor;

    private String name;

    private transient ResourceAdapter resourceAdapter;

    private transient PrintWriter out;

    /**
     * The instantiated repository.
     */
    private RepositoryImpl repository;

    public ManagedConnectionFactoryImpl() {
        repositoryDescriptor = new RepositoryDescriptor();
        repositoryDescriptor.properties = new HashMap<String, String>();
    }

    /*
     * ----- Java Bean -----
     */

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setXaDataSource(String xaDataSourceName) {
        repositoryDescriptor.xaDataSourceName = xaDataSourceName;
    }

    public String getXaDataSource() {
        return repositoryDescriptor.xaDataSourceName;
    }

    public void setProperty(String property) {
        String[] split = property.split("=", 2);
        if (split.length != 2) {
            log.error("Invalid property: " + property);
            return;
        }
        repositoryDescriptor.properties.put(split[0], split[1]);
    }

    public String getProperty() {
        return null;
    }

    /*
     * ----- javax.resource.spi.ResourceAdapterAssociation -----
     */

    /**
     * Called by the application server exactly once to associate this
     * ManagedConnectionFactory with a ResourceAdapter. The ResourceAdapter may
     * then be used to look up configuration.
     */
    public void setResourceAdapter(ResourceAdapter resourceAdapter)
            throws ResourceException {
        this.resourceAdapter = resourceAdapter;
    }

    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    /*
     * ----- javax.resource.spi.ManagedConnectionFactory -----
     */

    public void setLogWriter(PrintWriter out) {
        this.out = out;
    }

    public PrintWriter getLogWriter() {
        return out;
    }

    /*
     * Used in non-managed scenarios.
     */
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(new ConnectionManagerImpl());
    }

    /*
     * Used in managed scenarios.
     */
    public Object createConnectionFactory(ConnectionManager connectionManager)
            throws ResourceException {
        ConnectionFactoryImpl connectionFactory = new ConnectionFactoryImpl(
                this, connectionManager);
        log.debug("Created repository factory (" + connectionFactory + ')');
        return connectionFactory;
    }

    /*
     * Creates a new physical connection to the underlying storage. Called by
     * the application server pool (or the non-managed ConnectionManagerImpl)
     * when it needs a new connection.
     */
    /*
     * If connectionRequestInfo is null then it means that the call is made by
     * the application server for the recovery case (6.5.3.5).
     */
    public ManagedConnection createManagedConnection(Subject subject,
            ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        assert connectionRequestInfo instanceof ConnectionRequestInfoImpl;
        initializeRepository();
        return new ManagedConnectionImpl(this,
                (ConnectionRequestInfoImpl) connectionRequestInfo);
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     * <p>
     * Called by the application server when it's looking for an appropriate
     * connection to server from a pool.
     */
    @SuppressWarnings("unchecked")
    public ManagedConnection matchManagedConnections(Set set, Subject subject,
            ConnectionRequestInfo cri) throws ResourceException {
        for (Object candidate : set) {
            if (!(candidate instanceof ManagedConnectionImpl)) {
                continue;
            }
            ManagedConnectionImpl managedConnection = (ManagedConnectionImpl) candidate;
            if (!this.equals(managedConnection.getManagedConnectionFactory())) {
                continue;
            }
            return managedConnection;
        }
        return null;
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof ManagedConnectionFactoryImpl) {
            return equals((ManagedConnectionFactoryImpl) other);
        }
        return false;
    }

    private boolean equals(ManagedConnectionFactoryImpl other) {
        return name == null ? false : name.equals(other.name);
    }

    /*
     * ----- org.nuxeo.ecm.core.storage.sql.RepositoryManagement -----
     */

    public int getActiveSessionsCount() {
        if (repository == null) {
            return 0;
        }
        return repository.getActiveSessionsCount();
    }

    public int clearCaches() {
        if (repository == null) {
            return 0;
        }
        return repository.clearCaches();
    }

    /*
     * ----- -----
     */

    private void initializeRepository() throws StorageException {
        synchronized (this) {
            if (repository == null) {
                // XXX TODO
                SchemaManager schemaManager;
                try {
                    schemaManager = Framework.getService(SchemaManager.class);
                } catch (Exception e) {
                    throw new StorageException(e);
                }
                repository = new RepositoryImpl(repositoryDescriptor,
                        schemaManager);
            }
        }
    }

    /**
     * Called by the {@link ManagedConnectionImpl} constructor to get a new
     * physical connection.
     */
    protected SessionImpl getConnection(ConnectionSpecImpl connectionSpec)
            throws StorageException {
        return repository.getConnection(connectionSpec);
    }

}
