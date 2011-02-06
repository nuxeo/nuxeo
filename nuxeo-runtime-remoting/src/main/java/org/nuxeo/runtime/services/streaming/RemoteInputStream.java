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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Read an input stream from a remote machine using the streaming service
 * <p>
 * Warn that this implementation is not doing buffering so it is recommended to wrap it
 * inside a {@link BufferedInputStream}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RemoteInputStream extends InputStream {

    private final StreamManagerClient streamMgr;
    private final String uri;

    private long sid = 0;
    private boolean eof = false;
    //TODO
    private byte[] buffer = new byte[4096];
    private int start = 0;
    private int end = 0;
    private int preferredSize = 64*1024;


    public RemoteInputStream(StreamManagerClient streamMgr, String uri) {
        this.streamMgr = streamMgr;
        this.uri = uri;
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException(
                "method not implemented -> use read(byte[]) instead");
//        byte[] bytes = new byte[1];
//        int r = read(bytes, 0, 1);
//        return r > -1 ? bytes[0] : -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (eof) {
            return -1;
        }
        int available = end - start;
        if (available > 0) { // we have some data in buffer
            int needBytes = len - available;
            if (needBytes > 0) { // we need to fetch more data - at least 'needBytes' bytes
                // fill out buf with available bytes
                System.arraycopy(buffer, start, b, off, available);
                // re-fill the buffer with data from network
                int ava = fillBytes(needBytes); // fetch at least "needBytes" bytes
                if (eof) {
                    return available; // >>> tested ok
                }
                System.arraycopy(buffer, start, b, off + available, ava);
                start += ava;
                return available + ava;
            } else { // we have all needed bytes in the buffer
                if (buffer == null || buffer.length == 0) {
                    // XXX : fix problem when the blob has to traverse several JVMs
                    // reset session
                    sid = 0;
                    fillBytes(len);
                }
                System.arraycopy(buffer, start, b, off, len);
                start += len;
                return len;
            }
        } else { // no more data in buffer
            int ava = fillBytes(len); // fetch at leat len bytes
            if (eof) {
                return -1;
            }
            System.arraycopy(buffer, start, b, off, ava);
            start += ava;
            return ava;
        }
    }

    /**
     * Refills the buffer with data from network, resets offset
     * and resizes buffer if needed.
     */
    protected int fillBytes(int count) throws IOException {
        if (eof) {
            int available = end - start;
            if (available < 1) {
                return -1;
            }
            return count < available ? count : available;
        }
        int initialCount = count;
        try {
            if (sid == 0) {
                DownloadInfo di = streamMgr.getServer().createDownloadSession(uri);
                preferredSize = streamMgr.getBufferSize(di.preferredSize);
                sid = di.sid;
            }
            if (count < preferredSize) {
                count = preferredSize;
            }
            // get count bytes from remote
            buffer = streamMgr.getServer().downloadBytes(sid, count);
            start = 0;
            if (buffer == null) {
                eof = true;
                return -1;
                //close();
            }
            end = buffer.length;
            return buffer.length > initialCount ? initialCount : buffer.length;
        } catch (IOException e) {
            // TODO: do something here, or remove the try/catch construct.
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (sid != 0) {
            streamMgr.getServer().closeDownloadSession(sid);
            sid = 0;
            eof = false;
        }
    }

}
