/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jackson.JsonNode;

/**
 * This {@link InputStream} is a technical wrapper for {@link JsonNode}. It's used to broadcast a JsonNode between
 * marshallers.
 * <p>
 * take a look at {@link AbstractJsonReader#getNode(InputStream, boolean)} to understand the mechanism.
 * </p>
 *
 * @since 7.2
 */
public class InputStreamWithJsonNode extends InputStream {

    private JsonNode jn;

    private InputStream real = null;

    public InputStreamWithJsonNode(JsonNode jn) {
        super();
        this.jn = jn;
    }

    public JsonNode getJsonNode() {
        return jn;
    }

    public InputStream getRealInputStream() {
        if (real == null) {
            real = new ByteArrayInputStream(jn.toString().getBytes());
        }
        return real;
    }

    @Override
    public int read() throws IOException {
        return getRealInputStream().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return getRealInputStream().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return getRealInputStream().read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return getRealInputStream().skip(n);
    }

    @Override
    public String toString() {
        return getRealInputStream().toString();
    }

    @Override
    public int available() throws IOException {
        return getRealInputStream().available();
    }

    @Override
    public void close() throws IOException {
        if (real != null) {
            getRealInputStream().close();
        }
    }

    @Override
    public void mark(int readlimit) {
        getRealInputStream().mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        if (real != null) {
            getRealInputStream().reset();
        }
    }

    @Override
    public boolean markSupported() {
        return getRealInputStream().markSupported();
    }

}
