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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.io.pojo.Child;
import org.nuxeo.ecm.core.io.pojo.Marshallers.ChildListWriter;
import org.nuxeo.ecm.core.io.pojo.Marshallers.ChildWriter;
import org.nuxeo.ecm.core.io.pojo.Marshallers.DefaultWriter;
import org.nuxeo.ecm.core.io.pojo.Marshallers.ParentListWriter;
import org.nuxeo.ecm.core.io.pojo.Marshallers.ParentWriter;
import org.nuxeo.ecm.core.io.pojo.Parent;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreIOFeature.class)
public class TestWriterRegistry {

    private RenderingContext ctx;

    private MarshallerRegistry registry;

    @Before
    public void setup() {
        ctx = RenderingContext.CtxBuilder.get();
        registry = Framework.getService(MarshallerRegistry.class);
        registry.clear();
    }

    @Test(expected = MarshallingException.class)
    public void registerInvalidWriter() throws Exception {
        registry.register(InvalidWriter.class);
    }

    @Test(expected = MarshallingException.class)
    public void registerClassNotSupported() throws Exception {
        registry.register(NotSupportedClass.class);
    }

    @Test
    public void simpleRegistering() throws Exception {
        registry.register(DefaultNumberWriter.class);
        Writer<?> writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertNotNull(writer);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
    }

    @Test
    public void registerTwice() throws Exception {
        registry.register(DefaultNumberWriter.class);
        registry.register(DefaultNumberWriter.class);
        Writer<?> writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
    }

    @Test
    public void priorities() throws Exception {
        registry.register(DefaultNumberWriter.class);
        registry.register(LowerPriorityWriter.class);
        Writer<?> writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
        registry.register(HigherPriorityWriter.class);
        writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(HigherPriorityWriter.class, writer.getClass());
    }

    @Test
    public void prioriseSingletonToPerThreadToEachTime() throws Exception {
        registry.register(EachTimeWriter.class);
        registry.register(PerThreadWriter.class);
        Writer<?> writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(PerThreadWriter.class, writer.getClass());
        registry.register(DefaultNumberWriter.class);
        writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
        registry.clear();
        registry.register(PerThreadWriter.class);
        registry.register(DefaultNumberWriter.class);
        writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
        registry.register(LowerPriorityWriter.class);
        writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
    }

    // to force sub classes managing their priorities
    @Test
    public void prioriseParentClasses() throws Exception {
        registry.register(DefaultNumberWriter.class);
        registry.register(SubClassWriter.class);
        Writer<?> writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
        registry.clear();
        registry.register(SubClassWriter.class);
        registry.register(DefaultNumberWriter.class);
        writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
    }

    @Test
    public void byMediaType() throws Exception {
        registry.register(AnyTypeWriter.class);
        Writer<?> writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(AnyTypeWriter.class, writer.getClass());
        registry.register(DefaultNumberWriter.class);
        writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
        registry.register(XmlWriter.class);
        writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, Integer.class, null, TEXT_XML_TYPE);
        assertEquals(XmlWriter.class, writer.getClass());
        registry.clear();
        registry.register(DefaultNumberWriter.class);
        writer = registry.getWriter(ctx, Integer.class, null, WILDCARD_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
    }

