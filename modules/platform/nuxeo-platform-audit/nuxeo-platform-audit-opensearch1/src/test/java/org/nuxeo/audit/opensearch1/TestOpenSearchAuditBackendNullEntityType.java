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
 *     Guillaume Renard
 */
package org.nuxeo.audit.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.LogEntryList;
import org.nuxeo.audit.service.AuditComponent;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.runtime.opensearch1.OpenSearchClientService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.xcontent.XContentType;

/**
 * @since 2025.24
 */
@RunWith(FeaturesRunner.class)
@Features(OpenSearchAuditFeature.class)
@Deploy("org.nuxeo.ecm.platform.audit.tests:test-audit-contrib.xml")
@Deploy("org.nuxeo.audit.opensearch1.test:OSGI-INF/opensearch-audit-pageprovider-test-contrib.xml")
@SuppressWarnings("unchecked")
public class TestOpenSearchAuditBackendNullEntityType {

    // Simulate a legacy audit entry: entity-type is null, as produced by some code paths using ESAuditBackend
    protected static final String LEGACY_ENTRY_NULL_ENTITY_TYPE = """
            {"id": 1, "entity-type": null, "eventId": "legacyEvent", "eventDate": "2024-01-01T00:00:00.000Z"}
            """;

    // Simulate an entry with entity-type field entirely absent
    protected static final String LEGACY_ENTRY_MISSING_ENTITY_TYPE = """
            {"id": 2, "eventId": "legacyEventNoField", "eventDate": "2024-01-01T00:00:00.000Z"}
            """;

    @Inject
    protected AuditService auditService;

    @Inject
    protected OpenSearchClientService clientService;

    protected OpenSearchAuditBackend backend() {
        return auditService.getAuditBackend(AuditComponent.DEFAULT_AUDIT_BACKEND);
    }

    @Inject
    protected PageProviderService pps;

    @Inject
    protected TransactionalFeature transactionalFeature;

    /**
     * Indexes a raw JSON audit entry directly via the OpenSearch client, bypassing the backend write API. This
     * simulates legacy entries (e.g. produced by {@code ESAuditBackend.addLogEntries()}) that were persisted without a
     * valid {@code entity-type}.
     */
    protected void indexRawEntry(String json, String docId) {
        var client = clientService.getClient("audit/default");
        var bulkRequest = new BulkRequest();
        bulkRequest.add(new IndexRequest(backend().getIndexName()).id(docId).source(json.strip(), XContentType.JSON));
        client.bulk(bulkRequest);
    }

    @Test
    public void testQueryLogsWithNullEntityType() {
        indexRawEntry(LEGACY_ENTRY_NULL_ENTITY_TYPE, "1");
        transactionalFeature.nextTransaction();

        LogEntryList entries = backend().queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, "legacyEvent")));
        assertEquals(1, entries.size());
        assertEquals("legacyEvent", entries.getFirst().getEventId());
    }

    @Test
    public void testQueryLogsWithMissingEntityType() {
        indexRawEntry(LEGACY_ENTRY_MISSING_ENTITY_TYPE, "2");
        transactionalFeature.nextTransaction();

        LogEntryList entries = backend().queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, "legacyEventNoField")));
        assertEquals(1, entries.size());
        assertEquals("legacyEventNoField", entries.getFirst().getEventId());
    }

    @Test
    public void testGetLogEntryByIDWithNullEntityType() {
        indexRawEntry(LEGACY_ENTRY_NULL_ENTITY_TYPE, "1");
        transactionalFeature.nextTransaction();

        var entry = backend().getLogEntryByID(1L);
        assertEquals("legacyEvent", entry.getEventId());
    }

    @Test
    public void testGetLogEntryByIDWithMissingEntityType() {
        indexRawEntry(LEGACY_ENTRY_MISSING_ENTITY_TYPE, "2");
        transactionalFeature.nextTransaction();

        var entry = backend().getLogEntryByID(2L);
        assertEquals("legacyEventNoField", entry.getEventId());
    }

    @Test
    public void testPageProviderWithNullEntityType() {
        indexRawEntry(LEGACY_ENTRY_NULL_ENTITY_TYPE, "1");
        transactionalFeature.nextTransaction();

        PageProvider<?> pp = pps.getPageProvider(
                PageProviderSpec.builder("ADMIN_HISTORY").pageSize(10L).currentPage(0L).build());
        var entries = (List<LogEntry>) pp.getCurrentPage();
        assertTrue(entries.stream().anyMatch(e -> "legacyEvent".equals(e.getEventId())));
    }

    @Test
    public void testPageProviderWithMissingEntityType() {
        indexRawEntry(LEGACY_ENTRY_MISSING_ENTITY_TYPE, "2");
        transactionalFeature.nextTransaction();

        PageProvider<?> pp = pps.getPageProvider(
                PageProviderSpec.builder("ADMIN_HISTORY").pageSize(10L).currentPage(0L).build());
        var entries = (List<LogEntry>) pp.getCurrentPage();
        assertTrue(entries.stream().anyMatch(e -> "legacyEventNoField".equals(e.getEventId())));
    }
}
