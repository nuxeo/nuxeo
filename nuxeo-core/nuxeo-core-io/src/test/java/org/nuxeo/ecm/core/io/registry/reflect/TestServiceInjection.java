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

package org.nuxeo.ecm.core.io.registry.reflect;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.EACH_TIME;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
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
