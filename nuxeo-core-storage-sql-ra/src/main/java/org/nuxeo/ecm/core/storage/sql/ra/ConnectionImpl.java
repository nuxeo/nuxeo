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

/**
 * The connection wraps a connection to the underlying storage. It is returned
 * by the {@link ConnectionFactory} to application code.
 *
 * @author Florent Guillaume
 */
public class ConnectionImpl implements Session {

    private static final Log log = LogFactory.getLog(ConnectionImpl.class);

    private final Session session;

    private ManagedConnectionImpl managedConnection;

    public ConnectionImpl(ManagedConnectionImpl mc, Session session) {
        this.managedConnection = mc;
        this.session = session;
    }

    /*
     * ----- javax.resource.cci.Connection -----
     */

    public void close() throws ResourceException {
        // delegate this to the managed connection
        managedConnection.closeHandle(this);
    }

    public Interaction createInteraction() throws ResourceException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public ConnectionMetaData getMetaData() throws ResourceException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public ResultSetInfo getResultSetInfo() throws ResourceException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    /*
     * ----- Session -----
     */

    public void save() throws StorageException {
        session.save();
    }

    public Node addNode(Node parent, String name, String typeName)
            throws StorageException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public Model getModel() {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public Node getNode(Node parent, String name) throws StorageException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public Node getRootNode() throws StorageException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public void removeNode(Node node) throws StorageException {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    /*
     * ----- -----
     */

    public ManagedConnectionImpl getManagedConnection() {
        return managedConnection;
    }

    public void setManagedConnection(ManagedConnectionImpl mc) {
        this.managedConnection = mc;
    }

    public void dispose() {
        // delegate this to the managed connection
        try {
            managedConnection.closeHandle(this);
        } catch (ResourceException e) {
            log.error("error closing session");
        }
    }

    @Override
    public String toString() {
        return System.identityHashCode(this) + ":" + session.toString();
    }

}
