/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;

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
