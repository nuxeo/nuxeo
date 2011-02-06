/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.services.streaming;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.remoting.transporter.TransporterClient;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StreamManagerClient implements StreamManager {

    private static final Log log = LogFactory.getLog(StreamManagerClient.class);

    protected final StreamingServer server;

    protected int minBufSize;
    protected int maxBufSize;


    public StreamManagerClient(String serverLocator) throws Exception {
        this(serverLocator, 1024*8, 1024*1024*8);
    }

    public StreamManagerClient(String serverLocator, int minBufSize, int maxBufSize) throws Exception {
        server = (StreamingServer) TransporterClient.createTransporterClient(
                serverLocator, StreamingServer.class);
        this.minBufSize = minBufSize;
        this.maxBufSize = maxBufSize;
    }


    @Override
    public void start() throws Exception {
        // do nothing
    }

    @Override
    public void stop() {
        TransporterClient.destroyTransporterClient(server);
    }

    @Override
    public synchronized  String addStream(StreamSource src) throws IOException {
        InputStream in = src.getStream();
        String uri = server.createUploadSession();
        byte[] bytes = new byte[getBufferSize(in.available())];
        //System.out.println("Using a byte buffer size of "+bytes.length);
        try {
            while (true) {
                int ret = in.read(bytes);
                if (ret == -1) {
                    break;
                } else {
                    byte[] bytesToUpload;
                    if (ret < bytes.length) {
                        bytesToUpload = new byte[ret];
                        System.arraycopy(bytes, 0, bytesToUpload, 0, ret);
                    } else {
                        bytesToUpload = bytes;
                    }
                    server.uploadBytes(uri, bytesToUpload);
                }
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                log.error(e, e);
            }
            server.closeUploadSession(uri);
        }
        return uri;
    }


    protected int getBufferSize(int available) {
        if (available > 0) {
            if (available <= minBufSize) {
                return minBufSize;
            } else if (available > maxBufSize) {
                return maxBufSize;
            } else {
                return available;
            }
        } else {
            return 1024*64; //64K
        }
    }

    @Override
    public synchronized StreamSource getStream(String uri) throws IOException {
        RemoteInputStream in = new RemoteInputStream(this, uri);
        return new InputStreamSource(in);
    }

    @Override
    public synchronized boolean hasStream(String uri) {
        return server.hasStream(uri);
    }

    @Override
    public synchronized void removeStream(String uri) {
        server.removeStream(uri);
    }

    public void setMaxBufferSize(int maxBufSize) {
        this.maxBufSize = maxBufSize;
    }

    public void setMinBufferSize(int minBufSize) {
        this.minBufSize = minBufSize;
    }

    public StreamingServer getServer() {
        return server;
    }

    public static void main(String[] args) {
        try {
            StreamManagerClient client = new StreamManagerClient("socket://localhost:3233");
            client.start();
            System.out.println("stream client started");
//            StreamSource src = new URLSource(
//                    StreamManagerClient.class.getResource("StreamManagerClient.class"));
            StreamSource src = new FileSource(new File("/home/bstefanescu/test"));

//            byte[] orig = src.getBytes();

            double s = System.currentTimeMillis();
            String uri = client.addStream(src);
            double e = System.currentTimeMillis();
            System.out.println(">>> upload took " + ((e - s) / 1000) + " sec.");

            // retrierve remote stream
            src = client.getStream(uri);
//            byte[] retrieved = src.getBytes();
//            boolean ok = Arrays.equals(orig, retrieved);
//            System.out.println("Retrieved stream "+(ok ? " OK" : " ERROR"));

            // remove the stream from the server
            client.removeStream(uri);
            client.stop();

            System.out.println("Done.");
        } catch (Exception e) {
            log.error(e, e);
        }
    }

}
