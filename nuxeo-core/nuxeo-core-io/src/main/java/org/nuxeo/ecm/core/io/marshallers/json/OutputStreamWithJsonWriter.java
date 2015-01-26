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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.commons.io.output.WriterOutputStream;
import org.codehaus.jackson.JsonGenerator;

/**
 * This {@link OutputStream} is a technical wrapper for {@link JsonGenerator}. It's used to broadcast a
 * {@link JsonGenerator} between marshallers.
 * <p>
 * take a look at {@link AbstractJsonWriter#getGenerator(OutputStream, boolean)} to understand the mechanism.
 * </p>
 *
 * @since 7.2
 */
public class OutputStreamWithJsonWriter extends OutputStream {

    private OutputStream out;

    private JsonGenerator jsonGenerator;

    public OutputStreamWithJsonWriter(JsonGenerator jsonGenerator) {
        super();
        this.jsonGenerator = jsonGenerator;
        Object outputTarget = jsonGenerator.getOutputTarget();
        if (outputTarget instanceof OutputStream) {
            out = (OutputStream) outputTarget;
        } else if (outputTarget instanceof Writer) {
            out = new WriterOutputStream((Writer) outputTarget);
        }
    }

    public JsonGenerator getJsonGenerator() {
        return jsonGenerator;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

}
