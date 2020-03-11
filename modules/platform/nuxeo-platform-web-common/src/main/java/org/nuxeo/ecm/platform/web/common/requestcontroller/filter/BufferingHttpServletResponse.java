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
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.nuxeo.ecm.core.io.download.BufferingServletOutputStream;

/**
 * Buffers the response until {@link #stopBuffering()} is called.
 * <p>
 * This allows a container to commit a transaction before the body is written to the client.
 */
public class BufferingHttpServletResponse extends HttpServletResponseWrapper {

    protected BufferingServletOutputStream bufferingOutputStream;

    /**
     * A {@link HttpServletResponse} wrapper that buffers all data until {@link #stopBuffering()} is called.
     * <p>
     * {@link #stopBuffering()} <b>MUST</b> be called in a {@code finally} statement in order for resources to be closed
     * properly.
     */
    public BufferingHttpServletResponse(HttpServletResponse response) throws IOException {
        super(response);
        bufferingOutputStream = new BufferingServletOutputStream(response.getOutputStream());
    }

    @Override
    public BufferingServletOutputStream getOutputStream() throws IOException {
        return bufferingOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return bufferingOutputStream.getWriter();
    }

    /**
     * Stops buffering and sends any buffered data to the response's output stream.
     */
    public void stopBuffering() throws IOException {
        bufferingOutputStream.stopBuffering();
    }



    /**
     * Don't flush if we are still buffering.
     */
    @Override
    public void flushBuffer() throws IOException {
        bufferingOutputStream.flush();
    }

}
