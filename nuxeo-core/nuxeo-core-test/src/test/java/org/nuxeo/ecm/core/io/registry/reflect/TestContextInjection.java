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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.ThreadSafeRenderingContext;

public class TestContextInjection {

    private final RenderingContext ctx = RenderingContext.CtxBuilder.get();

    @Test
    public void noInjectionIfNoAnnotation() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(NoInjectionMarshaller.class);
        NoInjectionMarshaller instance = inspector.getInstance(ctx);
        assertNull(instance.ctx);
    }

    @Test
    public void ifNullContextInjectEmptyContext() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(SingletonMarshaller.class);
        SingletonMarshaller instance = inspector.getInstance(null);
        assertNotNull(instance.ctx);
        assertTrue(instance.ctx.getAllParameters().isEmpty());
    }

    @Test
    public void ifThreadSafeContextInjectDelegateContext() throws Exception {
        ThreadSafeRenderingContext tsCtx = new ThreadSafeRenderingContext();
        tsCtx.configureThread(ctx);
        MarshallerInspector inspector = new MarshallerInspector(EachTimeMarshaller.class);
        EachTimeMarshaller instance = inspector.getInstance(tsCtx);
        assertNotNull(instance.ctx);
        assertSame(ctx, instance.ctx);
    }

    @Test
    public void injectInEachTimeInstance() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(EachTimeMarshaller.class);
        EachTimeMarshaller instance = inspector.getInstance(ctx);
        assertSame(ctx, instance.ctx);
    }

    @Test
    public void injectInPerThreadInstance() throws Exception {
        final MarshallerInspector inspector = new MarshallerInspector(PerThreadMarshaller.class);
        PerThreadMarshaller instance1 = inspector.getInstance(ctx);
        assertSame(ctx, instance1.ctx);
        Thread subThread = new Thread() {
            @Override
            public void run() {
                synchronized (this) {
                    // in a different thread, it should be a different instance but same context
                    final PerThreadMarshaller instance2 = inspector.getInstance(ctx);
                    assertSame(ctx, instance2.ctx);
                    notify();
                }
            }

        };
        subThread.start();
        synchronized (subThread) {
            subThread.wait();
        }
    }

    @Test
    public void replaceContextInPerThreadInstance() throws Exception {
        final MarshallerInspector inspector = new MarshallerInspector(PerThreadMarshaller.class);
        PerThreadMarshaller instance1 = inspector.getInstance(ctx);
        RenderingContext ctx2 = RenderingContext.CtxBuilder.get();
        PerThreadMarshaller instance2 = inspector.getInstance(ctx2);
        assertSame(ctx2, instance1.ctx);
        assertSame(ctx2, instance2.ctx);
    }

    @Test
    public void injectInSingletonInstance() throws Exception {
        final MarshallerInspector inspector = new MarshallerInspector(SingletonMarshaller.class);
        SingletonMarshaller instance1 = inspector.getInstance(ctx);
        assertNotSame(ctx, instance1.ctx);
        assertTrue(instance1.ctx instanceof ThreadSafeRenderingContext);
        ThreadSafeRenderingContext safeCtx = (ThreadSafeRenderingContext) instance1.ctx;
        assertNotNull(safeCtx.getDelegate());
        assertSame(ctx, safeCtx.getDelegate());
        Thread subThread = new Thread() {
            @Override
            public void run() {
                synchronized (this) {
                    // in a different thread, it should be a different instance but same context
                    final SingletonMarshaller instance2 = inspector.getInstance(ctx);
                    assertNotSame(ctx, instance2.ctx);
                    assertTrue(instance2.ctx instanceof ThreadSafeRenderingContext);
                    ThreadSafeRenderingContext safeCtx = (ThreadSafeRenderingContext) instance2.ctx;
                    assertNotNull(safeCtx.getDelegate());
                    assertSame(ctx, safeCtx.getDelegate());
                    notify();
                }
            }

        };
        subThread.start();
        synchronized (subThread) {
            subThread.wait();
        }
    }

    @Test
    public void replaceContextInSingletonInstance() throws Exception {
        final MarshallerInspector inspector = new MarshallerInspector(SingletonMarshaller.class);
        SingletonMarshaller instance1 = inspector.getInstance(ctx);
        RenderingContext ctx2 = RenderingContext.CtxBuilder.get();
        SingletonMarshaller instance2 = inspector.getInstance(ctx2);
        ThreadSafeRenderingContext safeCtx1 = (ThreadSafeRenderingContext) instance1.ctx;
        ThreadSafeRenderingContext safeCtx2 = (ThreadSafeRenderingContext) instance2.ctx;
        assertSame(ctx2, safeCtx1.getDelegate());
        assertSame(ctx2, safeCtx2.getDelegate());
    }

    @Test
    public void inheritInjection() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(InheritMarshaller.class);
        InheritMarshaller instance = inspector.getInstance(ctx);
        assertSame(ctx, instance.ctx);
        assertSame(ctx, instance.ctx2);
    }

    @Setup(mode = Instantiations.EACH_TIME)
    public static class NoInjectionMarshaller implements Writer<Object> {
        private RenderingContext ctx;

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public void write(Object entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
                throws IOException {
        }
    }

    @Setup(mode = Instantiations.EACH_TIME)
    public static class EachTimeMarshaller implements Writer<Object> {
        @Inject
        protected RenderingContext ctx;

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public void write(Object entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
                throws IOException {
        }

    }

    @Setup(mode = Instantiations.PER_THREAD)
    public static class PerThreadMarshaller implements Writer<Object> {
        @Inject
        private RenderingContext ctx;

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public void write(Object entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
                throws IOException {
        }
    }

    @Setup(mode = Instantiations.SINGLETON)
    public static class SingletonMarshaller implements Writer<Object> {
        @Inject
        private RenderingContext ctx;

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public void write(Object entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
                throws IOException {
        }
    }

    @Setup(mode = Instantiations.EACH_TIME)
    public static class InheritMarshaller extends EachTimeMarshaller {
        @Inject
        private RenderingContext ctx2;
    }

}
