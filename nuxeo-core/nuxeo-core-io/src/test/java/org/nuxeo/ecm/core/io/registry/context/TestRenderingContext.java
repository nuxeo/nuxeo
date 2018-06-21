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

package org.nuxeo.ecm.core.io.registry.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.EMBED_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.HEADER_PREFIX;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class TestRenderingContext {

    private static final String VALUE1 = "value1";

    private static final String VALUE2 = "value2";

    private static final String VALUE3 = "value3";

    private static final String PARAM = "test";

    @Test
    public void emptyContext() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.get();
        assertEquals(RenderingContext.DEFAULT_URL, ctx.getBaseUrl());
        assertEquals(RenderingContext.DEFAULT_LOCALE, ctx.getLocale());
        assertTrue(ctx.getAllParameters().isEmpty());
    }

    @Test
    public void canSetAndGetSimpleParameter() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.param(PARAM, VALUE1).get();
        assertEquals(VALUE1, ctx.getParameter(PARAM));
    }

    /**
     * @since 8.4
     */
    @Test
    public void canSetAndGetSimpleWrappedParameters() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.get();
        try (Closeable resource = ctx.wrap().with(PARAM, VALUE1).open()) {
            assertEquals(VALUE1, ctx.getParameter(PARAM));
        }
    }

    @Test
    public void canSetAndGetMultipleParameters() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.paramValues(PARAM, VALUE1, VALUE2).get();
        List<String> list = ctx.getParameters(PARAM);
        assertEquals(2, list.size());
        assertTrue(list.contains(VALUE1));
        assertTrue(list.contains(VALUE2));
    }

    /**
     * @since 8.4
     */
    @Test
    public void canSetAndGetMultipleWrappedParameters() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.get();
        List<String> expectedList = new ArrayList<>();
        expectedList.add(VALUE1);
        expectedList.add(VALUE2);
        try (Closeable resource = ctx.wrap().with(PARAM, expectedList).open()) {
            List<String> list = ctx.getParameters(PARAM);
            assertEquals(2, list.size());
            assertTrue(list.contains(VALUE1));
            assertTrue(list.contains(VALUE2));
        }
    }

    @Test
    public void getQuoteSeparatedNxParam() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.param(EMBED_PROPERTIES, VALUE1 + "," + VALUE2).get();
        Set<String> embeds = ctx.getProperties();
        assertNotNull(embeds);
        assertEquals(2, embeds.size());
        assertTrue(embeds.contains(VALUE1));
        assertTrue(embeds.contains(VALUE2));
    }

    @Test
    public void getMultipleNxParam() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.param(EMBED_PROPERTIES, VALUE1)
                                                          .param(EMBED_PROPERTIES, VALUE2)
                                                          .get();
        Set<String> embeds = ctx.getProperties();
        assertNotNull(embeds);
        assertEquals(2, embeds.size());
        assertTrue(embeds.contains(VALUE1));
        assertTrue(embeds.contains(VALUE2));
    }

    @Test
    public void getPrefixedNxParam() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.param(HEADER_PREFIX + EMBED_PROPERTIES, VALUE1)
                                                          .param(HEADER_PREFIX + EMBED_PROPERTIES, VALUE2)
                                                          .get();
        Set<String> embeds = ctx.getProperties();
        assertNotNull(embeds);
        assertEquals(2, embeds.size());
        assertTrue(embeds.contains(VALUE1));
        assertTrue(embeds.contains(VALUE2));
    }

    @Test
    public void getMixedNxParam() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.param(EMBED_PROPERTIES, VALUE1 + "," + VALUE2)
                                                          .param(HEADER_PREFIX + EMBED_PROPERTIES, VALUE3)
                                                          .get();
        Set<String> embeds = ctx.getProperties();
        assertNotNull(embeds);
        assertEquals(3, embeds.size());
        assertTrue(embeds.contains(VALUE1));
        assertTrue(embeds.contains(VALUE2));
        assertTrue(embeds.contains(VALUE3));
    }

}
