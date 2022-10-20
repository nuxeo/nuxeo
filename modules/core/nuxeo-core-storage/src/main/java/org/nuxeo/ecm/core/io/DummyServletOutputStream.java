/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.io;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.nuxeo.common.function.ThrowableConsumer;

/**
 * A dummy {@link ServletOutputStream} for tests.
 *
 * @since 2023.0
 */
public class DummyServletOutputStream extends ServletOutputStream {

    protected final ThrowableConsumer<Integer, IOException> writer;

    /**
     * @param out the {@link OutputStream outputStream} used to delegate the writing
     */
    public DummyServletOutputStream(OutputStream out) {
        this(requireNonNull(out)::write);
    }

    /**
     * @param writer the {@link ThrowableConsumer consumer} used to delegate the writing
     */
    public DummyServletOutputStream(ThrowableConsumer<Integer, IOException> writer) {
        this.writer = requireNonNull(writer);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        // nothing
    }

    @Override
    public void write(int b) throws IOException {
        writer.accept(b);
    }
}
