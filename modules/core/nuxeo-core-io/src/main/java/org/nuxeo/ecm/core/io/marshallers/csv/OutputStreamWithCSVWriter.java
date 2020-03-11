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
 *     Funsho David
 */

package org.nuxeo.ecm.core.io.marshallers.csv;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.AppendableOutputStream;

/**
 * Wrapper for {@link CSVPrinter}.
 *
 * @since 10.3
 */
public class OutputStreamWithCSVWriter extends OutputStream {

    protected AppendableOutputStream<Appendable> out;

    protected CSVPrinter csvPrinter;

    public OutputStreamWithCSVWriter() {
        super();
    }

    public OutputStreamWithCSVWriter(CSVPrinter csvPrinter) {
        super();
        setCSVPrinter(csvPrinter);
    }

    public void setCSVPrinter(CSVPrinter csvPrinter) {
        this.csvPrinter = csvPrinter;
        out = new AppendableOutputStream<>(csvPrinter.getOut());
    }

    @Override
    public void write(int i) throws IOException {
        out.write(i);
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

    /**
     * Returns the wrapped CSV printer.
     *
     * @return the CSV printer
     */
    public CSVPrinter getCsvPrinter() {
        return csvPrinter;
    }

    @Override
    public String toString() {
        return out.getAppendable().toString();
    }
}
