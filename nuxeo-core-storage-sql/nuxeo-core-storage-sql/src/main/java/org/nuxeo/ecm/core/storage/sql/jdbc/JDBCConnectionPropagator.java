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
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * Knows all the {@link JDBCConnection}s in use by a backend, so that they can
 * notify each other when there's a connection failure.
 */
public class JDBCConnectionPropagator {

    public final List<JDBCConnection> connections; // used synchronized

    public ClusterNodeHandler clusterNodeHandler;

    public JDBCConnectionPropagator() {
        connections = new ArrayList<JDBCConnection>();
    }

    public synchronized void addConnection(JDBCConnection connection) {
        connections.add(connection);
    }

    public void setClusterNodeHandler(ClusterNodeHandler clusterNodeHandler) {
        this.clusterNodeHandler = clusterNodeHandler;
    }

    public synchronized void removeConnection(JDBCConnection connection) {
        connections.remove(connection);
    }

    /**
     * Notifies all connection that they must check their validity.
     *
     * @param connection the connection that was reset
     */
    public synchronized void connectionWasReset(JDBCConnection connection)
            throws StorageException {
        if (clusterNodeHandler != null
                && clusterNodeHandler.getConnection() == connection) {
            clusterNodeHandler.connectionWasReset();
        }
        for (JDBCConnection c : connections) {
            if (c != connection) {
                c.connectionWasReset();
            }
        }
    }

}
