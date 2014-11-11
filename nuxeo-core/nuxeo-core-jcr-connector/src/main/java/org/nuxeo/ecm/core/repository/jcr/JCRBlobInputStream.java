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
package org.nuxeo.ecm.core.repository.jcr;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This input stream is used to wrap the input stream that jackrabbit return for blob values so that
 * we can reset it at a higher level - to be able to read it more than once without refetching the blob from the
 * repository.
 * <p>
 * This fix issue NXP-2072
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * @see JCRBlob#getStream()
 * @see NXP-2072
 *
 */
public class JCRBlobInputStream extends InputStream {

    private static final Log log = LogFactory.getLog(JCRBlobInputStream.class);

    private InputStream  in;
    private JCRBlob blob;

    public JCRBlobInputStream(JCRBlob blob) {
        this.blob = blob;
        in = null;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (in != null) {
            close();
        }
        in = blob.getStream();
    }

    @Override
    public void close() throws IOException {
        getIn().close();
        in = null;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return getIn().read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return getIn().read(b);
    }

    @Override
    public int read() throws IOException {
        return getIn().read();
    }

    @Override
    public int available() throws IOException {
        return getIn().available();
    }

    @Override
    public boolean equals(Object obj) {
        return getIn().equals(obj);
    }

    @Override
    public int hashCode() {
        return getIn().hashCode();
    }

    @Override
    public synchronized void mark(int readlimit) {
        getIn().mark(readlimit);
    }

    @Override
    public long skip(long n) throws IOException {
        return getIn().skip(n);
    }

    @Override
    public boolean markSupported() {
        return getIn().markSupported();
    }

    @Override
    public String toString() {
        return getIn().toString();
    }

    public final InputStream getIn() {
        if (in == null) {
            try {
                in = blob._getStream();
            } catch (IOException e) {
                log.error("Failed to open input stream from jcr blob", e);
            }
        }
        return in;
    }

}
