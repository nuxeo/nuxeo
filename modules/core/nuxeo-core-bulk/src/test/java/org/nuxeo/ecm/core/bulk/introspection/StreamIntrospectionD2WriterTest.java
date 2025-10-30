/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.bulk.introspection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.common.test.ModuleUnderTest.getClassLoaderResourceAsString;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionD2Writer.PARAMETER_EXCLUDE_FILTER;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionD2Writer.PARAMETER_EXCLUDE_INACTIVE;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospections.getClassLoaderResourceAsStreamIntrospection;

import java.io.IOException;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.d2.AbstractD2WriterTest;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 2025.12
 */
@Deploy("org.nuxeo.ecm.core.bulk:OSGI-INF/bulk-introspection-io-contrib.xml")
public class StreamIntrospectionD2WriterTest
        extends AbstractD2WriterTest.Local<StreamIntrospectionD2Writer, StreamIntrospection> {

    @Test
    public void testD2Conversion() throws IOException {
        var streamIntrospection = getClassLoaderResourceAsStreamIntrospection("data/introspection.json");
        String d2 = asD2(streamIntrospection);
        assertTrue(d2.contains("# Stream Introspection"));
        assertTrue(d2.contains("stream_bulk_command"));
        assertTrue(d2, d2.contains("group_bulk_scroller"));
    }

    @Test
    public void testD2ConversionSimple() throws IOException {
        var streamIntrospection = getClassLoaderResourceAsStreamIntrospection("data/simple.json");
        String d2 = asD2(streamIntrospection);
        assertEquals(getClassLoaderResourceAsString("data/simple.d2"), d2);
    }

    @Test
    public void testD2ConversionWithFiltering() throws IOException {
        var streamIntrospection = getClassLoaderResourceAsStreamIntrospection("data/introspection.json");
        String json = getClassLoaderResourceAsString("data/introspection.json");

        // Test without filtering
        String d2NoFilter = asD2(streamIntrospection);
        assertTrue(d2NoFilter.contains("stream_bulk_command"));
        assertTrue(d2NoFilter.contains("group_bulk_scroller"));

        // Test with pattern filtering to exclude "bulk/" patterns
        String d2WithPatternFilter = asD2(streamIntrospection,
                RenderingContext.CtxBuilder.param(PARAMETER_EXCLUDE_FILTER, "bulk/")
                                           .param(PARAMETER_EXCLUDE_INACTIVE, false)
                                           .get());
        assertFalse(d2WithPatternFilter.contains("stream_bulk_command"));
        assertFalse(d2WithPatternFilter.contains("group_bulk_scroller"));

        // Test with inactive filtering (should exclude both inactive computations and empty streams)
        String d2WithInactiveFilter = asD2(streamIntrospection,
                RenderingContext.CtxBuilder.param(PARAMETER_EXCLUDE_FILTER, "")
                                           .param(PARAMETER_EXCLUDE_INACTIVE, true)
                                           .get());
        assertTrue(d2WithInactiveFilter.contains("# Stream Introspection"));
        // Should exclude streams with 0 records (stream-empty class)
        assertFalse(d2WithInactiveFilter.contains("stream_work_collections")); // This stream has 0 records

        // Test with both filters
        String d2WithBothFilters = asD2(streamIntrospection,
                RenderingContext.CtxBuilder.param(PARAMETER_EXCLUDE_FILTER, "bulk/")
                                           .param(PARAMETER_EXCLUDE_INACTIVE, true)
                                           .get());
        assertFalse(d2WithBothFilters.contains("bulk_"));
    }
}
