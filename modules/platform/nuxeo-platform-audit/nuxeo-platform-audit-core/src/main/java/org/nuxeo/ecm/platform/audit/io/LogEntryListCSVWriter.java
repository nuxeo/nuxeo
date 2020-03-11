/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.audit.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;
import org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriter;
import org.nuxeo.ecm.core.io.marshallers.csv.OutputStreamWithCSVWriter;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * @since 11.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class LogEntryListCSVWriter extends AbstractCSVWriter<List<LogEntry>> {

    public LogEntryListCSVWriter() {
        super();
    }

    @Override
    protected void write(List<LogEntry> entity, CSVPrinter printer) throws IOException {
        Writer<LogEntry> writer = registry.getWriter(ctx, LogEntry.class, TEXT_CSV_TYPE);
        for (LogEntry logEntry : entity) {
            try (OutputStream out = new OutputStreamWithCSVWriter(printer)) {
                writer.write(logEntry, LogEntry.class, LogEntry.class, TEXT_CSV_TYPE, out);
            }
            printer.println();
        }

    }

    @Override
    protected void writeHeader(List<LogEntry> entity, CSVPrinter printer) throws IOException {
        LogEntryCSVWriter.writeHeader(printer, ctx);
    }
}
