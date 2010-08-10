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

package org.nuxeo.runtime.detection;

import java.net.InetAddress;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Peer<T> {

    /** the identity of that peer */
    public final String identity;

    /** timestamp of the last heart beat */
    public long lastHeartBeat;

    /** this peer address */
    public final InetAddress addr;

    /** this peer port **/
    public final int port;

    /** This may be used by higher layer to attach some data to a peer entry **/
    public T data;


    public Peer(InetAddress addr, int port, String identity) {
        this.addr = addr;
        this.port = port;
        this.identity = identity;
        lastHeartBeat = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return identity + " [" + addr + ':' + port + ']';
    }

}
