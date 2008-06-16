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
import java.io.Serializable;
import java.util.Map;

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
import org.nuxeo.ecm.core.storage.sql.Session;

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

    private final ManagedConnectionFactoryImpl managedConnectionFactory;

    private final ConnectionRequestInfo connectionRequestInfo;

    /**
     * List of listeners set by the application server which we must notify of
     * all activity happening on our {@link Connection}s.
     */
    private final ListenerList listeners;

    /**
     * High-level {@link Connection} returned to the application, if an
     * association has been made.
     */
    private ConnectionImpl connection;

    private PrintWriter out;

    /**
     * Creates a new physical connection to the underlying storage. Called by
     * the {@link ManagedConnectionFactory} when it needs a new connection.
     */
    public ManagedConnectionImpl(
            ManagedConnectionFactoryImpl managedConnectionFactory,
            ConnectionRequestInfo connectionRequestInfo) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionRequestInfo = connectionRequestInfo;
        listeners = new ListenerList();
        out = managedConnectionFactory.getLogWriter();
    }

    /*
     * ----- javax.resource.spi.ManagedConnection -----
     */

    public PrintWriter getLogWriter() {
        return out;
    }

    public void setLogWriter(PrintWriter out) {
        this.out = out;
    }

    /**
     * Creates a new {@link Connection} handle to this {@link ManagedConnection}
     * .
     */
    public synchronized Connection getConnection(Subject subject,
            ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        log.debug("--------- getConnection");
        if (connection != null) {
            throw new ResourceException("Sharing not supported");
        }
        initializeHandle(connectionRequestInfo);
        log.debug(">>>>>>>> returning: " + connection);
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
        log.debug("------- calling cleanup");
        connection = null;
    }

    /**
     * Destroys the physical connection.
     * <p>
     * Called by the application server before this {@link ManagedConnection} is
     * destroyed.
     */
    public void destroy() {
        log.debug("------- calling destroy");
        cleanup();
        // TODO close physical connection
    }

    /*
     * Used by the application server to change the association of an
     * application-level connection handle with a ManagedConnection instance.
     */
    public synchronized void associateConnection(Object connection)
            throws ResourceException {
        log.debug("----- calling associateConnection");
        ConnectionImpl handle = (ConnectionImpl) connection;
        if (handle.getManagedConnection() != this) {
            try {
                closeHandle(this.connection);
                this.connection = handle;
                this.connection.getManagedConnection().connection = null;
                this.connection.setManagedConnection(this);
            } catch (ResourceException e) {
                throw new ResourceException("Failed to close handle", e);
            }
        }
    }

    public XAResource getXAResource() throws ResourceException {
        log.debug("----- get XAResource");
        initializeHandle(connectionRequestInfo);
        // return xar;
        return null;
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
        return "XXX"; // XXX
    }

    /*
     * -----
     */

    private Session openSession(ConnectionRequestInfo cri)
            throws ResourceException {
        // Session session =
        // managedConnectionFactory.getRepository().getSession();
        // log("Created session for repository " +
        // managedConnectionFactory.getRepository().getName());
        // return session;
        return null;
    }

    public ManagedConnectionFactoryImpl getManagedConnectionFactory() {
        return managedConnectionFactory;
    }

    protected void closeHandle(ConnectionImpl connection)
            throws ResourceException {
        if (this.connection != connection) {
            throw new ResourceException(
                    "Connection not associated with this ManagedConnection");
        }
        // log.debug(">>>>>>>>>>>>>>> trying to close handle ........."+
        // handle);
        if (isHandleValid()) {
            // log.debug(">>>>>>>>>>>>>>> closing handle ........."+
            // handle);
            sendClosedEvent(connection);
            // log.debug(">>>>>>>>>>>>>>> close event sent");
            // connection.getSession().close();
            // log.debug(">>>>>>>>>>>>>>> jcr session closed");
        }
        this.connection = null;
    }

    public void log(String message) {
        log(message, null);
    }

    public void log(String message, Throwable exception) {
        if (out != null) {
            out.println(message);
            if (exception != null) {
                exception.printStackTrace(out);
            }
        }
    }

    public ConnectionImpl getHandle() {
        return connection;
    }

    public synchronized void initializeHandle(ConnectionRequestInfo cri)
            throws ResourceException {
        if (!isHandleValid()) {
            // log.debug(">>>>>>>>>>>>>> handle "+handle+
            // " is not valid creating a new one ...........");
            connection = new ConnectionImpl(this, openSession(cri));
            // /xar.setXAResource(connection.getXAResource());
            // log.debug(">>>>>>>>>>>>>> new handle is "+handle+
            // " ...........");
        }
    }

    public boolean isHandleValid() {
        return connection != null; // && connection.isLive();
    }

    /*
     * ----- Event management -----
     */

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

    private void sendEvent(int type, Object handle, Exception cause) {
        ConnectionEvent event = new ConnectionEvent(this, type, cause);
        if (handle != null) {
            event.setConnectionHandle(handle);
        }
        sendEvent(event);
    }

    public void sendClosedEvent(ConnectionImpl handle) {
        sendEvent(ConnectionEvent.CONNECTION_CLOSED, handle, null);
    }

    public void sendTxStartedEvent(ConnectionImpl handle) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED, handle, null);
    }

    public void sendTxCommittedEvent(ConnectionImpl handle) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, handle, null);
    }

    public void sendTxRolledbackEvent(ConnectionImpl handle) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, handle, null);
    }

    public void sendErrorEvent(ConnectionImpl handle, Exception cause) {
        sendEvent(ConnectionEvent.CONNECTION_ERROR_OCCURRED, handle, cause);
    }

}
