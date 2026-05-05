/*
 * (C) Copyright 2017-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Tiry
 *     Kevin Leturc
 */
package org.nuxeo.audit.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.audit.test.test:OSGI-INF/test-pageprovider-track-contrib.xml")
public class TestAuditPageProviderTracking {

    @Inject
    protected AuditBackend backend;

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pps;

    @Inject
    protected AuditFeature auditFeature;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Test
    public void shouldLogPageProviderCallsInAudit() {

        PageProvider<?> pp = pps.getPageProvider(
                PageProviderSpec.builder("CURRENT_DOCUMENT_CHILDREN_TRACK")
                                .pageSize(10L)
                                .currentPage(0L)
                                .property(CORE_SESSION_PROPERTY, (Serializable) session)
                                .parameters(session.getRootDocument().getId())
                                .build());
        assertNotNull(pp);

        List<LogEntry> trail = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, "search")));

        assertEquals(0, trail.size());

        pp.getCurrentPage();
        transactionalFeature.nextTransaction();

        trail = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, "search")));
        assertEquals(1, trail.size());

        LogEntry entry = trail.get(0);

        assertEquals(session.getPrincipal().getName(), entry.getPrincipalName());

        assertEquals("search", entry.getEventId());

        assertEquals("CURRENT_DOCUMENT_CHILDREN_TRACK", entry.getExtendedValue("pageProviderName"));

        assertEquals(Long.valueOf(0L), entry.getExtendedValue("pageIndex"));

        assertEquals(Long.valueOf(0L), entry.getExtendedValue("resultsCountInPage"));

        assertTrue(entry.<List<String>> getExtendedValue("params").contains(session.getRootDocument().getId()));

        pp.getCurrentPage();
        transactionalFeature.nextTransaction();

        trail = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, "search")));
        assertEquals(2, trail.size());

    }

    @Test
    public void shouldLogPageProviderCallsAndSearchDocumentModelInAudit() {

        DocumentModel rootDoc = session.getRootDocument();
        PageProvider<?> pp = pps.getPageProvider(
                PageProviderSpec.builder("CURRENT_DOCUMENT_CHILDREN_SEARCH_DOCUMENT_TRACK")
                                .searchDocument(rootDoc)
                                .pageSize(2L)
                                .currentPage(0L)
                                .property(CORE_SESSION_PROPERTY, (Serializable) session)
                                .build());
        assertNotNull(pp);

        List<LogEntry> trail = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, "search")));

        assertEquals(0, trail.size());

        pp.getCurrentPage();
        transactionalFeature.nextTransaction();

        trail = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, "search")));
        assertEquals(1, trail.size());

        LogEntry entry = trail.get(0);
        assertEquals(session.getPrincipal().getName(), entry.getPrincipalName());
        assertEquals("search", entry.getEventId());

        assertEquals("CURRENT_DOCUMENT_CHILDREN_SEARCH_DOCUMENT_TRACK", entry.getExtendedValue("pageProviderName"));
        assertEquals(Long.valueOf(0L), entry.getExtendedValue("pageIndex"));
        assertEquals(Long.valueOf(0L), entry.getExtendedValue("resultsCountInPage"));

        if (auditFeature.isBackendSql()) {
            assertTrue("searchDocumentModel should be a String",
                    entry.getExtendedValue("searchDocumentModel") instanceof String);
            String searchDoc = entry.getExtendedValue("searchDocumentModel");
            assertTrue(searchDoc.contains(String.format("\"uid\":\"%s\"", rootDoc.getId())));
        } else {
            assertTrue("searchDocumentModel should be a real object",
                    entry.getExtendedValue("searchDocumentModel") instanceof Map);
            Map<String, Object> searchDoc = entry.getExtendedValue("searchDocumentModel");
            assertEquals(rootDoc.getId(), searchDoc.get("uid"));
        }

        pp.refresh(); // clear cache
        pp.getCurrentPage();
        transactionalFeature.nextTransaction();

        trail = backend.queryLogs(new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, "search")));
        assertEquals(2, trail.size());
    }

}
