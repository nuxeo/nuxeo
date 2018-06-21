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

package org.nuxeo.ecm.core.io.registry;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.MediaType.TEXT_XML_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.DERIVATIVE;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.OVERRIDE_REFERENCE;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreIOFeature.class)
public class TestReaderRegistry {

    private RenderingContext ctx;

    private MarshallerRegistry registry;

    @Before
    public void setup() {
        ctx = RenderingContext.CtxBuilder.get();
        registry = Framework.getService(MarshallerRegistry.class);
        registry.clear();
    }

    @Test(expected = MarshallingException.class)
    public void registerInvalidReader() {
        registry.register(InvalidReader.class);
    }

    @Test(expected = MarshallingException.class)
    public void registerClassNotSupported() {
        registry.register(NotSupportedClass.class);
    }

    @Test
    public void simpleRegistering() {
        registry.register(DefaultNumberReader.class);
        Reader<?> Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertNotNull(Reader);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
    }

    @Test
    public void registerTwice() {
        registry.register(DefaultNumberReader.class);
        registry.register(DefaultNumberReader.class);
        Reader<?> Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
    }

    @Test
    public void priorities() {
        registry.register(DefaultNumberReader.class);
        registry.register(LowerPriorityReader.class);
        Reader<?> Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
        registry.register(HigherPriorityReader.class);
        Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(HigherPriorityReader.class, Reader.getClass());
    }

    @Test
    public void prioriseSingletonToPerThreadToEachTime() {
        registry.register(EachTimeReader.class);
        registry.register(PerThreadReader.class);
        Reader<?> Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(PerThreadReader.class, Reader.getClass());
        registry.register(DefaultNumberReader.class);
        Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
        registry.clear();
        registry.register(PerThreadReader.class);
        registry.register(DefaultNumberReader.class);
        Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
        registry.register(LowerPriorityReader.class);
        Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
    }

    // to force sub classes managing their priorities
    @Test
    public void prioriseParentClasses() {
        registry.register(DefaultNumberReader.class);
        registry.register(SubClassReader.class);
        Reader<?> Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
        registry.clear();
        registry.register(SubClassReader.class);
        registry.register(DefaultNumberReader.class);
        Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
    }

