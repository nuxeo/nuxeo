/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Tests for {@link SearchLimitation} and {@link LimitationKind}.
 *
 * @since 2025.17
 */
public class TestSearchLimitation {

    @Test
    public void testSearchLimitationCreation() {
        var lim = SearchLimitation.of(LimitationKind.INDEX_MAPPING, SearchClient.Capability.AGGREGATE,
                "Field dc:source not in index");
        assertNotNull(lim);
        assertEquals(LimitationKind.INDEX_MAPPING, lim.getKind());
        assertEquals(SearchClient.Capability.AGGREGATE, lim.getAffectedCapability());
        assertEquals("Field dc:source not in index", lim.getMessage());
    }

    @Test
    public void testSearchLimitationWithNullContext() {
        var lim = SearchLimitation.of(LimitationKind.UNSUPPORTED, SearchClient.Capability.HIGHLIGHT,
                "Client does not support highlight");
        assertEquals(LimitationKind.UNSUPPORTED, lim.getKind());
    }

    @Test
    public void testMissingCapabilitiesDerivedFromLimitations() {
        var limitations = List.of(
                SearchLimitation.of(LimitationKind.INDEX_MAPPING, SearchClient.Capability.AGGREGATE, "msg"),
                SearchLimitation.of(LimitationKind.UNSUPPORTED, SearchClient.Capability.AGGREGATE, "msg2"));
        var response = SearchResponse.builder(List.of()).total(0).limitations(limitations).build();
        assertEquals(List.of(SearchClient.Capability.AGGREGATE), response.getMissingCapabilities());
        assertTrue(response.isMissingCapabilities());
        assertEquals(2, response.getLimitations().size());
    }

    @Test
    @SuppressWarnings("removal")
    public void testBackwardCompatibilityWithoutLimitations() {
        var response = SearchResponse.builder(List.of())
                                     .total(0)
                                     .missingCapabilities(List.of(SearchClient.Capability.HIGHLIGHT))
                                     .build();
        assertEquals(List.of(SearchClient.Capability.HIGHLIGHT), response.getMissingCapabilities());
        assertTrue(response.isMissingCapabilities());
        assertEquals(1, response.getLimitations().size());
        assertEquals(SearchClient.Capability.HIGHLIGHT, response.getLimitations().getFirst().getAffectedCapability());
    }

    @Test
    public void testEmptyResponseNoLimitations() {
        var response = SearchResponse.builder(List.of()).total(0).build();
        assertFalse(response.isMissingCapabilities());
        assertTrue(response.getMissingCapabilities().isEmpty());
        assertTrue(response.getLimitations().isEmpty());
    }
}
