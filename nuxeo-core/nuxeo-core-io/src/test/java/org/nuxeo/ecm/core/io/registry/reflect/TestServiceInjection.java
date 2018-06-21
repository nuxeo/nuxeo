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

package org.nuxeo.ecm.core.io.registry.reflect;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.EACH_TIME;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreIOFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
public class TestServiceInjection {

    private final RenderingContext ctx = RenderingContext.CtxBuilder.get();

    @Test
    public void noInjectionIfNoAnnotation() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(NoInjectionMarshaller.class);
        NoInjectionMarshaller instance = inspector.getInstance(ctx);
        assertNull(instance.service);
    }

    @Test
    public void injectService() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(SimpleServiceMarshaller.class);
        SimpleServiceMarshaller instance = inspector.getInstance(ctx);
        assertNotNull(instance.service);
    }

    @Test
    public void inheritInjection() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(InheritMarshaller.class);
        InheritMarshaller instance = inspector.getInstance(ctx);
        assertNotNull(instance.service);
        assertNotNull(instance.service2);
    }

    @Setup(mode = EACH_TIME)
    public static class SimpleServiceMarshaller implements Writer<Object> {
        @Inject
        protected SchemaManager service;

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public void write(Object entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
                throws IOException {
        }
    }

    @Setup(mode = EACH_TIME)
    public static class NoInjectionMarshaller implements Writer<Object> {
        private SchemaManager service;

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public void write(Object entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
                throws IOException {
        }
    }

    @Setup(mode = EACH_TIME)
    public static class InheritMarshaller extends SimpleServiceMarshaller {
        @Inject
        private SchemaManager service2;
    }

}
