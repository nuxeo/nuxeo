/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.automation.jaxrs.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.internet.SharedInputStream;

public class SharedFileInputStream extends InputStream implements SharedInputStream {

    protected final InputStream in;

    protected final SharedFileInputStream parent;

    protected final File file;

    protected final long length;

    protected final long start;

    protected long current;

    protected long marked;

    public SharedFileInputStream(File file) throws IOException {
        this.in = new BufferedInputStream(new FileInputStream(file));
        this.parent = null;
        this.file = file;
        this.start = 0;
        this.length = file.length();
        this.in.skip(start);
    }

    protected SharedFileInputStream(SharedFileInputStream parent, long start, long len) throws IOException {
        this.in = new BufferedInputStream(new FileInputStream(parent.file));
        this.parent = parent;
        this.file = parent.file;
        this.start = start;
        this.length = len;
        this.in.skip(start);
    }

    @Override
    public long getPosition() {
        return this.current;
    }

    @Override
    public SharedFileInputStream newStream(long start, long end) {
        try {
            long length;
            if (end == -1L) {
                length = this.length - start;
            } else {
                length = end - start;
            }
            return new SharedFileInputStream(this, this.start + start, length);
        } catch (IOException localIOException) {
            throw new IllegalStateException("unable to create shared stream: " + localIOException);
        }
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int len) throws IOException {
        int i = 0;
        if (len == 0)
            return 0;
        while (i < len) {
            int j = read();
            if (j < 0)
                break;
            buffer[(offset + i)] = (byte) j;
            ++i;
        }
        if (i == 0)
            return -1;
        return i;
    }

    @Override
    public int read() throws IOException {
        if (this.current == this.length)
            return -1;
        this.current += 1L;
        return this.in.read();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public long skip(long len) throws IOException {
        for (int count = 0; count < len; ++count) {
            if (read() < 0)
                return count;
        }
        return len;
    }

    @Override
    public void mark(int limit) {
        this.marked = this.current;
        this.in.mark(limit);
    }

    @Override
    public void reset() throws IOException {
        current = marked;
        in.reset();
    }

    public SharedFileInputStream getRoot() {
        if (parent != null)
            return parent.getRoot();
        return this;
    }
}
