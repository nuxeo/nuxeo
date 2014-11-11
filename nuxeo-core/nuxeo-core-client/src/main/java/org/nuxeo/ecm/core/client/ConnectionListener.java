/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.client;

/**
 * Listen to connection events
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ConnectionListener {

    /**
     * The client connected to a server.
     *
     * @param client the client
     */
    void connected(NuxeoClient client);

    /**
     * The client disconnected.
     *
     * @param client the client
     */
    void disconnected(NuxeoClient client);

    /**
     * The client authentication failed against the remote server.
     *
     * @param client the client
     */
    boolean authenticationFailed(NuxeoClient client);

}
