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

import java.util.Calendar;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.sql.Repository;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLSession;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainer.ConnectionManagerWrapper;

/**
 * The connection factory delegates connection requests to the application server {@link ConnectionManager}.
 * <p>
 * An instance of this class is returned to the application when a JNDI lookup is done.
 *
 * @author Florent Guillaume
 */
public class ConnectionFactoryImpl implements Repository, org.nuxeo.ecm.core.model.Repository {

    private static final long serialVersionUID = 1L;

    private final ManagedConnectionFactoryImpl managedConnectionFactory;

    private final ConnectionManager connectionManager;

    private final String name;

    private Reference reference;

    /**
     * This is {@code true} if the connectionManager comes from an application server, or {@code false} if the
     * {@link ConnectionFactoryImpl} was constructed by application code and passed our manual
     * {@link ConnectionManagerImpl}.
     */
    @SuppressWarnings("unused")
    private final boolean managed;

    public ConnectionFactoryImpl(ManagedConnectionFactoryImpl managedConnectionFactory,
            ConnectionManager connectionManager) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
        managed = !(connectionManager instanceof ConnectionManagerImpl);
        name = managedConnectionFactory.getName();
    }

    /*
     * ----- javax.resource.cci.ConnectionFactory -----
     */

    /**
     * Gets a new connection.
     *
     * @param connectionSpec the connection spec (unused)
     * @return the connection
     */
    @Override
    public Session getConnection(ConnectionSpec connectionSpec) {
        return getConnection();
    }

    /**
     * Gets a new connection.
     *
     * @return the connection
     */
    @Override
    public Session getConnection() {
        try {
            return (Session) connectionManager.allocateConnection(managedConnectionFactory, null);
        } catch (ResourceException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("No ManagedConnections available")) {
                String err = "Connection pool is fully used";
                if (connectionManager instanceof ConnectionManagerWrapper) {
                    ConnectionManagerWrapper cmw = (ConnectionManagerWrapper) connectionManager;
                    NuxeoConnectionManagerConfiguration config = cmw.getConfiguration();
                    err = err + ", consider increasing " + "nuxeo.vcs.blocking-timeout-millis (currently "
                            + config.getBlockingTimeoutMillis() + ") or " + "nuxeo.vcs.max-pool-size (currently "
                            + config.getMaxPoolSize() + ")";
                }
                throw new NuxeoException(err, e);
            }
            throw new NuxeoException(e);
        }
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /*
     * ----- javax.naming.Referenceable -----
     */

    @Override
    public Reference getReference() {
        return reference;
    }

    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    /*
     * ----- Repository -----
     */

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Repository -----
     */

    @Override
    public String getName() {
        return name;
    }

    @Override
    public org.nuxeo.ecm.core.model.Session getSession() {
        return new SQLSession(getConnection(), this);
    }

    @Override
    public void shutdown() {
        try {
            NuxeoContainer.disposeConnectionManager(connectionManager);
        } catch (RuntimeException e) {
            LogFactory.getLog(ConnectionFactoryImpl.class).warn("cannot dispose connection manager of " + name);
        }
        try {
            managedConnectionFactory.shutdown();
        } catch (NuxeoException e) {
            LogFactory.getLog(ConnectionFactoryImpl.class).warn("cannot shutdown connection factory  " + name);
        }
    }

    @Override
    public FulltextConfiguration getFulltextConfiguration() {
        return managedConnectionFactory.getFulltextConfiguration();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.RepositoryManagement -----
     */

    @Override
    public int getActiveSessionsCount() {
        return managedConnectionFactory.getActiveSessionsCount();
    }

    @Override
    public long getCacheSize() {
        return managedConnectionFactory.getCacheSize();
    }

    @Override
    public long getCachePristineSize() {
        return managedConnectionFactory.getCachePristineSize();
    }

    @Override
    public long getCacheSelectionSize() {
        return managedConnectionFactory.getCacheSelectionSize();
    }

    @Override
    public int clearCaches() {
        return managedConnectionFactory.clearCaches();
    }

    @Override
    public void processClusterInvalidationsNext() {
        managedConnectionFactory.processClusterInvalidationsNext();
    }

    @Override
    public void markReferencedBinaries() {
        managedConnectionFactory.markReferencedBinaries();
    }

    @Override
    public int cleanupDeletedDocuments(int max, Calendar beforeTime) {
        return managedConnectionFactory.cleanupDeletedDocuments(max, beforeTime);
    }

}
