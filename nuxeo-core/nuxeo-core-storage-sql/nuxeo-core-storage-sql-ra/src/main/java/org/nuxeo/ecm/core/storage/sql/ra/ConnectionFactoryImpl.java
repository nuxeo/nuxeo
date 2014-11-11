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

package org.nuxeo.ecm.core.storage.sql.ra;

import java.util.Calendar;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.storage.sql.Repository;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLSession;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainer.ConnectionManagerWrapper;

/**
 * The connection factory delegates connection requests to the application
 * server {@link ConnectionManager}.
 * <p>
 * An instance of this class is returned to the application when a JNDI lookup
 * is done. This is the datasource equivalent of {@link SQLRepository}.
 *
 * @author Florent Guillaume
 */
public class ConnectionFactoryImpl implements Repository,
        org.nuxeo.ecm.core.model.Repository {

    private static final long serialVersionUID = 1L;

    private final ManagedConnectionFactoryImpl managedConnectionFactory;

    private final ConnectionManager connectionManager;

    private final String name;

    private Reference reference;

    /**
     * This is {@code true} if the connectionManager comes from an application
     * server, or {@code false} if the {@link ConnectionFactoryImpl} was
     * constructed by application code and passed our manual
     * {@link ConnectionManagerImpl}.
     */
    @SuppressWarnings("unused")
    private final boolean managed;

    public ConnectionFactoryImpl(
            ManagedConnectionFactoryImpl managedConnectionFactory,
            ConnectionManager connectionManager) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = connectionManager;
        managed = !(connectionManager instanceof ConnectionManagerImpl);
        name = managedConnectionFactory.getName();
    }

    // NXP 3992 -- exposed this for clean shutdown on cluster
    public ManagedConnectionFactoryImpl getManagedConnectionFactory() {
        return managedConnectionFactory;
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
    public Session getConnection(ConnectionSpec connectionSpec)
            throws StorageException {
        return getConnection();
    }

    /**
     * Gets a new connection.
     *
     * @return the connection
     */
    @Override
    public Session getConnection() throws StorageException {
        try {
            return (Session) connectionManager.allocateConnection(
                    managedConnectionFactory, null);
        } catch (StorageException e) {
            throw e;
        } catch (ResourceException e) {
            String msg = e.getMessage();
            if (msg != null
                    && msg.startsWith("No ManagedConnections available")) {
                String err = "Connection pool is fully used";
                if (connectionManager instanceof ConnectionManagerWrapper) {
                    ConnectionManagerWrapper cmw = (ConnectionManagerWrapper) connectionManager;
                    NuxeoConnectionManagerConfiguration config = cmw.getConfiguration();
                    err = err + ", consider increasing "
                            + "nuxeo.vcs.blocking-timeout-millis (currently "
                            + config.getBlockingTimeoutMillis() + ") or "
                            + "nuxeo.vcs.max-pool-size (currently "
                            + config.getMaxPoolSize() + ")";
                }
                throw new StorageException(err, e);
            }
            throw new StorageException(e);
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
    public void close() throws StorageException {
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
    public org.nuxeo.ecm.core.model.Session getSession(String sessionId)
            throws DocumentException {
        try {
            return new SQLSession(getConnection(), this, sessionId);
        } catch (StorageException e) {
            throw new DocumentException(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        try {
            NuxeoContainer.disposeConnectionManager(connectionManager);
        } catch (Exception e) {
            LogFactory.getLog(ConnectionFactoryImpl.class).warn("cannot dispose connection manager of "
                    + name);
        }
        try {
            managedConnectionFactory.shutdown();
        } catch (StorageException e) {
            LogFactory.getLog(ConnectionFactoryImpl.class).warn("cannot shutdown connection factory  "
                    + name);
        }
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
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return managedConnectionFactory.getBinaryGarbageCollector();
    }

    @Override
    public void markReferencedBinaries(BinaryGarbageCollector gc) {
        managedConnectionFactory.markReferencedBinaries(gc);
    }

    @Override
    public int cleanupDeletedDocuments(int max, Calendar beforeTime) {
        return managedConnectionFactory.cleanupDeletedDocuments(max, beforeTime);
    }

}
