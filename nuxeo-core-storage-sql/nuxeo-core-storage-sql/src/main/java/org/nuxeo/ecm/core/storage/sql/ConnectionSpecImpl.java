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

package org.nuxeo.ecm.core.storage.sql;

import javax.resource.cci.ConnectionSpec;

import org.nuxeo.ecm.core.storage.Credentials;

/**
 * This represents the parameters (mostly, credentials) passed by the
 * application to the {@link Repository} when requesting the creation of a
 * {@link Session}.
 *
 * @author Florent Guillaume
 */
public class ConnectionSpecImpl implements ConnectionSpec {

    private final Credentials credentials;

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