    @Test
    public void ensureAcceptMethodIsCalled() throws Exception {
        registry.register(SingletonStateWriter.class);
        registry.register(DefaultNumberWriter.class);
        Writer<?> writer = registry.getWriter(ctx, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(SingletonStateWriter.class, writer.getClass());
        RenderingContext ctx2 = RenderingContext.CtxBuilder.param("doNotAccept", true).get();
        writer = registry.getWriter(ctx2, Integer.class, null, APPLICATION_JSON_TYPE);
        assertEquals(DefaultNumberWriter.class, writer.getClass());
    }

    // keep those, we want to test reflection on private fields
    @SuppressWarnings("unused")
    private Map<String, List<Integer>> listIntegerMapProperty = null;

    @SuppressWarnings("unused")
    private Map<String, List<?>> listMapProperty = null;

    @SuppressWarnings("unused")
    private Map<?, ?> mapProperty = null;

    @Test
    public void genericTypeChecking() throws Exception {
        Writer<?> writer;
        Type listIntegerMap = TestWriterRegistry.class.getDeclaredField("listIntegerMapProperty").getGenericType();
        Type listMap = TestWriterRegistry.class.getDeclaredField("listMapProperty").getGenericType();
        Type map = TestWriterRegistry.class.getDeclaredField("mapProperty").getGenericType();
        registry.register(ListIntegerMapWriter.class);
        writer = registry.getWriter(ctx, Map.class, listIntegerMap, APPLICATION_JSON_TYPE);
        assertNotNull(writer);
        assertEquals(writer.getClass(), ListIntegerMapWriter.class);
        writer = registry.getWriter(ctx, Map.class, listMap, APPLICATION_JSON_TYPE);
        assertNull(writer);
        writer = registry.getWriter(ctx, Map.class, map, APPLICATION_JSON_TYPE);
        assertNull(writer);
        registry.register(ListMapWriter.class);
        writer = registry.getWriter(ctx, Map.class, listIntegerMap, APPLICATION_JSON_TYPE);
        assertNotNull(writer);
        writer = registry.getWriter(ctx, Map.class, listMap, APPLICATION_JSON_TYPE);
        assertNotNull(writer);
        assertEquals(writer.getClass(), ListMapWriter.class);
        writer = registry.getWriter(ctx, Map.class, map, APPLICATION_JSON_TYPE);
        assertNull(writer);
        registry.register(MapWriter.class);
        writer = registry.getWriter(ctx, Map.class, listIntegerMap, APPLICATION_JSON_TYPE);
        assertNotNull(writer);
        writer = registry.getWriter(ctx, Map.class, listMap, APPLICATION_JSON_TYPE);
        assertNotNull(writer);
        writer = registry.getWriter(ctx, Map.class, map, APPLICATION_JSON_TYPE);
        assertNotNull(writer);
        assertEquals(writer.getClass(), MapWriter.class);
    }

    // NXP-33227
    @Test
    public void childAndChildListClassesShouldBeChosen() {
        Writer<?> writer;
        Type parentListType = TypeUtils.parameterize(List.class, Parent.class);
        Type childListType = TypeUtils.parameterize(List.class, Child.class);

        registry.register(ParentWriter.class);
        registry.register(ParentListWriter.class);
        registry.register(ChildWriter.class);
        registry.register(ChildListWriter.class);

        writer = registry.getWriter(ctx, Parent.class, null, APPLICATION_JSON_TYPE);
        assertEquals(ParentWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, List.class, parentListType, APPLICATION_JSON_TYPE);
        assertEquals(ParentListWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, Child.class, null, APPLICATION_JSON_TYPE);
        assertEquals(ChildWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, List.class, childListType, APPLICATION_JSON_TYPE);
        assertEquals(ChildListWriter.class, writer.getClass());

        registry.clear();

        registry.register(ParentListWriter.class);
        registry.register(ParentWriter.class);
        registry.register(ChildListWriter.class);
        registry.register(ChildWriter.class);

        writer = registry.getWriter(ctx, Parent.class, null, APPLICATION_JSON_TYPE);
        assertEquals(ParentWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, List.class, parentListType, APPLICATION_JSON_TYPE);
        assertEquals(ParentListWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, Child.class, null, APPLICATION_JSON_TYPE);
        assertEquals(ChildWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, List.class, childListType, APPLICATION_JSON_TYPE);
        assertEquals(ChildListWriter.class, writer.getClass());

        registry.clear();

        registry.register(ChildWriter.class);
        registry.register(ChildListWriter.class);
        registry.register(ParentWriter.class);
        registry.register(ParentListWriter.class);

        writer = registry.getWriter(ctx, Parent.class, null, APPLICATION_JSON_TYPE);
        assertEquals(ParentWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, List.class, parentListType, APPLICATION_JSON_TYPE);
        assertEquals(ParentListWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, Child.class, null, APPLICATION_JSON_TYPE);
        assertEquals(ChildWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, List.class, childListType, APPLICATION_JSON_TYPE);
        assertEquals(ChildListWriter.class, writer.getClass());

        registry.clear();

        registry.register(ChildListWriter.class);
        registry.register(ChildWriter.class);
        registry.register(ParentListWriter.class);
        registry.register(ParentWriter.class);

        writer = registry.getWriter(ctx, Parent.class, null, APPLICATION_JSON_TYPE);
        assertEquals(ParentWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, List.class, parentListType, APPLICATION_JSON_TYPE);
        assertEquals(ParentListWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, Child.class, null, APPLICATION_JSON_TYPE);
        assertEquals(ChildWriter.class, writer.getClass());
        writer = registry.getWriter(ctx, List.class, childListType, APPLICATION_JSON_TYPE);
        assertEquals(ChildListWriter.class, writer.getClass());
    }

    // no @Setup annotation
    public static class InvalidWriter implements DefaultWriter<Object> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class NotSupportedClass {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class DefaultNumberWriter implements DefaultWriter<Number> {
    }

    @Setup(mode = SINGLETON, priority = OVERRIDE_REFERENCE)
    public static class SingletonStateWriter extends DefaultNumberWriter {

        @Inject
        RenderingContext ctx;

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return !ctx.getBooleanParameter("doNotAccept");
        }

    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    public static class SubClassWriter extends DefaultNumberWriter {
    }

    @Setup(mode = SINGLETON, priority = OVERRIDE_REFERENCE)
    public static class HigherPriorityWriter extends DefaultNumberWriter {
    }

    @Setup(mode = SINGLETON, priority = DERIVATIVE)
    public static class LowerPriorityWriter extends DefaultNumberWriter {
    }

    @Setup(mode = SINGLETON, priority = DERIVATIVE)
    public static class PerThreadWriter extends DefaultNumberWriter {
    }

    @Setup(mode = SINGLETON, priority = DERIVATIVE)
    public static class EachTimeWriter extends DefaultNumberWriter {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(TEXT_XML)
    public static class XmlWriter extends DefaultNumberWriter {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(WILDCARD)
    public static class AnyTypeWriter extends DefaultNumberWriter {
    }

    @Setup(mode = SINGLETON)
    @Supports(APPLICATION_JSON)
    public static class ListIntegerMapWriter implements DefaultWriter<Map<String, List<Integer>>> {
    }

    @Setup(mode = SINGLETON)
    @Supports(APPLICATION_JSON)
    public static class ListMapWriter implements DefaultWriter<Map<?, List<?>>> {
    }

    @Setup(mode = SINGLETON)
    @Supports(APPLICATION_JSON)
    public static class MapWriter implements DefaultWriter<Map<?, ?>> {
    }
}
