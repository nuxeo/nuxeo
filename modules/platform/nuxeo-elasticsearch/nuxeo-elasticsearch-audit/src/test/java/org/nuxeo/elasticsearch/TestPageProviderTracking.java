/*
 * (C) Copyright 2014-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *
 */
package org.nuxeo.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.ecm.platform.audit.api")
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.ecm.platform.uidgen.core")
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.elasticsearch.seqgen")
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit")
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:audit-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:test-pageprovider-track-contrib.xml")
public class TestPageProviderTracking {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected PageProviderService pps;

    @Inject
    protected AuditReader reader;

    @Before
    public void setupIndex() throws Exception {
        // make sure that the audit bulker don't drain pending log entries while we reset the index
        LogEntryGen.flushAndSync();
        esa.initIndexes(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldLogPageProviderCallsInAudit() throws Exception {

        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("CURRENT_DOCUMENT_CHILDREN_TRACK", null, 10L, 0L, props,
                session.getRootDocument().getId());
        assertNotNull(pp);

        List<LogEntry> trail = reader.queryLogs(new String[] { "search" }, null);

        assertEquals(0, trail.size());

        pp.getCurrentPage();

        LogEntryGen.flushAndSync();
        trail = reader.queryLogs(new String[] { "search" }, null);
        assertEquals(1, trail.size());

        LogEntry entry = trail.get(0);

        assertEquals(session.getPrincipal().getName(), entry.getPrincipalName());

        assertEquals("search", entry.getEventId());

        assertEquals("CURRENT_DOCUMENT_CHILDREN_TRACK",
                entry.getExtendedInfos().get("pageProviderName").getSerializableValue());

        assertEquals(0L, entry.getExtendedInfos().get("pageIndex").getSerializableValue());

        assertEquals(0L, entry.getExtendedInfos().get("resultsCountInPage").getSerializableValue());

        assertTrue(((List<String>) entry.getExtendedInfos().get("params").getSerializableValue()).contains(
                session.getRootDocument().getId()));

        pp.getCurrentPage();

        LogEntryGen.flushAndSync();
        trail = reader.queryLogs(new String[] { "search" }, null);
        assertEquals(2, trail.size());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldLogPageProviderCallsAndSearchDocumentModelInAudit() throws Exception {

        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        DocumentModel rootDoc = session.getRootDocument();
        PageProvider<?> pp = pps.getPageProvider("CURRENT_DOCUMENT_CHILDREN_SEARCH_DOCUMENT_TRACK", rootDoc, null,
                Long.valueOf(2), Long.valueOf(0), props);
        assertNotNull(pp);

        List<LogEntry> trail = reader.queryLogs(new String[] { "search" }, null);

        assertEquals(0, trail.size());

        pp.getCurrentPage();

        LogEntryGen.flushAndSync();
        trail = reader.queryLogs(new String[] { "search" }, null);
        assertEquals(1, trail.size());

        LogEntry entry = trail.get(0);
        assertEquals(session.getPrincipal().getName(), entry.getPrincipalName());
        assertEquals("search", entry.getEventId());

        Map<String, ExtendedInfo> extended = entry.getExtendedInfos();
        assertEquals("CURRENT_DOCUMENT_CHILDREN_SEARCH_DOCUMENT_TRACK",
                extended.get("pageProviderName").getSerializableValue());
        assertEquals(0L, extended.get("pageIndex").getSerializableValue());
        assertEquals(0L, extended.get("resultsCountInPage").getSerializableValue());

        ExtendedInfo extSearchDoc = extended.get("searchDocumentModel");
        // TODO standardize audit backends - SQL and MongoDB have a String
        assertTrue("searchDocumentModel should be a real object", extSearchDoc.getSerializableValue() instanceof Map);
        Map<String, Object> searchDoc = (Map<String, Object>) extSearchDoc.getSerializableValue();
        assertEquals(rootDoc.getId(), searchDoc.get("uid"));

        pp.refresh(); // clear cache
        pp.getCurrentPage();

        LogEntryGen.flushAndSync();
        trail = reader.queryLogs(new String[] { "search" }, null);
        assertEquals(2, trail.size());
    }

}
