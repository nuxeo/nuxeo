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

import java.io.Serializable;
import java.util.List;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;

/**
 * A connection is a handle to the underlying storage. It is returned by the
 * {@link ConnectionFactory} to application code.
 * <p>
 * The actual link to the underlying storage ({@link Session}) is provided by
 * the {@link ManagedConnection} which created this {@link Connection}.
 *
 * @author Florent Guillaume
 */
public class ConnectionImpl implements Session {

    private static final Log log = LogFactory.getLog(ConnectionImpl.class);

    private ManagedConnectionImpl managedConnection;

    private SessionImpl session;

    public ConnectionImpl(ManagedConnectionImpl managedConnection,
            SessionImpl session) {
        this.managedConnection = managedConnection;
        this.session = session;
    }

    /*
     * ----- -----
     */

    /**
     * Called by {@link ManagedConnectionImpl#associateConnection}
     */
    protected ManagedConnectionImpl getManagedConnection() {
        return managedConnection;
    }

    /**
     * Called by {@link ManagedConnectionImpl#associateConnection}
     */
    protected void setManagedConnection(
            ManagedConnectionImpl managedConnection, SessionImpl session) {
        this.managedConnection = managedConnection;
        this.session = session;
    }

    /*
     * ----- javax.resource.cci.Connection -----
     */

    public void close() throws ResourceException {
        managedConnection.close();
    }

    public Interaction createInteraction() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    public ConnectionMetaData getMetaData() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- org.nuxeo.ecm.core.storage.sql.Session -----
     */

    public boolean isLive() {
        return session.isLive();
    }

    public Model getModel() {
        return session.getModel();
    }

    public void save() throws StorageException {
        session.save();
    }

    public Node getRootNode() throws StorageException {
        return session.getRootNode();
    }

    public Node getNodeById(Serializable id) throws StorageException {
        return session.getNodeById(id);
    }

    public Node getNodeByPath(String path, Node node) throws StorageException {
        return session.getNodeByPath(path, node);
    }

    public boolean hasChildNode(Node parent, String name, boolean complexProp)
            throws StorageException {
        return session.hasChildNode(parent, name, complexProp);
    }

    public Node getChildNode(Node parent, String name, boolean complexProp)
            throws StorageException {
        return session.getChildNode(parent, name, complexProp);
    }

    public boolean hasChildren(Node parent, boolean complexProp)
            throws StorageException {
        return session.hasChildren(parent, complexProp);
    }

    public List<Node> getChildren(Node parent, boolean complexProp, String name)
            throws StorageException {
        return session.getChildren(parent, complexProp, name);
    }

    public Node addChildNode(Node parent, String name, String typeName,
            boolean complex) throws StorageException {
        return session.addChildNode(parent, name, typeName, complex);
    }

    public void removeNode(Node node) throws StorageException {
        session.removeNode(node);
    }

    public Node getParentNode(Node node) throws StorageException {
        return session.getParentNode(node);
    }

    public String getPath(Node node) throws StorageException {
        return session.getPath(node);
    }

}
