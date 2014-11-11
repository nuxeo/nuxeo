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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MulticastDetector<T> {

    private static final Log log = LogFactory.getLog(MulticastDetector.class);

    protected final InetAddress groupAddr;
    protected final int groupPort;

    protected final String identity;

    protected final Map<String, Peer<T>> peers;

    protected MulticastSocket socket;
    protected long heartBeatTimeout = 5000; // 10 sec

    private DetectionHandler handler;

    private HeartBeatDetection heartBeatDetection;
    private Timer heartBeatTimer;
    private Timer processingTimer;


    public MulticastDetector(String identity, InetAddress groupAddr,
            int groupPort) throws IOException {
        this.identity = identity;
        this.groupAddr = groupAddr;
        this.groupPort = groupPort;
        socket = new MulticastSocket(groupPort);
        peers = new HashMap<String, Peer<T>>();
    }

    public MulticastDetector(String identity) throws IOException {
        this(identity, "224.1.9.2", 4444);
    }

    public MulticastDetector(String identity, String groupAddr, int groupPort)
            throws IOException {
        this(identity, InetAddress.getByName(groupAddr), groupPort);
    }

    public void setDetectionHandler(DetectionHandler handler) {
        this.handler = handler;
    }

    public DetectionHandler getDetectionHandler() {
        return handler;
    }

    public MulticastSocket getSocket() {
        return socket;
    }

    public void setHeartBeatTimeout(long ms) {
        heartBeatTimeout = ms;
    }

    public long getHeartBeatTimeout() {
        return heartBeatTimeout;
    }

    public synchronized void start() {
        if (heartBeatDetection != null) {
            return;
        }
        try {
            socket.setSoTimeout((int) heartBeatTimeout); // give a chance to stop heart beat detector
            heartBeatDetection = new HeartBeatDetection();
            heartBeatDetection.start();
            processingTimer = new Timer("Nuxeo.Detection.Cleanup");
            processingTimer.schedule(new CleanupTask(), heartBeatTimeout, heartBeatTimeout);
            socket.joinGroup(groupAddr);
            heartBeatTimer = new Timer("Nuxeo.Detection.HeartBeat");
            heartBeatTimer.schedule(new HeartBeatTask(), 0, heartBeatTimeout);
        } catch (Throwable t) {
            stop();
        }
    }

    public synchronized void stop() {
        if (heartBeatDetection == null) {
            return;
        }
        heartBeatTimer.cancel();
        heartBeatTimer = null;
        heartBeatDetection.cancel();
        heartBeatDetection = null;
        processingTimer.cancel();
        processingTimer = null;
    }

    public String getIdentity() {
        return identity;
    }

    @SuppressWarnings("unchecked")
    public Peer<T>[] getPeers() {
        synchronized (peers) {
            return peers.values().toArray(new Peer[peers.size()]);
        }
    }

    private DatagramPacket createHeartBeat() {
        byte[] bytes = identity.getBytes();
        return new DatagramPacket(bytes, bytes.length, groupAddr, groupPort);
    }

    private String readHeartBeat(DatagramPacket p) {
        return new String(p.getData(), p.getOffset(), p.getLength());
    }

    protected void notifyPeerOnline(Peer<T> peer) {
        // async exec needed in that case otherwise
        // heartbeat detection may be significantly delayed and some heartbeats lost
        if (handler != null) {
            processingTimer.schedule(new NotifyTask(peer, true), 0);
        }
    }

    protected void notifyPeerOffline(Peer<T> peer) {
        // async exec not needed in that case
        if (handler != null) {
            handler.peerOffline(peer);
        }
    }

    class HeartBeatDetection extends Thread {
        private boolean running = false;
        private final Object runLock = new Object();

        HeartBeatDetection() {
            super("Nuxeo.HeartBeatDetection");
        }

        public void cancel() {
            synchronized (runLock) {
                running = false;
            }
            interrupt();
        }

        @Override
        public synchronized void start() {
            synchronized (runLock) {
                running = true;
            }
            super.start();
        }

        @Override
        public void run() {
            while (true) {
                //System.out.println(identity+": running heart beat listener");
                try {
                    synchronized (runLock) {
                        if (!running) {
                            break; // detector was stopped
                        }
                    }
                    byte[] bytes = new byte[4000];
                    DatagramPacket p = new DatagramPacket(bytes, bytes.length);
                    socket.receive(p); // block until a new message is received
                    String identity = readHeartBeat(p);
                    if (MulticastDetector.this.identity.equals(identity)) {
                        continue;
                    }
                    Peer<T> peer;
                    synchronized (peers) {
                        peer = peers.get(identity);
                        if (peer == null) { // create new peer
                            peer = new Peer<T>(p.getAddress(), p.getPort(), identity);
                            assert peer.addr.equals(p.getAddress());
                            assert peer.port == p.getPort();
                            peers.put(identity, peer);
                        } else { // update peer last heart beat
                            peer.lastHeartBeat = System.currentTimeMillis();
                            peer = null;
                        }
                    }
                    if (peer != null) { // new peer detected
                        System.out.println("Peer online: "+peer);
                        notifyPeerOnline(peer);
                    }
                } catch (SocketTimeoutException e) {
                    // socket timeout -> continue
                } catch (Throwable e) {
                    log.error(e, e);
                }
            }
        }
    }

    class HeartBeatTask extends TimerTask {
        @Override
        public void run() {
            //System.out.println(identity+": running heart beat task");
            try {
                socket.send(createHeartBeat());
            } catch (IOException e) {
                log.error(e, e);
            }
        }
    }

    class CleanupTask extends TimerTask {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            //System.out.println(identity+": running cleanup task");
            long tm = System.currentTimeMillis();
            // copy existing peers into an array to avoid race conditions
            Peer[] arPeers = getPeers();
            // remove expired peers
            for (Peer peer : arPeers) {
                if (tm - peer.lastHeartBeat > heartBeatTimeout*2) {
                    synchronized (peers) {
                        peers.remove(peer.identity);
                    }
                    System.out.println("Peer Offline: "+peer);
                    notifyPeerOffline(peer);
                    peer.data = null;
                }
            }
        }
    }

    class NotifyTask extends TimerTask {
        private final boolean online;
        private final Peer peer;

        NotifyTask(Peer<T> peer, boolean online) {
            this.peer = peer;
            this.online = online;
        }

        @Override
        public void run() {
            if (handler == null) {
                return;
            }
            if (online) {
                handler.peerOnline(peer);
            }
            else {
                handler.peerOffline(peer);
            }
        }
    }

}
