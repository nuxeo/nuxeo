/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

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
 * Base class to test Java to Json marshallers.
 * <p>
 * see {@link JsonAssert} to see use simple json assertion.
 * </p>
 * <p>
 * To use it, write a default constructor and call the parent with the needed classes.
 *
 * <pre>
 * &#064;Test
 * public void test() throws Exception {
 *     MarshalledType objectToTransformAsJson = ...;
 *     JsonAssert json = jsonAssert(objectToTransformAsJson);
 *     json.isObject();
 *     json.properties(2);
 *     json.has(&quot;entity-type&quot;).isEquals(&quot;someEntity&quot;);
 *     ...
 * }
 * </pre>
 * </p>
 *
 * @param <WriterClass> The marshaller type to test.
 * @param <MarshalledType> The marshalled type to test.
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreIOFeature.class)
public abstract class AbstractJsonWriterTest<WriterClass extends Writer<MarshalledType>, MarshalledType> {

    public static abstract class Local<WriterClass extends Writer<MarshalledType>, MarshalledType>
            extends AbstractJsonWriterTest<WriterClass, MarshalledType> {

        public Local(Class<WriterClass> writerClass, Class<?> marshalledClass, Type marshalledGenericType) {
            super(writerClass, marshalledClass, marshalledGenericType);
        }

        public Local(Class<WriterClass> writerClass, Class<?> marshalledClass) {
            super(writerClass, marshalledClass);
        }

    }

    @Deploy("org.nuxeo.runtime.stream")
    public static abstract class External<WriterClass extends Writer<MarshalledType>, MarshalledType>
            extends AbstractJsonWriterTest<WriterClass, MarshalledType> {

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

    public AbstractJsonWriterTest(Class<WriterClass> writerClass, Class<?> marshalledClass) {
        this(writerClass, marshalledClass, marshalledClass);
    }

    public AbstractJsonWriterTest(Class<WriterClass> writerClass, Class<?> marshalledClass,
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

    public String asJson(MarshalledType object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        getInstance().write(object, marshalledClass, marshalledGenericType, APPLICATION_JSON_TYPE, baos);
        return baos.toString();
    }

    public String asJson(MarshalledType object, RenderingContext ctx) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        getInstance(ctx).write(object, marshalledClass, marshalledGenericType, APPLICATION_JSON_TYPE, baos);
        return baos.toString();
    }

    public JsonAssert jsonAssert(MarshalledType object) throws IOException {
        return JsonAssert.on(asJson(object));
    }

    public JsonAssert jsonAssert(MarshalledType object, RenderingContext ctx) throws IOException {
        return JsonAssert.on(asJson(object, ctx));
    }

}
