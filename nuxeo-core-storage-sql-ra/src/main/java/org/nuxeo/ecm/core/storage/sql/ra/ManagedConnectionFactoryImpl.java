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

/**
 * The managed connection factory receives requests from the application server
 * to create new {@link ManagedConnection} (the physical connection).
 * <p>
 * It also is a factory for {@link ConnectionFactory}s.
 *
 * @author Florent Guillaume
 */
public class ManagedConnectionFactoryImpl implements
        ManagedConnectionFactory, ResourceAdapterAssociation {

    private static final Log log = LogFactory.getLog(ManagedConnectionFactoryImpl.class);

    private static final long serialVersionUID = 1L;

    /**
     * The repository name
     * <p>
     * Needed to be able to lazy load the repository descriptor (the repository
     * may not be yet registered at time of data source deployment).
     */
    private String name;

    private transient ResourceAdapter resourceAdapter;

    private transient PrintWriter out;

    public ManagedConnectionFactoryImpl() {
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

    public PrintWriter getLogWriter() {
        return out;
    }

    public void setLogWriter(PrintWriter out) {
        this.out = out;
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
        return new ManagedConnectionImpl(this, connectionRequestInfo);
    }

    /*
     * Returns a matched connection from the candidate set of connections.
     */
    @SuppressWarnings("unchecked")
    public ManagedConnection matchManagedConnections(Set set, Subject subject,
            ConnectionRequestInfo cri) throws ResourceException {
        log.debug("---------- calling matchManagedConnection");
        for (Object candidate : set) {
            if (!(candidate instanceof ManagedConnectionImpl)) {
                continue;
            }
            ManagedConnectionImpl managedConnection = (ManagedConnectionImpl) candidate;
            if (!equals(managedConnection.getManagedConnectionFactory())) {
                continue;
            }
            if (!managedConnection.isHandleValid()) {
                // reuse the first inactive managedConnection
                log.debug("------- matching.. " + managedConnection.getHandle());
                // reinitialize the connection
                managedConnection.initializeHandle(cri);
                return managedConnection;
            }
        }
        log.debug("---------- no match");
        return null;
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }

    private boolean equals(ManagedConnectionFactoryImpl other) {
        return name == null ? false : name.equals(other.name);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof ManagedConnectionFactoryImpl) {
            return equals((ManagedConnectionFactoryImpl) other);
        } else {
            return false;
        }
    }

    /*
     * ----- -----
     */

}
