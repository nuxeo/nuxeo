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

import java.util.Hashtable;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PeerRegistry {

    private final Map<String, Peer> peers = new Hashtable<String, Peer>();

    public void addPeer(Peer peer) {
        peers.put(peer.identity, peer);
    }

    public void removePeer(String identity) {
        Peer peer = peers.remove(identity);
    }

    public void getPeer(String identity) {

    }

    public Peer[] getPeers() {
        //return peers.values().toArray(new Peer[peers.size()])
        return null;
    }

}
