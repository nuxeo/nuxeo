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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.marshallers.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Base class to test Json to Java marshallers.
 * <p>
 * To use it, write a default constructor and call the parent with the needed classes.
 *
 * <pre>
 * &#064;Test
 * public void test() throws Exception {
 *     String jsonToTransformAsObject = ...;
 *     MarshalledType json = asObject(jsonToTransformAsObject);
 *     ...
 * }
 * </pre>
 * </p>
 *
 * @param <ReaderClass> The marshaller type to test.
 * @param <MarshalledType> The marshalled type to test.
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreIOFeature.class)
public abstract class AbstractJsonReaderTest<ReaderClass extends Reader<MarshalledType>, MarshalledType> {

    public static abstract class Local<ReaderClass extends Reader<MarshalledType>, MarshalledType>
            extends AbstractJsonReaderTest<ReaderClass, MarshalledType> {

        public Local(Class<ReaderClass> readerClass, Class<?> marshalledClass, Type marshalledGenericType) {
            super(readerClass, marshalledClass, marshalledGenericType);
        }

        public Local(Class<ReaderClass> readerClass, Class<?> marshalledClass) {
            super(readerClass, marshalledClass);
        }

    }

    @Deploy("org.nuxeo.runtime.stream")
    public static abstract class External<ReaderClass extends Reader<MarshalledType>, MarshalledType>
            extends AbstractJsonReaderTest<ReaderClass, MarshalledType> {

        public External(Class<ReaderClass> readerClass, Class<?> marshalledClass, Type marshalledGenericType) {
            super(readerClass, marshalledClass, marshalledGenericType);
        }

        public External(Class<ReaderClass> readerClass, Class<?> marshalledClass) {
            super(readerClass, marshalledClass);
        }

    }

    @Inject
    protected MarshallerRegistry registry;

    private Class<ReaderClass> readerClass;

    private Class<?> marshalledClass;

    private Type marshalledGenericType;

    public AbstractJsonReaderTest(Class<ReaderClass> readerClass, Class<?> marshalledClass) {
        this(readerClass, marshalledClass, marshalledClass);
    }

    public AbstractJsonReaderTest(Class<ReaderClass> readerClass, Class<?> marshalledClass,
            Type marshalledGenericType) {
        super();
        this.readerClass = readerClass;
        this.marshalledClass = marshalledClass;
        this.marshalledGenericType = marshalledGenericType;
    }

    public ReaderClass getInstance() {
        return registry.getInstance(RenderingContext.CtxBuilder.get(), readerClass);
    }

    public ReaderClass getInstance(RenderingContext ctx) {
        return registry.getInstance(ctx, readerClass);
    }

    public MarshalledType asObject(String json) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(UTF_8))) {
            return getInstance().read(marshalledClass, marshalledGenericType, APPLICATION_JSON_TYPE, inputStream);
        }
    }

    public MarshalledType asObject(String json, RenderingContext ctx) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes(UTF_8))) {
            return getInstance(ctx).read(marshalledClass, marshalledGenericType, APPLICATION_JSON_TYPE, inputStream);
        }
    }

    public MarshalledType asObject(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return getInstance().read(marshalledClass, marshalledGenericType, APPLICATION_JSON_TYPE, inputStream);
        }
    }

    public MarshalledType asObject(File file, RenderingContext ctx) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return getInstance(ctx).read(marshalledClass, marshalledGenericType, APPLICATION_JSON_TYPE, inputStream);
        }
    }

}
