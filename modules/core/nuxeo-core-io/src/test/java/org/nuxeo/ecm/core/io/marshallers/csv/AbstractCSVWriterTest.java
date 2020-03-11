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

import static org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriter.TEXT_CSV_TYPE;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreIOFeature.class)
public abstract class AbstractCSVWriterTest<WriterClass extends Writer<MarshalledType>, MarshalledType> {

    public static abstract class Local<WriterClass extends Writer<MarshalledType>, MarshalledType>
            extends AbstractCSVWriterTest<WriterClass, MarshalledType> {

        public Local(Class<WriterClass> writerClass, Class<?> marshalledClass, Type marshalledGenericType) {
            super(writerClass, marshalledClass, marshalledGenericType);
        }

        public Local(Class<WriterClass> writerClass, Class<?> marshalledClass) {
            super(writerClass, marshalledClass);
        }

    }

    @Deploy("org.nuxeo.runtime.stream")
    public static abstract class External<WriterClass extends Writer<MarshalledType>, MarshalledType>
            extends AbstractCSVWriterTest<WriterClass, MarshalledType> {

        public External(Class<WriterClass> writerClass, Class<?> marshalledClass, Type marshalledGenericType) {
            super(writerClass, marshalledClass, marshalledGenericType);
        }

        public External(Class<WriterClass> writerClass, Class<?> marshalledClass) {
            super(writerClass, marshalledClass);
        }

    }

    @Inject
    protected MarshallerRegistry registry;

    private Class<WriterClass> writerClass;

    private Class<?> marshalledClass;

    private Type marshalledGenericType;

    public AbstractCSVWriterTest(Class<WriterClass> writerClass, Class<?> marshalledClass) {
        this(writerClass, marshalledClass, marshalledClass);
    }

    public AbstractCSVWriterTest(Class<WriterClass> writerClass, Class<?> marshalledClass,
            Type marshalledGenericType) {
        super();
        this.writerClass = writerClass;
        this.marshalledClass = marshalledClass;
        this.marshalledGenericType = marshalledGenericType;
    }

    public WriterClass getInstance() {
        return registry.getInstance(RenderingContext.CtxBuilder.get(), writerClass);
    }

    public WriterClass getInstance(RenderingContext ctx) {
        return registry.getInstance(ctx, writerClass);
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
