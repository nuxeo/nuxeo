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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl.blob;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.StreamManager;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * TODO: describe what this blob is and does.
 * <p>
 * This blob has the limitation you will find in all stream blobs.
 * <p>
 * Once the stream was acquired there is no more guarantee that another
 * getStream() call will return a valid stream.
 * <p>
 * This could be fixed by using a temp file to store the stream.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LazyBlob extends DefaultStreamBlob implements Serializable {

    private static final long serialVersionUID = -6138173743804682559L;

    private static final Log log = LogFactory.getLog(LazyBlob.class);

    public static final InputStream EMPTY_INPUT_STREAM = new EmptyInputStream();

    private static final Random RANDOM = new Random();

    private static final File TMP_DIR;

    // the session id
    private String sid;

    // the repository name
    private final String repositoryName;

    // the content property path
    private final String dataKey;

    private transient InputStream in;

    private transient File file;

    private final long length;

    static {
        TMP_DIR = new File(Framework.getRuntime().getHome(), "tmp/blobs");
        TMP_DIR.mkdirs();
    }

    public LazyBlob(InputStream in, String encoding, String mimeType,
            String sid, String dataKey, String repositoryName, String filename,
            String digest, long length) {
        this.in = in == null ? EMPTY_INPUT_STREAM : in;
        this.encoding = encoding;
        this.mimeType = mimeType;
        this.sid = sid;
        this.dataKey = dataKey;
        this.repositoryName = repositoryName;
        this.filename = filename;
        this.digest = digest;
        this.length = length;
    }

    public LazyBlob(InputStream in, String encoding, String mimeType,
            String sid, String dataKey, String repositoryName) {
        this(in, encoding, mimeType, sid, dataKey, repositoryName, null, null, -1);
    }

    @Override
    public long getLength() {
        return length;
    }

    public String getSid() {
        return sid;
    }

    public String getDataKey() {
        return dataKey;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void reset() {
        FileUtils.close(in);
        in = null;
    }

    /**
     * Returns a Nuxeo Core session.
     * <p>
     * XXX: complete this sentence.
     * core instance and grab the content back. Thus this method will try out to
     * create a new session if the sid is not accurate anymore. If the session
     * has been disconnect it won't be possible to reconnect on a XXX(???).
     *
     * @return a Nuxeo Core session instance.
     * @throws ClientException
     */
    private CoreSession getClient() throws ClientException {

        CoreSession client = null;

        if (sid != null) {
            client = CoreInstance.getInstance().getSession(sid);
        }

        if (client == null) {
            if (repositoryName != null) {
                // We Will use the null value as a flag to know if whether or
                // not we will have to close the new opened connection.
                sid = null;
                Map<String, Serializable> ctx = new HashMap<String, Serializable>();
                client = CoreInstance.getInstance().open(repositoryName, ctx);
            } else {
                throw new ClientException(
                        "Cannot reconnect to a Nuxeo core instance... No repository name provided...");
            }
        }

        return client;
    }

    public InputStream getStream() throws IOException {

        // Get the client.
        CoreSession client;
        try {
            client = getClient();
        } catch (ClientException ce) {
            throw new IOException(ce.getMessage());
        }

        if (in == null) {
            // this should be a remote invocation
            StreamManager sm = Framework.getLocalService(StreamManager.class);
            String uri = null;
            try {
                if (sm == null) {
                    throw new IOException("No Streaming service was registered");
                }
                uri = client.getStreamURI(dataKey);
                StreamSource src = sm.getStream(uri);
                file = new File(TMP_DIR, Long.toHexString(RANDOM.nextLong()));
                file.deleteOnExit();
                src.copyTo(file); // persist the content
                in = new FileInputStream(file);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                log.error(e);
                throw new IOException("Failed to load lazy content for: "
                        + dataKey);
            } finally {
                // destroy the remote blob and close any opened stream on the server
                if (uri != null) {
                    sm.removeStream(uri); // destroy the remote stream
                }
            }
        } else if (file != null) {
            in = new FileInputStream(file);
        }

        // Close the session because it means we opened a new one.
        if (sid == null) {
            CoreInstance.getInstance().close(client);
        }

        return in;
    }

    public boolean isPersistent() {
        return false;
    }

    public Blob persist() throws IOException {
        // NXP-3190: fetch it first in case it's not initialized
        if (in == null) {
            getStream();
        }
        // optimize -> when the underlying stream is an SerializableINputStream
        // this can be optimized by reusing the temp file
        // of the underlying stream
        return new FileBlob(in, mimeType, encoding);
    }

    @Override
    protected void finalize() {
        if (file != null) {
            FileUtils.close(in);
            file.delete();
        }
    }

    protected static boolean equalValues(Object first, Object second) {
        if (first == null) {
            return second == null;
        } else {
            return first.equals(second);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LazyBlob)) {
            return false;
        }
        LazyBlob other = (LazyBlob) obj;
        boolean encodingEquals = equalValues(encoding, other.encoding);
        if (!encodingEquals) {
            return false;
        }
        boolean mimetypeEquals = equalValues(mimeType, other.mimeType);
        if (!mimetypeEquals) {
            return false;
        }
        boolean sidEquals = equalValues(sid, other.sid);
        if (!sidEquals) {
            return false;
        }
        boolean dataKeyEquals = equalValues(dataKey, other.dataKey);
        if (!dataKeyEquals) {
            return false;
        }
        return equalValues(repositoryName, other.repositoryName);
    }

    @Override
    public int hashCode() {
        int result = sid != null ? sid.hashCode() : 0;
        result = 31 * result + (repositoryName != null ? repositoryName.hashCode() : 0);
        result = 31 * result + (dataKey != null ? dataKey.hashCode() : 0);
        result = 31 * result + (encoding != null ? encoding.hashCode() : 0);
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        return result;
    }

    // private void readObject(ObjectInputStream in)
    // throws ClassNotFoundException, IOException {
    // in.defaultReadObject();
    // }
    //
    // private void writeObject(ObjectOutputStream out) throws IOException {
    // out.defaultWriteObject();
    // }

    public static class EmptyInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            return -1;
        }

    }

}
