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

import javax.resource.cci.Connection;
import javax.resource.spi.ConnectionRequestInfo;

import org.nuxeo.ecm.core.storage.sql.ConnectionSpecImpl;

/**
 * The connection request info encapsulates the credentials and other info
 * passed when creating a {@link Connection}.
 * <p>
 * Its implementation is entirely specific to the resource adapter.
 *
 * @author Florent Guillaume
 */
public class ConnectionRequestInfoImpl implements ConnectionRequestInfo {

    protected final ConnectionSpecImpl connectionSpec;

    public ConnectionRequestInfoImpl() {
        connectionSpec = new ConnectionSpecImpl(null);
    }

    public ConnectionRequestInfoImpl(ConnectionSpecImpl connectionSpec) {
        this.connectionSpec = connectionSpec;
    }

    public ConnectionSpecImpl getConnectionSpec() {
        return connectionSpec;
    }

    @Override
    public int hashCode() {
        return connectionSpec == null ? 0 : connectionSpec.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof ConnectionRequestInfoImpl) {
            return equals((ConnectionRequestInfoImpl) other);
        }
        return false;
    }

    private boolean equals(ConnectionRequestInfoImpl other) {
        if (connectionSpec == null) {
            return other.connectionSpec == null;
        } else {
            return connectionSpec.equals(other.connectionSpec);
        }
    }

}
