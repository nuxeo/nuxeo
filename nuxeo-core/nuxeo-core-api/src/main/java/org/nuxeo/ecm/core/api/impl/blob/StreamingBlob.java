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

package org.nuxeo.ecm.core.api.impl.blob;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.ByteArraySource;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.InputStreamSource;
import org.nuxeo.runtime.services.streaming.StreamManager;
import org.nuxeo.runtime.services.streaming.StreamSource;
import org.nuxeo.runtime.services.streaming.StringSource;
import org.nuxeo.runtime.services.streaming.URLSource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StreamingBlob extends DefaultBlob implements Serializable {

    // use in memory buffers for data under 1MB
    public static final int MEM_MAX_LIMIT = 1024 * 1024;

    private static final long serialVersionUID = 8275917049427979525L;

    protected transient StreamSource src;

    protected transient File persistedTmpFile;

    public StreamingBlob(StreamSource src) {
        this(src, null, null);
    }

    public StreamingBlob(StreamSource src, String mimeType) {
        this(src, mimeType, null);
    }

    public StreamingBlob(StreamSource src, String mimeType, String encoding) {
        this(src, mimeType, encoding, null, null);
    }

    public StreamingBlob(StreamSource src, String mimeType, String encoding,
            String filename, String digest) {
        this.src = src;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.filename = filename;
        this.digest = digest;
    }

    public static StreamingBlob createFromStream(InputStream is) {
        return createFromStream(is, null);
    }

    public static StreamingBlob createFromStream(InputStream is, String mimeType) {
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        InputStreamSource src = new InputStreamSource(is);
        return new StreamingBlob(src, mimeType);
    }

    public static StreamingBlob createFromByteArray(byte[] bytes) {
        return createFromByteArray(bytes, null);
    }

    public static StreamingBlob createFromByteArray(byte[] bytes,
            String mimeType) {
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        ByteArraySource src = new ByteArraySource(bytes);
        return new StreamingBlob(src, mimeType);
    }

    public static StreamingBlob createFromString(String str) {
        return createFromString(str, null);
    }

    public static StreamingBlob createFromString(String str, String mimeType) {
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        StringSource src = new StringSource(str);
        return new StreamingBlob(src, mimeType);
    }

    public static StreamingBlob createFromFile(File file) {
        return createFromFile(file, null);
    }

    public static StreamingBlob createFromFile(File file, String mimeType) {
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        FileSource src = new FileSource(file);
        return new StreamingBlob(src, mimeType);
    }

    public static StreamingBlob createFromURL(URL url) {
        return createFromURL(url, null);
    }

    public static StreamingBlob createFromURL(URL url, String mimeType) {
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        URLSource src = new URLSource(url);
        return new StreamingBlob(src, mimeType);
    }

    public byte[] getByteArray() throws IOException {
        return src.getBytes();
    }

    public long getLength() {
        try {
            return src.getLength();
        } catch (IOException e) {
            return -1;
        }
    }

    public Reader getReader() throws IOException {
        return new InputStreamReader(getStream());
    }

    public InputStream getStream() throws IOException {
        return src.getStream();
    }

    public String getString() throws IOException {
        return src.getString();
    }

    public boolean isPersistent() {
        return src.canReopen();
    }

    /**
     * If the source is cannot be reopen, copy the binary content of the
     * original source to a temporary file and replace the source inplace by a
     * new FileSource instance pointing to the tmp file.
     *
     * return the current instance with a re-openable internal source
     */
    public Blob persist() throws IOException {
        if (!isPersistent()) {
            OutputStream out = null;
            InputStream in = null;
            try {
                persistedTmpFile = File.createTempFile(
                        "NXCore-persisted-StreamingBlob-", ".tmp");
                persistedTmpFile.deleteOnExit();
                in = src.getStream();
                out = new FileOutputStream(persistedTmpFile);
                copy(in, out);
                src = new FileSource(persistedTmpFile);
                Framework.trackFile(persistedTmpFile, this);
            } finally {
                FileUtils.close(in);
                FileUtils.close(out);
            }
        }
        return this;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        long len = src.getLength();
        StreamManager sm = Framework.getLocalService(StreamManager.class);
        if (sm == null || (len > -1 && len <= MEM_MAX_LIMIT)) {
            // use in memory buffers for less than 1MB of data or if no
            // streaming service is available
            byte[] bytes = src.getBytes();
            out.writeInt(bytes.length);
            out.write(bytes);
        } else {
            out.writeInt(-1); // marker how many bytes follows - if -1 => an
            // URI follow
            String uri = sm.addStream(src);
            out.writeUTF(uri);
        }
    }

    private void readObject(ObjectInputStream in)
            throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        int len = in.readInt();
        if (len == -1) {
            StreamManager sm = Framework.getLocalService(StreamManager.class);
            if (sm == null) {
                throw new IOException(
                        "There is no streaming service registered");
            }
            String uri = in.readUTF();
            src = sm.getStream(uri);
        } else {
            byte[] bytes = new byte[len];
            in.readFully(bytes);
            src = new ByteArraySource(bytes);
        }
    }

}
