/*
 * (C) Copyright 2025-2026 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.search.SearchClient.Capability.AGGREGATE;
import static org.nuxeo.ecm.core.search.SearchClient.Capability.HIGHLIGHT;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateTerm;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestSearchInvalidQueries {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected SearchIndexingService searchIndexingService;

    @Test
    public void testValidNxql() {
        var response = search("SELECT * FROM Document");
        assertFalse(response.isMissingCapabilities());
        response = search("SELECT * FROM Document WHERE dc:modified < NOW('-P1D')");
        assertFalse(response.isMissingCapabilities());
    }

    @Test
    public void testInvalidNxql() {
        // invalid nxql raises QueryParseException
        assertThrows(QueryParseException.class, () -> search("This is not an NXQL query"));
        assertThrows(QueryParseException.class, () -> search("SELECT * FROM Document; SELECT * FROM Document"));
        assertThrows(QueryParseException.class, () -> search("SELECT * FROM UnknownDocumentType"));
        // assertThrows(QueryParseException.class, () -> search("SELECT now(), ecm:uuid FROM Document"));
        assertThrows(QueryParseException.class,
                () -> search("SELECT * FROM Document WHERE dc:modified < now('INVALID')"));
        assertThrows(QueryParseException.class,
                () -> search("SELECT * FROM Document WHERE dc:modified < UNKNOWN_FUNCTION('-P1D')"));
        assertThrows(QueryParseException.class,
                () -> search("SELECT * FROM Document WHERE dc:modified ! 'unknown operation'"));
        assertThrows(QueryParseException.class,
                () -> search("SELECT * FROM Document WHERE dc:modified UNKNOWN 'unknown operation'"));
    }

    @Test
    public void testInvalidAggregate() {
        // Creating an invalid agg raises IllegalArgumentException
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("UnknownType");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("dc:source");
        aggDef.setProperty("size", "1");
        assertThrows(IllegalArgumentException.class, () -> new AggregateTerm(aggDef, null));
    }

    @Test
    public void testQueryOnMissingCapability() {
        var response = searchService.search(
                SearchQuery.builder("SELECT * FROM Document", session).addHighlight("dc:title").build());
        boolean highlightPresent = !response.getMissingCapabilities().contains(HIGHLIGHT);
        assertEquals(response.toString(), hasCapability(HIGHLIGHT), highlightPresent);
        if (!highlightPresent) {
            assertTrue(response.isMissingCapabilities());
        }
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("terms");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("dc:source");
        aggDef.setProperty("size", "1");
        AggregateTerm agg = new AggregateTerm(aggDef, null);
        response = searchService.search(
                SearchQuery.builder("SELECT * FROM Document", session).addAggregate(agg).build());
        boolean aggregatePresent = !response.getMissingCapabilities().contains(AGGREGATE);
        assertEquals(response.toString(), hasCapability(AGGREGATE), aggregatePresent);

        response = searchService.search(SearchQuery.builder("SELECT * FROM Document", session)
                                                   .addHighlight("dc:title")
                                                   .addAggregate(agg)
                                                   .build());
        highlightPresent = !response.getMissingCapabilities().contains(HIGHLIGHT);
        assertEquals(response.toString(), hasCapability(HIGHLIGHT), highlightPresent);
        aggregatePresent = !response.getMissingCapabilities().contains(AGGREGATE);
        assertEquals(response.toString(), hasCapability(AGGREGATE), aggregatePresent);
    }

    protected boolean hasCapability(SearchClient.Capability capability) {
        var searchIndex = searchService.getSearchIndex(searchService.getDefaultIndexName());
        return searchIndexingService.getClient(searchIndex.client()).hasCapability(capability);
    }

    protected SearchResponse search(String nxql) {
        return searchService.search(SearchQuery.builder(nxql, session).build());
    }
}
