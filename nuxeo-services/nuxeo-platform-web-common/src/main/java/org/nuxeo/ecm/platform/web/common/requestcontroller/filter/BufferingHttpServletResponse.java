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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Buffers the response until {@link #stopBuffering()} is called.
 * <p>
 * This allows a container to commit a transaction before the body is written to
 * the client.
 */
public class BufferingHttpServletResponse extends HttpServletResponseWrapper {

    protected BufferingServletOutputStream bufferingOutputStream;

    /**
     * A {@link HttpServletResponse} wrapper that buffers all data until
     * {@link #stopBuffering()} is called.
     * <p>
     * {@link #stopBuffering()} <b>MUST</b> be called in a {@code finally}
     * statement in order for resources to be closed properly.
     */
    public BufferingHttpServletResponse(HttpServletResponse response)
            throws IOException {
        super(response);
        bufferingOutputStream = new BufferingServletOutputStream(
                response.getOutputStream());
    }

    @Override
    public BufferingServletOutputStream getOutputStream() throws IOException {
        return bufferingOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(new OutputStreamWriter(bufferingOutputStream));
    }

    /**
     * Stops buffering and sends any buffered data to the response's output
     * stream.
     */
    public void stopBuffering() throws IOException {
        bufferingOutputStream.stopBuffering();
    }

}
