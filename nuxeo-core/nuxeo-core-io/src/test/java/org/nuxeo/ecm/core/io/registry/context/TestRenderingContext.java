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

package org.nuxeo.ecm.core.io.registry.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.DOCUMENT_PROPERTIES_HEADER;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.EMBED_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.HEADER_PREFIX;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.NXCONTENT_CATEGORY_HEADER;

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

    @Test
    public void canSetAndGetMultipleParameters() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.paramValues(PARAM, VALUE1, VALUE2).get();
        List<String> list = ctx.getParameters(PARAM);
        assertEquals(2, list.size());
        assertTrue(list.contains(VALUE1));
        assertTrue(list.contains(VALUE2));
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
        RenderingContext ctx = RenderingContext.CtxBuilder.param(EMBED_PROPERTIES, VALUE1).param(EMBED_PROPERTIES, VALUE2).get();
        Set<String> embeds = ctx.getProperties();
        assertNotNull(embeds);
        assertEquals(2, embeds.size());
        assertTrue(embeds.contains(VALUE1));
        assertTrue(embeds.contains(VALUE2));
    }

    @Test
    public void getPrefixedNxParam() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.param(HEADER_PREFIX + EMBED_PROPERTIES, VALUE1).param(
                HEADER_PREFIX + EMBED_PROPERTIES, VALUE2).get();
        Set<String> embeds = ctx.getProperties();
        assertNotNull(embeds);
        assertEquals(2, embeds.size());
        assertTrue(embeds.contains(VALUE1));
        assertTrue(embeds.contains(VALUE2));
    }

    @Test
    public void getMixedNxParam() throws Exception {
        RenderingContext ctx = RenderingContext.CtxBuilder.param(EMBED_PROPERTIES, VALUE1 + "," + VALUE2).param(
                HEADER_PREFIX + EMBED_PROPERTIES, VALUE3).get();
        Set<String> embeds = ctx.getProperties();
        assertNotNull(embeds);
        assertEquals(3, embeds.size());
        assertTrue(embeds.contains(VALUE1));
        assertTrue(embeds.contains(VALUE2));
        assertTrue(embeds.contains(VALUE3));
    }

    @Test
    public void nxParamBackwardCompatPropertiesHeader() throws Exception {
        @SuppressWarnings("deprecation")
        RenderingContext ctx = RenderingContext.CtxBuilder.param(DOCUMENT_PROPERTIES_HEADER, VALUE1 + "," + VALUE2).get();
        Set<String> embeds = ctx.getProperties();
        assertNotNull(embeds);
        assertEquals(2, embeds.size());
        assertTrue(embeds.contains(VALUE1));
        assertTrue(embeds.contains(VALUE2));
    }

    @Test
    public void nxParamBackwardCompatEnricherHeader() throws Exception {
        @SuppressWarnings("deprecation")
        RenderingContext ctx = RenderingContext.CtxBuilder.param(NXCONTENT_CATEGORY_HEADER, VALUE1 + "," + VALUE2).get();
        Set<String> embeds = ctx.getEnrichers(ENTITY_TYPE);
        assertNotNull(embeds);
        assertEquals(2, embeds.size());
        assertTrue(embeds.contains(VALUE1));
        assertTrue(embeds.contains(VALUE2));
    }

}
