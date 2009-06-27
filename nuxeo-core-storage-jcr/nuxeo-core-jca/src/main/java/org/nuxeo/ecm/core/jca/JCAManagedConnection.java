/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.core.jca;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Map;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Session;

/**
 * This class implements the managed connection for
 * this resource adapter.
 * <p>
 * These sources are based on the JackRabbit JCA implementation (http://jackrabbit.apache.org/).
 * <p>
 * Notes: Based on debugging JCA on JBoss:
 * the jca managed connection is
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class JCAManagedConnection
        implements ManagedConnection, ManagedConnectionMetaData {

    private static final Log log = LogFactory.getLog(JCAManagedConnection.class);

    /**
     * Managed connection factory.
     */
    private final JCAManagedConnectionFactory mcf;

    /**
     * Connection request info.
     */
    private final JCAConnectionRequestInfo cri;


    /**
     * Listeners.
     */
    private final ListenerList listeners;

    /**
     * Handles.
     */
    //private final LinkedList<JCAConnection> handles;

    // Application handle for the repository connection
    private JCAConnection handle;

    private final ManagedXAResource xar;

    /**
     * Log writer.
     */
    private PrintWriter logWriter;

    /**
     * Construct the managed connection.
     */
    public JCAManagedConnection(JCAManagedConnectionFactory mcf, JCAConnectionRequestInfo cri) {
        this.mcf = mcf;
        this.cri = cri;
        xar = new ManagedXAResource();
        listeners = new ListenerList();
    }

    /**
     * Create a new session.
     */
    private Session openSession(JCAConnectionRequestInfo cri)
            throws ResourceException {
        Map<String, Serializable> ctx = cri.getSessionContext();

        try {
            Session session = mcf.getRepository().getSession(ctx);
            log("Created session for repository " + mcf.getRepository().getName());
            return session;
        } catch (DocumentException e) {
            log("Failed to create workspace session", e);
            throw new ResourceException(e);
        }
    }

    /**
     * Return the managed connection factory.
     */
    public JCAManagedConnectionFactory getManagedConnectionFactory() {
        return mcf;
    }

    /**
     * Return the connection request info.
     */
    public JCAConnectionRequestInfo getConnectionRequestInfo() {
        return cri;
    }

    /**
     * Gets the log writer.
     */
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    /**
     * Sets the log writer.
     */
    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    /**
     * Creates a new connection handle for the underlying physical
     * connection represented by the ManagedConnection instance.
     */
    public synchronized Object getConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        //System.out.println("JCA --------------------------------------------------- getConnection");

        initializeHandle((JCAConnectionRequestInfo) cri);
        log.debug(">>>>>>>> returning: " + handle);
        return handle;
    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     */
    public void destroy() {
        //System.out.println("JCA >>>>>>>>>>>>>>>>>>>>>> calling destroy");
        cleanup();
    }

    /**
     * Application server calls this method to force any cleanup on
     * the ManagedConnection instance.
     */
    public synchronized void cleanup() {
        //System.out.println("JCA >>>>>>>>>>>>>>>>>>>>>> calling cleanup");
    }


    /**
     * Used by the container to change the association of an
     * application-level connection handle with a ManagedConnection instance.
     */
    public synchronized void associateConnection(Object connection)
            throws ResourceException {
        //System.out.println("JCA >>>>>>>>>>>>>>>>>>>>>> calling associateConnection");
        JCAConnection handle = (JCAConnection) connection;
        if (handle.getManagedConnection() != this) {
            try {
                closeHandle(this.handle);
                this.handle = handle;
                this.handle.getManagedConnection().handle = null;
                this.handle.setManagedConnection(this);
            } catch (DocumentException e) {
                throw new ResourceException("Failed to close handle " + this.handle.getSession(), e);
            }
        }
    }

    /**
     * Returns an javax.transaction.xa.XAresource instance.
     */
    public XAResource getXAResource()
            throws ResourceException {
        //System.out.println("XARESource ---------------------------------------------------");
        initializeHandle(cri);
        return xar;
    }

    /**
     * Returns an javax.resource.spi.LocalTransaction instance.
     */
    public LocalTransaction getLocalTransaction() {
        throw new UnsupportedOperationException("Local transaction is not supported");
    }

    /**
     * Gets the metadata information for this connection's underlying
     * EIS resource manager instance.
     */
    public ManagedConnectionMetaData getMetaData() {
        return this;
    }

    /**
     * Closes the handle.
     */
    public void closeHandle(JCAConnection handle) throws DocumentException {
        //System.out.println(">>>>>>>>>>>>>>> trying to close handle ........."+handle);
        if (isHandleValid() && this.handle == handle) {
            //System.out.println(">>>>>>>>>>>>>>> closing handle ........."+handle);
            sendClosedEvent(handle);
            //System.out.println(">>>>>>>>>>>>>>> close event sent");
            handle.getSession().close();
            //System.out.println(">>>>>>>>>>>>>>> jcr session closed");
        }
    }

    /**
     * Returns the product name.
     */
    public String getEISProductName() {
        return "NXCore Repository"; // TODO i18n
    }

    /**
     * Return the product version.
     */
    // TODO: put product info inside a properties file
    public String getEISProductVersion() {
        return "0.0.1";
    }

    /**
     * Returns number of max connections.
     */
    public int getMaxConnections() {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns the user name.
     */
    public String getUserName()
            throws ResourceException {
        initializeHandle(cri);
        return (String) handle.getSessionContext().get(Session.USER_NAME);
    }

    /**
     * Logs a message.
     */
    public void log(String message) {
        log(message, null);
    }

    /**
     * Logs a message.
     */
    public void log(String message, Throwable exception) {
        if (logWriter != null) {
            logWriter.println(message);

            if (exception != null) {
                exception.printStackTrace(logWriter);
            }
        }
    }

    /**
     * Adds a listener.
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener.
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Send event.
     */
    private void sendEvent(ConnectionEvent event) {
        Object[] listenersArray = listeners.getListenersCopy();
        for (Object object : listenersArray) {
            ConnectionEventListener listener = (ConnectionEventListener) object;

            switch (event.getId()) {
                case ConnectionEvent.CONNECTION_CLOSED:
                    listener.connectionClosed(event);
                    break;
                case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                    listener.connectionErrorOccurred(event);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                    listener.localTransactionCommitted(event);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                    listener.localTransactionRolledback(event);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                    listener.localTransactionStarted(event);
                    break;
            }
        }
    }

    /**
     * Send event.
     */
    private void sendEvent(int type, Object handle, Exception cause) {
        ConnectionEvent event = new ConnectionEvent(this, type, cause);
        if (handle != null) {
            event.setConnectionHandle(handle);
        }

        sendEvent(event);
    }

    /**
     * Send connection closed event.
     */
    public void sendClosedEvent(JCAConnection handle) {
        sendEvent(ConnectionEvent.CONNECTION_CLOSED, handle, null);
    }

    /**
     * Send connection error event.
     */
    public void sendErrorEvent(JCAConnection handle, Exception cause) {
        sendEvent(ConnectionEvent.CONNECTION_ERROR_OCCURRED, handle, cause);
    }

    /**
     * Send transaction committed event.
     */
    public void sendTxCommittedEvent(JCAConnection handle) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, handle, null);
    }

    /**
     * Send transaction rolledback event.
     */
    public void sendTxRolledbackEvent(JCAConnection handle) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, handle, null);
    }

    /**
     * Send transaction started event.
     */
    public void sendTxStartedEvent(JCAConnection handle) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED, handle, null);
    }


    /**
     * @return Returns the handle.
     */
    public JCAConnection getHandle() {
        return handle;
    }


    public synchronized void initializeHandle(JCAConnectionRequestInfo cri) throws ResourceException {
        if (!isHandleValid()) {
            //System.out.println(">>>>>>>>>>>>>> handle "+handle+" is not valid creating a new one ...........");
            handle = new JCAConnection(this, openSession(cri));
            xar.setXAResource(handle.getXAResource());
            //System.out.println(">>>>>>>>>>>>>> new handle is "+handle+" ...........");
        }
    }

    public boolean isHandleValid() {
        return handle != null && handle.isLive();
    }

}
