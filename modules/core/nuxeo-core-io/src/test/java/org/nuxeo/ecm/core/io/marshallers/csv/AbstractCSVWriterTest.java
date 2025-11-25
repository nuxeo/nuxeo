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

import static org.nuxeo.ecm.core.io.marshallers.NuxeoMediaType.TEXT_CSV_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import org.nuxeo.ecm.core.io.marshallers.AbstractWriterTest;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 10.3
 */
public abstract class AbstractCSVWriterTest<WriterClass extends Writer<MarshalledType>, MarshalledType>
        extends AbstractWriterTest<WriterClass, MarshalledType> {

    public static abstract class Local<WriterClass extends Writer<MarshalledType>, MarshalledType>
            extends AbstractCSVWriterTest<WriterClass, MarshalledType> {

        public Local() {
            super();
        }

        /** @deprecated since 2025.12, use {@link #Local()} instead */
        @Deprecated(since = "2025.12", forRemoval = true)
        public Local(Class<WriterClass> writerClass, Class<?> marshalledClass, Type marshalledGenericType) {
            super(writerClass, marshalledClass, marshalledGenericType);
        }

        /** @deprecated since 2025.12, use {@link #Local()} instead */
        @Deprecated(since = "2025.12", forRemoval = true)
        public Local(Class<WriterClass> writerClass, Class<?> marshalledClass) {
            super(writerClass, marshalledClass);
        }

    }

    @Deploy("org.nuxeo.runtime.stream")
    public static abstract class External<WriterClass extends Writer<MarshalledType>, MarshalledType>
            extends AbstractCSVWriterTest<WriterClass, MarshalledType> {

        public External() {
            super();
        }

        /** @deprecated since 2025.12, use {@link #External()} instead */
        @Deprecated(since = "2025.12", forRemoval = true)
        public External(Class<WriterClass> writerClass, Class<?> marshalledClass, Type marshalledGenericType) {
            super(writerClass, marshalledClass, marshalledGenericType);
        }

        /** @deprecated since 2025.12, use {@link #External()} instead */
        @Deprecated(since = "2025.12", forRemoval = true)
        public External(Class<WriterClass> writerClass, Class<?> marshalledClass) {
            super(writerClass, marshalledClass);
        }

    }

    public AbstractCSVWriterTest() {
        super();
    }

    /** @deprecated since 2025.12, use {@link #AbstractCSVWriterTest()} instead */
    @Deprecated(since = "2025.12", forRemoval = true)
    public AbstractCSVWriterTest(Class<WriterClass> writerClass, Class<?> marshalledClass) {
        this(writerClass, marshalledClass, marshalledClass);
    }

    /** @deprecated since 2025.12, use {@link #AbstractCSVWriterTest()} instead */
    @SuppressWarnings("removal") // weirdly detected as still used
    @Deprecated(since = "2025.12", forRemoval = true)
    public AbstractCSVWriterTest(Class<WriterClass> writerClass, Class<?> marshalledClass, Type marshalledGenericType) {
        super(writerClass, marshalledClass, marshalledGenericType);
    }

    public String asCsv(MarshalledType object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        getInstance().write(object, marshalledClass, marshalledGenericType, TEXT_CSV_TYPE, baos);
        return baos.toString();
    }

    public String asCsv(MarshalledType object, RenderingContext ctx) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        getInstance(ctx).write(object, marshalledClass, marshalledGenericType, TEXT_CSV_TYPE, baos);
        return baos.toString();
    }

    public CSVAssert csvAssert(MarshalledType object) throws IOException {
        return CSVAssert.on(asCsv(object));
    }

    public CSVAssert csvAssert(MarshalledType object, RenderingContext ctx) throws IOException {
        return CSVAssert.on(asCsv(object, ctx));
    }


}