    @Test
    public void byMediaType() {
        registry.register(AnyTypeReader.class);
        Reader<?> Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(AnyTypeReader.class, Reader.getClass());
        registry.register(DefaultNumberReader.class);
        Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
        registry.register(XmlReader.class);
        Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
        Reader = registry.getReader(ctx, Integer.class, null, TEXT_XML_TYPE);
        assertEquals(XmlReader.class, Reader.getClass());
        registry.clear();
        registry.register(DefaultNumberReader.class);
        Reader = registry.getReader(ctx, Integer.class, null, WILDCARD_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
    }

    @Test
    public void ensureAcceptMethodIsCalled() {
        registry.register(SingletonStateReader.class);
        registry.register(DefaultNumberReader.class);
        Reader<?> Reader = registry.getReader(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(SingletonStateReader.class, Reader.getClass());
        RenderingContext ctx2 = RenderingContext.CtxBuilder.param("doNotAccept", true).get();
        Reader = registry.getReader(ctx2, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberReader.class, Reader.getClass());
    }

    @SuppressWarnings("unused")
    private Map<String, List<Integer>> listIntegerMapProperty = null;

    @SuppressWarnings("unused")
    private Map<String, List<?>> listMapProperty = null;

    @SuppressWarnings("unused")
    private Map<?, ?> mapProperty = null;

    @Test
    public void genericTypeChecking() throws Exception {
        Reader<?> Reader;
        Type listIntegerMap = TestReaderRegistry.class.getDeclaredField("listIntegerMapProperty").getGenericType();
        Type listMap = TestReaderRegistry.class.getDeclaredField("listMapProperty").getGenericType();
        Type map = TestReaderRegistry.class.getDeclaredField("mapProperty").getGenericType();
        registry.register(ListIntegerMapReader.class);
        Reader = registry.getReader(ctx, Map.class, listIntegerMap, APPLICATION_JSON_TYPE);
        assertNotNull(Reader);
        assertEquals(Reader.getClass(), ListIntegerMapReader.class);
        Reader = registry.getReader(ctx, Map.class, listMap, APPLICATION_JSON_TYPE);
        assertNull(Reader);
        Reader = registry.getReader(ctx, Map.class, map, APPLICATION_JSON_TYPE);
        assertNull(Reader);
        registry.register(ListMapReader.class);
        Reader = registry.getReader(ctx, Map.class, listIntegerMap, APPLICATION_JSON_TYPE);
        assertNotNull(Reader);
        Reader = registry.getReader(ctx, Map.class, listMap, APPLICATION_JSON_TYPE);
        assertNotNull(Reader);
        assertEquals(Reader.getClass(), ListMapReader.class);
        Reader = registry.getReader(ctx, Map.class, map, APPLICATION_JSON_TYPE);
        assertNull(Reader);
        registry.register(MapReader.class);
        Reader = registry.getReader(ctx, Map.class, listIntegerMap, APPLICATION_JSON_TYPE);
        assertNotNull(Reader);
        Reader = registry.getReader(ctx, Map.class, listMap, APPLICATION_JSON_TYPE);
        assertNotNull(Reader);
        Reader = registry.getReader(ctx, Map.class, map, APPLICATION_JSON_TYPE);
        assertNotNull(Reader);
        assertEquals(Reader.getClass(), MapReader.class);
    }

    // no @Setup annotation
    public static class InvalidReader implements Reader<Object> {

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public Object read(Class<?> clazz, Type genericType, MediaType mediatype, InputStream in) {
            return null;
        }

    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class NotSupportedClass {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class DefaultNumberReader implements Reader<Number> {

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public Number read(Class<?> clazz, Type genericType, MediaType mediatype, InputStream in) {
            return null;
        }

    }

    @Setup(mode = SINGLETON, priority = OVERRIDE_REFERENCE)
    public static class SingletonStateReader extends DefaultNumberReader {

        @Inject
        RenderingContext ctx;

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return !ctx.getBooleanParameter("doNotAccept");
        }

    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    public static class SubClassReader extends DefaultNumberReader {
    }

    @Setup(mode = SINGLETON, priority = OVERRIDE_REFERENCE)
    public static class HigherPriorityReader extends DefaultNumberReader {
    }

    @Setup(mode = SINGLETON, priority = DERIVATIVE)
    public static class LowerPriorityReader extends DefaultNumberReader {
    }

    @Setup(mode = SINGLETON, priority = DERIVATIVE)
    public static class PerThreadReader extends DefaultNumberReader {
    }

    @Setup(mode = SINGLETON, priority = DERIVATIVE)
    public static class EachTimeReader extends DefaultNumberReader {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(TEXT_XML)
    public static class XmlReader extends DefaultNumberReader {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(WILDCARD)
    public static class AnyTypeReader extends DefaultNumberReader {
    }

    @Setup(mode = SINGLETON)
    @Supports(APPLICATION_JSON)
    public static class ListIntegerMapReader implements Reader<Map<String, List<Integer>>> {

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public Map<String, List<Integer>> read(Class<?> clazz, Type genericType, MediaType mediatype, InputStream in) {
            return null;
        }

    }

    @Setup(mode = SINGLETON)
    @Supports(APPLICATION_JSON)
    public static class ListMapReader implements Reader<Map<?, List<?>>> {

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public Map<?, List<?>> read(Class<?> clazz, Type genericType, MediaType mediatype, InputStream in) {
            return null;
        }

    }

    @Setup(mode = SINGLETON)
    @Supports(APPLICATION_JSON)
    public static class MapReader implements Reader<Map<?, ?>> {

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public Map<?, ?> read(Class<?> clazz, Type genericType, MediaType mediatype, InputStream in) {
            return null;
        }

    }

}
