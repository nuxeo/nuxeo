/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.io.IOUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * A serializable input stream.
 * <p>
 * Note: The stream is closed after the object is serialized.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SerializableInputStream extends InputStream implements Serializable {

    public static final int IN_MEM_LIMIT = 1024 * 64;

    private static final long serialVersionUID = -2816387281878881614L;

    private transient File file;

    private transient InputStream in;

    public SerializableInputStream(InputStream in) {
        this.in = in;
    }

    public SerializableInputStream(byte[] content) {
        in = new ByteArrayInputStream(content);
    }

    public SerializableInputStream(String content) {
        this(content.getBytes());
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    public File getTempFile() {
        return file;
    }

    public InputStream reopen() throws IOException {
        if (!canReopen()) {
            throw new IOException("Cannot reopen non persistent stream");
        }
        return new BufferedInputStream(new FileInputStream(file));
    }

    public boolean canReopen() {
        return file != null && file.isFile();
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        // always perform the default de-serialization first
        in.defaultReadObject();
        // create a temp file where we will put the blob content
        file = Framework.createTempFile("SerializableIS-", ".tmp");
        Framework.trackFile(file, file);
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buffer = new byte[IN_MEM_LIMIT];
            int read;
            int bytes = in.readInt();
            while (bytes > -1 && (read = in.read(buffer, 0, bytes)) != -1) {
                out.write(buffer, 0, read);
                bytes -= read;
                if (bytes == 0) {
                    bytes = in.readInt();
                }
            }
        } finally {
            if (out != null) {
                out.close();
            }
            if (file.isFile()) {
                this.in = new BufferedInputStream(new FileInputStream(file));
            }
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // write content
        if (in == null) {
            return;
        }
        try {
            int read;
            byte[] buf = new byte[IN_MEM_LIMIT];
            while ((read = in.read(buf)) != -1) {
                // next follows a chunk of 'read' bytes
                out.writeInt(read);
                out.write(buf, 0, read);
            }
            out.writeInt(-1); // EOF
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        IOUtils.closeQuietly(in);
        if (file != null) {
            file.delete();
        }
        super.finalize();
    }

}
