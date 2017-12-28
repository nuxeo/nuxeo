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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.commons.io.output.WriterOutputStream;

import com.fasterxml.jackson.core.JsonGenerator;

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
