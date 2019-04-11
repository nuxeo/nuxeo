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

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;

/**
 * The managed connection represents an actual physical connection to the underlying storage. It is created by the
 * {@link ManagedConnectionFactory}, and then encapsulated into a {@link Connection} which is then returned to the
 * application (via the {@link ConnectionFactory}).
 * <p>
 * If sharing is allowed, several different {@link Connection}s may be associated to a given {@link ManagedConnection},
 * although not at the same time.
 *
 * @author Florent Guillaume
 */
public class ManagedConnectionImpl implements ManagedConnection, ManagedConnectionMetaData {

    private static final Log log = LogFactory.getLog(ManagedConnectionImpl.class);

    private PrintWriter out;

    private final ManagedConnectionFactoryImpl managedConnectionFactory;

    /**
     * All the {@link Connection} handles for this managed connection. There is usually only one, unless sharing is in
     * effect.
     */
    private final Set<ConnectionImpl> connections;

    /**
     * The low-level session managed by this connection.
     */
    private final SessionImpl session;

    /**
     * The wrapped session as a connection-aware xaresource.
     */
    private final XAResource xaresource;

    /**
     * List of listeners set by the application server which we must notify of all activity happening on our
     * {@link Connection}.
     */
    private final ListenerList listeners;

    /**
     * Creates a new physical connection to the underlying storage. Called by the {@link ManagedConnectionFactory} when
     * it needs a new connection.
     *
     * @throws ResourceException
     */
    public ManagedConnectionImpl(ManagedConnectionFactoryImpl managedConnectionFactory) throws ResourceException {
        log.debug("construct: " + this);
        out = managedConnectionFactory.getLogWriter();
        this.managedConnectionFactory = managedConnectionFactory;
        connections = new HashSet<>();
        listeners = new ListenerList();
        // create the underlying session
        session = managedConnectionFactory.getConnection();
        xaresource = session.getXAResource();
    }

    /*
     * ----- javax.resource.spi.ManagedConnection -----
     */

    /**
     * Creates a new {@link Connection} handle to this {@link ManagedConnection} .
     */
    @Override
    public synchronized Connection getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        // connectionRequestInfo unused
        log.debug("getConnection: " + this);
        ConnectionImpl connection = new ConnectionImpl(this);
        addConnection(connection);
        return connection;
    }

    /**
     * Cleans up the physical connection, so that it may be reused.
     * <p>
     * Called by the application server before putting back this {@link ManagedConnection} into the application server
     * pool.
     * <p>
     * Later, the application server may call {@link #getConnection} again.
     */
    @Override
    public void cleanup() {
        log.debug("cleanup: " + this);
        synchronized (connections) {
            // TODO session.cancel
            connections.clear();
        }
    }

    /**
     * Destroys the physical connection.
     * <p>
     * Called by the application server before this {@link ManagedConnection} is destroyed.
     */
    @Override
    public void destroy() throws ResourceException {
        log.debug("destroy: " + this);
        try {
            session.close();
        } finally {
            cleanup();
        }
    }

    /**
     * Called by the application server to change the association of an application-level {@link Connection} handle with
     * a {@link ManagedConnection} instance.
     */
    @Override
    public void associateConnection(Object object) throws ResourceException {
        ConnectionImpl connection = (ConnectionImpl) object;
        log.debug("associateConnection: " + this + ", connection: " + connection);
        ManagedConnectionImpl other = connection.getManagedConnection();
        if (other != this) {
            log.debug("associateConnection other: " + other);
            other.removeConnection(connection);
            addConnection(connection);
        }
    }

    @Override
    public XAResource getXAResource() {
        return xaresource;
    }

    @Override
    public LocalTransaction getLocalTransaction() {
        throw new UnsupportedOperationException("Local transactions not supported");
    }

    /**
     * Called by the application server to add a listener who should be notified of all relevant events on this
     * connection.
     */
    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Called by the application server to remove a listener.
     */
    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return this;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.out = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return out;
    }

    /*
     * ----- javax.resource.spi.ManagedConnectionMetaData -----
     */

    @Override
    public String getEISProductName() {
        return "Nuxeo Core SQL Storage";
    }

    @Override
    public String getEISProductVersion() {
        return "1.0.0";
    }

    @Override
    public int getMaxConnections() {
        return Integer.MAX_VALUE; // or lower?
    }

    @Override
    public String getUserName() throws ResourceException {
        return null;
    }

    /*
     * ----- Internal -----
     */

    /**
     * Adds a connection to those using this managed connection.
     */
    private void addConnection(ConnectionImpl connection) {
        log.debug("addConnection: " + connection);
        if (connections.add(connection) == false) {
            throw new IllegalStateException("already known connection " + connection + " in " + this);
        }
        connection.setManagedConnection(this);
        connection.associate(session);
    }

    /**
     * Removes a connection to those using this managed connection.
     */
    private void removeConnection(ConnectionImpl connection) {
        log.debug("removeConnection: " + connection);
        if (connections.remove(connection) == false) {
            throw new IllegalStateException("unknown connection " + connection + " in " + this);
        }
        connection.setManagedConnection(null);
        connection.disassociate();
    }

    /**
     * Called by {@link ConnectionImpl#close} when the connection is closed.
     */
    protected void close(ConnectionImpl connection) {
        removeConnection(connection);
        sendClosedEvent(connection);
    }

    /**
     * Called by {@link ManagedConnectionFactoryImpl#matchManagedConnections}.
     */
    protected ManagedConnectionFactoryImpl getManagedConnectionFactory() {
        return managedConnectionFactory;
    }

    /*
     * ----- Event management -----
     */

    private void sendClosedEvent(ConnectionImpl connection) {
        log.debug("closing a connection " + connection);
        sendEvent(ConnectionEvent.CONNECTION_CLOSED, connection, null);
    }

    protected void sendTxStartedEvent(ConnectionImpl connection) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED, connection, null);
    }

    protected void sendTxCommittedEvent(ConnectionImpl connection) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, connection, null);
    }

    protected void sendTxRolledbackEvent(ConnectionImpl connection) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, connection, null);
    }

    protected void sendErrorEvent(ConnectionImpl connection, Exception cause) {
        sendEvent(ConnectionEvent.CONNECTION_ERROR_OCCURRED, connection, cause);
    }

    private void sendEvent(int type, ConnectionImpl connection, Exception cause) {
        ConnectionEvent event = new ConnectionEvent(this, type, cause);
        if (connection != null) {
            event.setConnectionHandle(connection);
        }
        sendEvent(event);
    }

    /**
     * Notifies the application server, through the {@link ConnectionEventListener}s it has registered with us, of what
     * happens with this connection.
     */
    private void sendEvent(ConnectionEvent event) {
        for (Object object : listeners.getListeners()) {
            ConnectionEventListener listener = (ConnectionEventListener) object;
            switch (event.getId()) {
            case ConnectionEvent.CONNECTION_CLOSED:
                listener.connectionClosed(event);
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                listener.localTransactionStarted(event);
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                listener.localTransactionCommitted(event);
                break;
            case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                listener.localTransactionRolledback(event);
                break;
            case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                listener.connectionErrorOccurred(event);
                break;
            }
        }
    }

}
