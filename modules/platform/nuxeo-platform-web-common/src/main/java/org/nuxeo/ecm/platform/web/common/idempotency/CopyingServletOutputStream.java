/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.web.common.idempotency;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.apache.commons.io.output.DeferredFileOutputStream;

/**
 * Captures content written to the target stream.
 *
 * @since 11.5
 */
public class CopyingServletOutputStream extends ServletOutputStream {

    protected final ServletOutputStream output;

    protected final DeferredFileOutputStream copy;

    public CopyingServletOutputStream(ServletOutputStream output, DeferredFileOutputStream copy) {
        this.output = output;
        this.copy = copy;
    }

    @Override
    public void write(int b) throws IOException {
        output.write(b);
        copy.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        output.write(b);
        copy.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        output.write(b, off, len);
        copy.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        output.flush();
        copy.flush();
    }

    @Override
    public void close() throws IOException {
        output.close();
        copy.close();
    }

    @Override
    public boolean isReady() {
        return output.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        output.setWriteListener(writeListener);
    }

}
