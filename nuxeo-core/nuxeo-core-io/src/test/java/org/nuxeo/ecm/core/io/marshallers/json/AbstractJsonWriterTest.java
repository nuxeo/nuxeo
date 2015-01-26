/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

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
 *
 * </p>
 *
 * @param <WriterClass> The marshaller type to test.
 * @param <MarshalledType> The marshalled type to test.
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public abstract class AbstractJsonWriterTest<WriterClass extends Writer<MarshalledType>, MarshalledType> {

    @LocalDeploy({ "org.nuxeo.ecm.core.io:OSGI-INF/MarshallerRegistry.xml",
            "org.nuxeo.ecm.core.io:OSGI-INF/marshallers-contrib.xml" })
    public static abstract class Local<WriterClass extends Writer<MarshalledType>, MarshalledType> extends
            AbstractJsonWriterTest<WriterClass, MarshalledType> {

        public Local(Class<WriterClass> writerClass, Class<?> marshalledClass, Type marshalledGenericType) {
            super(writerClass, marshalledClass, marshalledGenericType);
        }

        public Local(Class<WriterClass> writerClass, Class<?> marshalledClass) {
            super(writerClass, marshalledClass);
        }

    }

    @Deploy({ "org.nuxeo.ecm.core.io" })
    public static abstract class External<WriterClass extends Writer<MarshalledType>, MarshalledType> extends
            AbstractJsonWriterTest<WriterClass, MarshalledType> {

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

    public AbstractJsonWriterTest(Class<WriterClass> writerClass, Class<?> marshalledClass, Type marshalledGenericType) {
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
