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

package org.nuxeo.ecm.core.storage.sql;

import javax.resource.cci.ConnectionSpec;

import org.nuxeo.ecm.core.storage.Credentials;

/**
 * This represents the parameteres (mostly, credentials) passed by the
 * application to the {@link Repository} when requesting the creation of a
 * {@link Session}.
 *
 * @author Florent Guillaume
 */
public class ConnectionSpecImpl implements ConnectionSpec {

    private Credentials credentials;

    public ConnectionSpecImpl(Credentials credentials) {
        this.credentials = credentials;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public int hashCode() {
        return credentials == null ? 0 : credentials.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ConnectionSpecImpl)) {
            return false;
        }
        return equals((ConnectionSpecImpl) o);
    }

    private boolean equals(ConnectionSpecImpl other) {
        if (credentials == null) {
            return other.credentials == null;
        } else {
            return credentials.equals(other.credentials);
        }
    }
}
