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

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;

import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Binary;
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

    private ManagedConnectionImpl managedConnection;

    private SessionImpl session;

    public ConnectionImpl(ManagedConnectionImpl managedConnection) {
        this.managedConnection = managedConnection;
    }

    /*
     * ----- callbacks -----
     */

    /**
     * Called by {@link ManagedConnectionImpl#associateConnection}.
     */
    protected ManagedConnectionImpl getManagedConnection() {
        return managedConnection;
    }

    /**
     * Called by {@link ManagedConnectionImpl#associateConnection}.
     */
    protected void setManagedConnection(ManagedConnectionImpl managedConnection) {
        this.managedConnection = managedConnection;
    }

    /**
     * Called by {@link ManagedConnectionImpl#addConnection}.
     */
    protected void associate(SessionImpl session) {
        this.session = session;
    }

    /**
     * Called by {@link ManagedConnectionImpl#removeConnection}.
     */
    protected void disassociate() {
        session = null;
    }

    /*
     * ----- javax.resource.cci.Connection -----
     */

    public void close() throws ResourceException {
        try {
            managedConnection.close(this);
        } finally {
            managedConnection = null;
        }
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

    private Session getSession() throws StorageException {
        if (session == null) {
            throw new StorageException("Cannot use closed connection handle");
        }
        return session;
    }

    public boolean isLive() {
        return session != null && session.isLive();
    }

    public Binary getBinary(InputStream in) throws StorageException {
        return getSession().getBinary(in);
    }

    public Model getModel() throws StorageException {
        return getSession().getModel();
    }

    public void save() throws StorageException {
        getSession().save();
    }

    public Node getRootNode() throws StorageException {
        return getSession().getRootNode();
    }

    public Node getNodeById(Serializable id) throws StorageException {
        return getSession().getNodeById(id);
    }

    public Node getNodeByPath(String path, Node node) throws StorageException {
        return getSession().getNodeByPath(path, node);
    }

    public boolean hasChildNode(Node parent, String name, boolean complexProp)
            throws StorageException {
        return getSession().hasChildNode(parent, name, complexProp);
    }

    public Node getChildNode(Node parent, String name, boolean complexProp)
            throws StorageException {
        return getSession().getChildNode(parent, name, complexProp);
    }

    public boolean hasChildren(Node parent, boolean complexProp)
            throws StorageException {
        return getSession().hasChildren(parent, complexProp);
    }

    public List<Node> getChildren(Node parent, String name, boolean complexProp)
            throws StorageException {
        return getSession().getChildren(parent, name, complexProp);
    }

    public Node addChildNode(Node parent, String name, Long pos,
            String typeName, boolean complexProp) throws StorageException {
        return getSession().addChildNode(parent, name, pos, typeName,
                complexProp);
    }

    public void removeNode(Node node) throws StorageException {
        getSession().removeNode(node);
    }

    public Node getParentNode(Node node) throws StorageException {
        return getSession().getParentNode(node);
    }

    public String getPath(Node node) throws StorageException {
        return getSession().getPath(node);
    }

    public Node move(Node source, Node parent, String name)
            throws StorageException {
        return getSession().move(source, parent, name);
    }

    public Node copy(Node source, Node parent, String name)
            throws StorageException {
        return getSession().copy(source, parent, name);
    }

    public Node checkIn(Node node, String label, String description)
            throws StorageException {
        return getSession().checkIn(node, label, description);
    }

    public void checkOut(Node node) throws StorageException {
        getSession().checkOut(node);
    }

    public void restoreByLabel(Node node, String label) throws StorageException {
        getSession().restoreByLabel(node, label);
    }

    public Node getVersionByLabel(Node node, String label)
            throws StorageException {
        return getSession().getVersionByLabel(node, label);
    }

    public List<Node> getVersions(Node node) throws StorageException {
        return getSession().getVersions(node);
    }

    public Node getLastVersion(Node node) throws StorageException {
        return getSession().getLastVersion(node);
    }

    public List<Node> getProxies(Node document, Node parent)
            throws StorageException {
        return getSession().getProxies(document, parent);
    }

    public Node addProxy(Serializable targetId, Serializable versionableId,
            Node parent, String name, Long pos) throws StorageException {
        return getSession().addProxy(targetId, versionableId, parent, name, pos);
    }

    public List<Serializable> query(SQLQuery query) throws StorageException {
        return getSession().query(query);
    }

}
