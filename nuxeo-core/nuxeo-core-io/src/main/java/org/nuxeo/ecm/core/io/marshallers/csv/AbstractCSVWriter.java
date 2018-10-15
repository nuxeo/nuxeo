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
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;
import org.nuxeo.ecm.core.schema.SchemaManager;

/**
 * Base class for CSV {@link Writer}.
 * </p>
 * It provides you a {@link CSVPrinter} to manage the marshalling.
 *
 * @param <T> The Java type to marshall as CSV.
 * @since 10.3
 */
@Supports(AbstractCSVWriter.TEXT_CSV)
public abstract class AbstractCSVWriter<T> implements Writer<T> {

    public static final String TEXT_CSV = "text/csv";

    public static final MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");

    /**
     * The current {@link RenderingContext}.
     */
    @Inject
    protected RenderingContext ctx;

    @Inject
    protected SchemaManager schemaManager;

    @Inject
    protected MarshallerRegistry registry;

    @Override
    public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        return TEXT_CSV_TYPE.equals(mediatype);
    }

    @Override
    public void write(T entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
            throws IOException {
        CSVPrinter printer = getCSVPrinter(entity, out);
        write(entity, printer);
        printer.flush();
    }

    protected abstract void write(T entity, CSVPrinter printer) throws IOException;

    protected abstract void writeHeader(T entity, CSVPrinter printer) throws  IOException;

    protected CSVPrinter getCSVPrinter(T entity, OutputStream out) throws IOException {
        if (out instanceof OutputStreamWithCSVWriter) {
            return ((OutputStreamWithCSVWriter) out).getCsvPrinter();
        }
        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out), CSVFormat.DEFAULT);
        writeHeader(entity, printer);
        return printer;
    }

    protected void printCalendar(Calendar value, CSVPrinter printer) throws IOException {
        if (value != null) {
            printer.print(((GregorianCalendar) value).toZonedDateTime());
        } else {
            printer.print(null);
        }
    }

}
