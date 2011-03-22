/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ZipEntrySource extends AbstractStreamSource {

    protected final String file;
    protected final String entry;

    public ZipEntrySource(String file, String entry) {
        this.file = file;
        this.entry = entry;
    }

    public ZipEntrySource(File file, String entry) {
        this.file = file.getAbsolutePath();
        this.entry = entry;
    }

    @Override
    public InputStream getStream() throws IOException {
        ZipFile zf = new ZipFile(file);
        ZipEntry zentry = zf.getEntry(entry);
        if (zentry == null) {
            return null;
        }
        return new ZipEntryInputStream(zf, zf.getInputStream(zentry));
    }

    @Override
    public boolean canReopen() {
        return true;
    }

    @Override
    public long getLength() throws IOException {
        return -1;
    }

    /**
     * An input stream which will close the zip file as well when closed.
     *
     * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
     */
    class ZipEntryInputStream extends InputStream {

        final InputStream in;
        final ZipFile zip;

        ZipEntryInputStream(ZipFile zip, InputStream in) {
            this.in = in;
            this.zip = zip;
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public void close() throws IOException {
            try {
                in.close();
            } finally {
                zip.close();
            }
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

        @Override
        public long skip(long n) throws IOException {
            return in.skip(n);
        }
    }

}
