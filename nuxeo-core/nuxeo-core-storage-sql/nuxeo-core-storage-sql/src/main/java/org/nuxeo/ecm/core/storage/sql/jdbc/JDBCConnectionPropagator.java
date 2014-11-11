/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.util.ArrayList;
import java.util.List;

/**
 * Knows all the {@link JDBCConnection}s in use by a backend, so that they can
 * notify each other when there's a connection failure.
 */
public class JDBCConnectionPropagator {

    public final List<JDBCConnection> connections; // used synchronized

    public JDBCConnectionPropagator() {
        connections = new ArrayList<JDBCConnection>();
    }

    public synchronized void addConnection(JDBCConnection connection) {
        connections.add(connection);
    }

    public synchronized void removeConnection(JDBCConnection connection) {
        connections.remove(connection);
    }

    /**
     * Notifies all connection that they must check their validity.
     *
     * @param exclude a connection to exclude, the one from which the request
     *            originates
     */
    public synchronized void checkConnectionValid(JDBCConnection exclude) {
        for (JDBCConnection c : connections) {
            if (c != exclude) {
                c.checkConnectionValid = true;
            }
        }
    }

}
