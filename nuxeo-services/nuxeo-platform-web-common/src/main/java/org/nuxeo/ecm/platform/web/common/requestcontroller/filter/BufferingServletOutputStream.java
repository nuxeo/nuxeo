/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link ServletOutputStream} that buffers everything until
 * {@link #stopBuffering()} is called.
 * <p>
 * Buffering is done first in memory, then on disk if the size exceeds a limit.
 */
public class BufferingServletOutputStream extends ServletOutputStream {

    private static final Log log = LogFactory.getLog(BufferingServletOutputStream.class);

    /** Initial memory buffer size. */
    public static final int INITIAL = 4 * 1024; // 4 KB

    /** Maximum memory buffer size, after this a file is used. */
    public static final int MAX = 64 * 1024; // 64 KB

    /** Used for 0-length writes. */
    private final static OutputStream EMPTY = new ByteArrayOutputStream(0);

    /** Have we stopped buffering to pass writes directly to the output stream. */
    protected boolean streaming;

    protected boolean needsFlush;

    protected boolean needsClose;

    protected final OutputStream outputStream;

    protected ByteArrayOutputStream memory;

    protected OutputStream file;

    protected File tmp;

    /**
     * A {@link ServletOutputStream} wrapper that buffers everything until
     * {@link #stopBuffering()} is called.
     * <p>
     * {@link #stopBuffering()} <b>MUST</b> be called in a {@code finally}
     * statement in order for resources to be closed properly.
     *
     * @param outputStream the underlying output stream
     */
    public BufferingServletOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * Finds the proper output stream where we can write {@code len} bytes.
     */
    protected OutputStream getOutputStream(int len) throws IOException {
        if (streaming) {
            return outputStream;
        }
        if (len == 0) {
            return EMPTY;
        }
        if (file != null) {
            // already to file
            return file;
        }
        int total;
        if (memory == null) {
            // no buffer yet
            if (len <= MAX) {
                memory = new ByteArrayOutputStream(Math.max(INITIAL, len));
                return memory;
            }
            total = len;
        } else {
            total = memory.size() + len;
        }
        if (total <= MAX) {
            return memory;
        } else {
            // switch to a file
            createTempFile();
            file = new BufferedOutputStream(new FileOutputStream(tmp));
            if (memory != null) {
                memory.writeTo(file);
                memory = null;
            }
            return file;
        }
    }

    protected void createTempFile() throws IOException {
        tmp = File.createTempFile("nxout", null);
    }

    @Override
    public void write(int b) throws IOException {
        getOutputStream(1).write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        getOutputStream(b.length).write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        getOutputStream(len).write(b, off, len);
    }

    /**
     * This implementation does nothing, we still want to keep buffering and not
     * flush.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        if (streaming) {
            outputStream.flush();
        } else {
            needsFlush = true;
        }
    }

    /**
     * This implementation does nothing, we still want to keep the buffer until
     * {@link #stopBuffering()} time.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (streaming) {
            outputStream.close();
        } else {
            needsClose = true;
        }

    }

    /**
     * Writes any buffered data to the underlying {@link OutputStream} and from
     * now on don't buffer anymore.
     */
    public void stopBuffering() throws IOException {
        if (streaming) {
            return;
        }
        streaming = true;
        if (log.isDebugEnabled()) {
            long len;
            if (memory != null) {
                len = memory.size();
            } else if (file != null) {
                len = tmp.length();
            } else {
                len = 0;
            }
            log.debug("buffered bytes: " + len);
        }
        try {
            if (memory != null) {
                memory.writeTo(outputStream);
            } else if (file != null) {
                try {
                    try {
                        file.flush();
                    } finally {
                        file.close();
                    }
                    FileInputStream in = new FileInputStream(tmp);
                    try {
                        IOUtils.copy(in, outputStream);
                    } finally {
                        in.close();
                    }
                } finally {
                    tmp.delete();
                }
            }
        } finally {
            memory = null;
            file = null;
            tmp = null;
            try {
                if (needsFlush) {
                    outputStream.flush();
                }
            } finally {
                if (needsClose) {
                    outputStream.close();
                }
            }
        }
    }

    /**
     * Tells the given {@link OutputStream} to stop buffering (if it was).
     */
    public static void stopBuffering(OutputStream out) throws IOException {
        if (out instanceof BufferingServletOutputStream) {
            ((BufferingServletOutputStream) out).stopBuffering();
        }
    }

}
