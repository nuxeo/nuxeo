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
import java.util.HashMap;
import java.util.Map;

import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionSpec;
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

    private final Map<String, Serializable> sessionContext;

    public ConnectionRequestInfoImpl() {
        sessionContext = new HashMap<String, Serializable>();
    }

    public ConnectionRequestInfoImpl(ConnectionSpec connectionSpec)
            throws IllegalArgumentException {
        this();
        if (connectionSpec instanceof ConnectionSpecImpl) {
            // TODO
        } else {
            throw new IllegalArgumentException("Invalid ConnectionSpec");
        }
    }

    public ConnectionRequestInfoImpl(Map<String, Serializable> context) {
        sessionContext = context;
    }

    public Map<String, Serializable> getSessionContext() {
        return sessionContext;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ConnectionRequestInfoImpl)) {
            return false;
        }
        ConnectionRequestInfoImpl other = (ConnectionRequestInfoImpl) o;
        if (sessionContext == null) {
            return other.sessionContext == null;
        } else {
            return sessionContext.equals(other.sessionContext);
        }
    }

    @Override
    public int hashCode() {
        return sessionContext == null ? 0 : sessionContext.hashCode();
    }

}
