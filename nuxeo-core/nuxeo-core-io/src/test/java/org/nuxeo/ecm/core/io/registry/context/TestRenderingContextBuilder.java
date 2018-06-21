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
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.context.RenderingContext.DEFAULT_LOCALE;
import static org.nuxeo.ecm.core.io.registry.context.RenderingContext.DEFAULT_URL;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.EMBED_ENRICHERS;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.EMBED_PROPERTIES;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;

public class TestRenderingContextBuilder {

    private static final String VALUE1 = "value1";

    private static final String VALUE2 = "value2";

    private static final String PARAM = "test";

    @Test
    public void emptyContext() throws Exception {
        RenderingContext ctx = CtxBuilder.get();
        assertNotNull(ctx);
        assertEquals(DEFAULT_URL, ctx.getBaseUrl());
        assertEquals(DEFAULT_LOCALE, ctx.getLocale());
        Map<String, List<Object>> allParameters = ctx.getAllParameters();
        assertNotNull(allParameters);
        assertTrue(allParameters.isEmpty());
    }

    @Test
    public void simpleContext() throws Exception {
        RenderingContext ctx = CtxBuilder.base("url")
                                         .locale(Locale.FRANCE)
                                         .param(PARAM, VALUE1)
                                         .param(PARAM, VALUE2)
                                         .get();
        assertEquals("url", ctx.getBaseUrl());
        assertEquals(Locale.FRANCE, ctx.getLocale());
        Map<String, List<Object>> allParameters = ctx.getAllParameters();
        assertEquals(1, allParameters.size());
        List<Object> values = allParameters.get(PARAM);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(VALUE1));
        assertTrue(values.contains(VALUE2));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDocProperties() throws Exception {
        RenderingContext ctx = CtxBuilder.properties("one,two, three")
                                         .properties("four")
                                         .param(EMBED_PROPERTIES, " five , six")
                                         .get();
        Set<String> properties = ctx.getProperties();
        assertEquals(6, properties.size());
        assertTrue(properties.contains("one"));
        assertTrue(properties.contains("two"));
        assertTrue(properties.contains("three"));
        assertTrue(properties.contains("four"));
        assertTrue(properties.contains("five"));
        assertTrue(properties.contains("six"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testEnrichment() throws Exception {
        RenderingContext ctx = CtxBuilder.enrich(ENTITY_TYPE, "one,two, three")
                                         .enrichDoc("four")
                                         .param(EMBED_ENRICHERS + "-document", " five , six")
                                         .get();
        Set<String> properties = ctx.getEnrichers(ENTITY_TYPE);
        assertEquals(6, properties.size());
        assertTrue(properties.contains("one"));
        assertTrue(properties.contains("two"));
        assertTrue(properties.contains("three"));
        assertTrue(properties.contains("four"));
        assertTrue(properties.contains("five"));
        assertTrue(properties.contains("six"));
    }

    @Test
    public void testFetchedProperties() throws Exception {
        RenderingContext ctx = CtxBuilder.fetchInDoc("one,two, three")
                                         .fetch(ENTITY_TYPE, "four")
                                         .param("fetch.document", " five , six")
                                         .get();
        Set<String> properties = ctx.getFetched(ENTITY_TYPE);
        assertEquals(6, properties.size());
        assertTrue(properties.contains("one"));
        assertTrue(properties.contains("two"));
        assertTrue(properties.contains("three"));
        assertTrue(properties.contains("four"));
        assertTrue(properties.contains("five"));
        assertTrue(properties.contains("six"));
    }

}
