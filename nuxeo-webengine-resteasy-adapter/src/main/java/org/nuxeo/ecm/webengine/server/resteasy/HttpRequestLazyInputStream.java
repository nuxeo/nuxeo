/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.server.resteasy;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

public class HttpRequestLazyInputStream extends InputStream {

    protected InputStream requestStream;

    protected final HttpServletRequest httpRequest;

    public HttpRequestLazyInputStream(HttpServletRequest request) {
        httpRequest = request;
    }

    protected InputStream getStream() throws IOException {
        if (requestStream == null) {
            requestStream = httpRequest.getInputStream();
        }
        return requestStream;
    }

    @Override
    public int read() throws IOException {
        return getStream().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return getStream().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return getStream().read(b, off, len);
    }

}
