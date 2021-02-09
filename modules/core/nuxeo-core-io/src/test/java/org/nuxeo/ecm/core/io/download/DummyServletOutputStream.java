/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.download;

import org.nuxeo.common.function.ThrowableConsumer;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @since 11.5
 */
public class DummyServletOutputStream extends ServletOutputStream {

    protected final ThrowableConsumer<Integer, IOException> writer;

    public DummyServletOutputStream(OutputStream out) {
        this(out::write);
    }

    public DummyServletOutputStream(ThrowableConsumer<Integer, IOException> writer) {
        this.writer = writer;
    }

    @Override
    public void write(int b) throws IOException {
        writer.accept(b);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
    }
}
