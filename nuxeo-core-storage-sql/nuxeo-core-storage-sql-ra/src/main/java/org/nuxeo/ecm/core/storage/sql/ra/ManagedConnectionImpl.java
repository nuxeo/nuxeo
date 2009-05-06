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
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.sql.ConnectionSpecImpl;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;

/**
 * The managed connection represents an actual physical connection to the
 * underlying storage. It is created by the {@link ManagedConnectionFactory},
 * and then encapsulated into a {@link Connection} which is then returned to the
 * application (via the {@link ConnectionFactory}).
 * <p>
 * If sharing is allowed, several different {@link Connection}s may be
 * associated to a given {@link ManagedConnection}, although not at the same
 * time.
 *
 * @author Florent Guillaume
 */
public class ManagedConnectionImpl implements ManagedConnection,
        ManagedConnectionMetaData {

    private static final Log log = LogFactory.getLog(ManagedConnectionImpl.class);

    private PrintWriter out;

    private final ManagedConnectionFactoryImpl managedConnectionFactory;

    private final ConnectionSpecImpl connectionSpec;

    /**
     * All the {@link Connection} handles for this managed connection. There is
     * usually only one, unless sharing is in effect.
     */
    private final Set<ConnectionImpl> connections;

    /**
     * The low-level session managed by this connection.
     */
    private final SessionImpl session;

    /**
     * The wrapped session as a connection-aware xaresource.
     */
    private final ConnectionAwareXAResource xaresource;

    /**
     * List of listeners set by the application server which we must notify of
     * all activity happening on our {@link Connection}.
     */
    private final ListenerList listeners;

    /**
     * Creates a new physical connection to the underlying storage. Called by
     * the {@link ManagedConnectionFactory} when it needs a new connection.
     *
     * @throws ResourceException
     */
    public ManagedConnectionImpl(
            ManagedConnectionFactoryImpl managedConnectionFactory,
            ConnectionRequestInfoImpl connectionRequestInfo)
            throws ResourceException {
        out = managedConnectionFactory.getLogWriter();
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionSpec = connectionRequestInfo.connectionSpec;
        connections = new HashSet<ConnectionImpl>();
        listeners = new ListenerList();
        // create the underlying session
        session = managedConnectionFactory.getConnection(connectionSpec);
        xaresource = new ConnectionAwareXAResource(session.getXAResource(),
                this);
    }

    /*
     * ----- javax.resource.spi.ManagedConnection -----
     */

    /**
     * Creates a new {@link Connection} handle to this {@link ManagedConnection}
     * .
     */
    public synchronized Connection getConnection(Subject subject,
            ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        log.debug("getConnection: " + this);
        assert connectionRequestInfo instanceof ConnectionRequestInfoImpl;
        ConnectionImpl connection = new ConnectionImpl(this);
        addConnection(connection);
        return connection;
    }

    /**
     * Cleans up the physical connection, so that it may be reused.
     * <p>
     * Called by the application server before putting back this
     * {@link ManagedConnection} into the application server pool.
     * <p>
     * Later, the application server may call {@link #getConnection} again.
     */
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
     * Called by the application server before this {@link ManagedConnection} is
     * destroyed.
     */
    public void destroy() throws ResourceException {
        log.debug("destroy: " + this);
        cleanup();
        session.close();
    }

    /**
     * Called by the application server to change the association of an
     * application-level {@link Connection} handle with a
     * {@link ManagedConnection} instance.
     */
    public void associateConnection(Object object) throws ResourceException {
        ConnectionImpl connection = (ConnectionImpl) object;
        log.debug("associateConnection: " + this + ", connection: "
                + connection);
        ManagedConnectionImpl other = connection.getManagedConnection();
        if (other != this) {
            log.debug("associateConnection other: " + other);
            // deassociate it from other ManagedConnection
            other.removeConnection(connection);
            // reassociate it with this
            connection.setManagedConnection(this);
            addConnection(connection);
        }
    }

    public XAResource getXAResource() {
        return xaresource;
    }

    public LocalTransaction getLocalTransaction() {
        throw new UnsupportedOperationException(
                "Local transactions not supported");
    }

    /**
     * Called by the application server to add a listener who should be notified
     * of all relevant events on this connection.
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Called by the application server to remove a listener.
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    public ManagedConnectionMetaData getMetaData() {
        return this;
    }

    public void setLogWriter(PrintWriter out) {
        this.out = out;
    }

    public PrintWriter getLogWriter() {
        return out;
    }

    /*
     * ----- javax.resource.spi.ManagedConnectionMetaData -----
     */

    public String getEISProductName() {
        return "Nuxeo Core SQL Storage";
    }

    public String getEISProductVersion() {
        return "1.0.0";
    }

    public int getMaxConnections() {
        return Integer.MAX_VALUE; // or lower?
    }

    public String getUserName() throws ResourceException {
        Credentials credentials = connectionSpec.getCredentials();
        if (credentials == null) {
            return ""; // XXX
        }
        return credentials.getUserName();
    }

    /*
     * ----- Internal -----
     */

    /**
     * Adds a connection to those using this managed connection.
     */
    private void addConnection(ConnectionImpl connection) {
        synchronized (connections) {
            log.debug("addConnection: " + connection);
            connections.add(connection);
            connection.associate(session);
        }
    }

    /**
     * Removes a connection from those of this managed connection.
     */
    private void removeConnection(ConnectionImpl connection) {
        synchronized (connections) {
            log.debug("removeConnection: " + connection);
            connection.disassociate();
            connections.remove(connection);
        }
    }

    /**
     * Called by {@link ConnectionImpl#close} when the connection is closed.
     */
    protected void close(ConnectionImpl connection) {
        log.debug("close: " + this);
        removeConnection(connection);
        sendClosedEvent(connection);
    }

    /**
     * Called by {@link ConnectionAwareXAResource} at the end of a transaction.
     */
    protected void closeConnections() {
        log.debug("closeConnections: " + this);
        synchronized (connections) {
            // copy to avoid ConcurrentModificationException
            ConnectionImpl[] array = new ConnectionImpl[connections.size()];
            for (ConnectionImpl connection : connections.toArray(array)) {
                log.debug("closing connection: " + connection);
                connection.disassociate();
                sendClosedEvent(connection);
            }
            connections.clear();
        }
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
        sendEvent(ConnectionEvent.CONNECTION_CLOSED, connection, null);
    }

    protected void sendTxStartedEvent(ConnectionImpl connection) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED, connection, null);
    }

    protected void sendTxCommittedEvent(ConnectionImpl connection) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, connection, null);
    }

    protected void sendTxRolledbackEvent(ConnectionImpl connection) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, connection,
                null);
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
     * Notifies the application server, through the
     * {@link ConnectionEventListener}s it has registered with us, of what
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
