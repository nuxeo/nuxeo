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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.io.download;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.runtime.api.Framework;

/**
 * A {@link ServletOutputStream} that buffers everything until {@link #stopBuffering()} is called.
 * <p>
 * There may only be one such instance per thread.
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

    protected PrintWriter writer;

    protected ByteArrayOutputStream memory;

    protected OutputStream file;

    protected File tmp;

    /**
     * A {@link ServletOutputStream} wrapper that buffers everything until {@link #stopBuffering()} is called.
     * <p>
     * {@link #stopBuffering()} <b>MUST</b> be called in a {@code finally} statement in order for resources to be closed
     * properly.
     *
     * @param outputStream the underlying output stream
     */
    public BufferingServletOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(this));
        }
        return writer;
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
        tmp = Framework.createTempFile("nxout", null);
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
     * This implementation does nothing, we still want to keep buffering and not flush.
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
     * This implementation does nothing, we still want to keep the buffer until {@link #stopBuffering()} time.
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
     * Writes any buffered data to the underlying {@link OutputStream} and from now on don't buffer anymore.
     */
    public void stopBuffering() throws IOException {
        if (streaming) {
            return;
        }
        if (writer != null) {
            writer.flush(); // don't close, streaming needs it
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
        boolean clientAbort = false;
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
                    } catch (IOException e) {
                        if (DownloadHelper.isClientAbortError(e)) {
                            DownloadHelper.logClientAbort(e);
                            clientAbort = true;
                        } else {
                            throw e;
                        }
                    } finally {
                        in.close();
                    }
                } finally {
                    tmp.delete();
                }
            }
        } catch (IOException e) {
            if (DownloadHelper.isClientAbortError(e)) {
                if (!clientAbort) {
                    DownloadHelper.logClientAbort(e);
                    clientAbort = true;
                }
            } else {
                throw e;
            }
        } finally {
            memory = null;
            file = null;
            tmp = null;
            try {
                if (needsFlush) {
                    outputStream.flush();
                }
            } catch (IOException e) {
                if (DownloadHelper.isClientAbortError(e)) {
                    if (!clientAbort) {
                        DownloadHelper.logClientAbort(e);
                    }
                } else {
                    throw e;
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
