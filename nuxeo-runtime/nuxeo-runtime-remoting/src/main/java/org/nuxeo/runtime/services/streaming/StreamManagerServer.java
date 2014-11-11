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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.remoting.transporter.TransporterServer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StreamManagerServer implements StreamingServer, StreamManager {

    private static final Log log = LogFactory.getLog(StreamManagerServer.class);

    private static long uploadCount = 0;
    private static long downloadCount = 0;

    // the registered streams
    protected final Map<String, StreamSource> streams = new Hashtable<String, StreamSource>();

    // the resources being uploaded
    protected final Map<String, UploadSession> uploads = new HashMap<String, UploadSession>();

    // the resources being downloaded
    protected final Map<Long, DownloadSession> downloads = new HashMap<Long, DownloadSession>();

    protected final String locatorURI;

    protected final File tmpDir;

    protected final TransporterServer transporterServer;


    public StreamManagerServer(String host, int port, File tmpDir) throws Exception {
        this(new InvokerLocator("socket", host, port, null, null), tmpDir);
    }

    public StreamManagerServer(String serverLocator, File tmpDir) throws Exception {
        this(new InvokerLocator(serverLocator), tmpDir);
    }

    public StreamManagerServer(InvokerLocator locator, File tmpDir) throws Exception {
        this.tmpDir = tmpDir;
        tmpDir.mkdirs(); // make sure the tmp dir exists
        locatorURI = locator.getLocatorURI();
        transporterServer = TransporterServer.createTransporterServer(
                locator, this, StreamingServer.class.getName());
    }

    public StreamManagerServer(TransporterServer transporterServer, File tmpDir) throws Exception {
        this.tmpDir = tmpDir;
        tmpDir.mkdirs(); // make sure the tmp dir exists
        locatorURI = transporterServer.getLocatorURI();
        this.transporterServer = null;
        transporterServer.addHandler(this, StreamingServer.class.getName());
    }

    @Override
    public void start() throws Exception {
        if (transporterServer != null) {
            transporterServer.start();
        }
    }

    @Override
    public void stop() throws Exception {
        if (transporterServer != null) {
            transporterServer.stop();
        }
    }


    @Override
    public String addStream(StreamSource src) throws IOException {
        long up;
        synchronized (uploads) {
            up = incrementUploads();
        }
        String uri = locatorURI + "/stream#" + up;
        addResource(uri, src);
        return uri;
    }

    public void addResource(String uri, StreamSource src) {
        streams.put(uri, src);
        //System.out.println("Added resource: "+uri);
    }

    @Override
    public StreamSource getStream(String uri) {
        return streams.get(uri);
    }

    @Override
    public boolean hasStream(String uri) {
        return streams.containsKey(uri);
    }

    @Override
    public void removeStream(String uri) {
        StreamSource src = streams.remove(uri);
        if (src != null) {
            src.destroy();
        }
    }

    private static long incrementUploads() {
        return uploadCount = (uploadCount == Long.MAX_VALUE ? 0 : uploadCount + 1);
    }

    private static long incrementDownloads() {
        return downloadCount = (downloadCount == Long.MAX_VALUE ? 0 : downloadCount + 1);
    }

    protected final String getNextFileName() {
        String fileName;
        synchronized (uploads) {
            fileName = "uploaded_resource#" + uploadCount;
            if (uploadCount == Long.MAX_VALUE) {
                uploadCount = 0;
            } else {
                uploadCount++;
            }
        }
        return fileName;
    }


    public class UploadSession {
        public String uri;
        public int uploaded;
        public File file;
        public FileOutputStream out;

        public UploadSession() throws IOException {
            String name = String.valueOf(incrementUploads());
            uri = locatorURI + "/stream#" + name;
            file = new File(tmpDir, name);
            out = new FileOutputStream(file);
            uploaded = 0;
        }
    }

    public class DownloadSession {
        public final long id;
        public int downloaded;
        public StreamSource src;
        public InputStream in;

        public DownloadSession(StreamSource src) throws IOException {
            id = incrementDownloads();
            this.src = src;
            in = src.getStream();
            downloaded = 0;
        }
    }

    @Override
    public DownloadInfo createDownloadSession(String uri) throws IOException {
        StreamSource src = streams.get(uri);
        if (src == null) {
            throw new NoSuchElementException("Not resource with uri " + uri);
        }
        DownloadSession ds;
        synchronized (downloads) {
            ds = new DownloadSession(src);
            downloads.put(ds.id, ds);
        }
        //System.out.println("Started download session for "+uri);
        return new DownloadInfo(ds.id, ds.in.available());
    }

    @Override
    public byte[] downloadBytes(long sid, int size) throws IOException {
        DownloadSession ds;
         synchronized (downloads) {
             ds = downloads.get(sid);
        }
        if (ds == null) {
            throw new IllegalArgumentException("No such download session: " + sid);
        }
        byte[] bytes = new byte[size];
        //System.out.println("Using a byte buffer size of "+ba.length);
        int length = ds.in.read(bytes, 0, size);
        if (length > -1) {
            ds.downloaded += length;
            if (length < bytes.length) {
                byte[] tmp = new byte[length];
                System.arraycopy(bytes, 0, tmp, 0, length);
                bytes = tmp;
            }
            return bytes;
        }
        return null;
    }

    @Override
    public void closeDownloadSession(long sid) throws IOException {
        DownloadSession ds;
        synchronized (downloads) {
            ds = downloads.remove(sid);
        }
        if (ds != null) {
            //System.out.println("Closed download session "+sid+". Downloaded "+ds.downloaded+" bytes");
            ds.src = null;
            ds.in.close();
            ds.in = null;
        } else {
            throw new IllegalArgumentException("No such download session: " + sid);
        }
    }

    @Override
    public String createUploadSession() throws IOException {
        synchronized (uploads) {
            UploadSession us = new UploadSession();
            uploads.put(us.uri, us);
            return us.uri;
        }
    }

    @Override
    public void uploadBytes(String uri, byte[] bytes) throws IOException {
        UploadSession us;
        synchronized (uploads) {
            us = uploads.get(uri);
        }
        if (us == null) {
            throw new IllegalArgumentException("No such upload session " + uri);
        }
        if (bytes == null) {
            doCloseUpload(us);
        } else {
            us.out.write(bytes);
        }
    }

    @Override
    public void closeUploadSession(String uri) throws IOException {
        UploadSession us;
        synchronized (uploads) {
            us = uploads.remove(uri);
        }
        if (us != null) {
            doCloseUpload(us);
        } else {
            throw new IllegalArgumentException("No such upload session: " + uri);
        }
    }

    protected void doCloseUpload(UploadSession us) throws IOException {
        us.out.close();
        addResource(us.uri, new UploadedStreamSource(us.file));
        //System.out.println("Closed upload session for "+us.uri+". Uploaded "+us.uploaded+" bytes");
        us.file = null;
        us.uri = null;
    }

    public static void main(String[] args) {

        try {
            StreamManagerServer server = new StreamManagerServer("localhost", 3234,
                    new File("/tmp/uploads"));
            server.start();
            //System.out.println("stream server started");
            while (true) { Thread.sleep(1000000); }
        } catch (Exception e) {
            log.error(e, e);
        }
    }

}
