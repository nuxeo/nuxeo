/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.io.marshallers.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;

/**
 * @since 10.3
 */
@Supports(AbstractCSVWriter.TEXT_CSV)
public abstract class AbstractCSVWriter<T> implements Writer<T> {

    public static final String SEPARATOR = ",";

    public static final String TEXT_CSV = "text/csv";

    public static final MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");

    public static final Charset UTF8 = Charset.forName("utf-8");

    private final Class<T> klass;

    protected AbstractCSVWriter(Class<T> klass) {
        this.klass = klass;
    }

    @Override
    public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        return TEXT_CSV_TYPE.equals(mediatype) && klass.isAssignableFrom(clazz);
    }

    protected void writeWithSeparator(OutputStream out, String value) throws IOException {
        write(out, value);
        writeSeparator(out);
    }

    protected void write(OutputStream out, String value) throws IOException {
        out.write((value == null ? "null" : value).getBytes(UTF8));
    }

    protected void writeSeparator(OutputStream out) throws IOException {
        write(out, SEPARATOR);
    }

}
