/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.AbstractAuditStorageTest;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.ecm.platform.audit.api")
@Deploy("org.nuxeo.ecm.core.persistence")
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
public class TestAuditWithElasticSearch  extends AbstractAuditStorageTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchAdmin esa;

    @Before
    public void setupIndex() throws Exception {
        // make sure that the audit bulker don't drain pending log entries while we reset the index
        LogEntryGen.flushAndSync();
        esa.initIndexes(true);
    }

    @Test
    public void shouldUseESBackend() throws Exception {

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        assertNotNull(audit);

        AuditBackend backend = audit.getBackend();
        assertNotNull(backend);

        assertTrue(backend instanceof ESAuditBackend);
    }

    @Test
    public void shouldLogInAudit() throws Exception {
        // generate events
        DocumentModel doc = session.createDocumentModel("/", "a-file", "File");
        doc.setPropertyValue("dc:title", "A File");
        doc = session.createDocument(doc);

        LogEntryGen.flushAndSync();

        doc.setPropertyValue("dc:title", "A modified File");
        doc = session.saveDocument(doc);

        LogEntryGen.flushAndSync();

        // test audit trail
        AuditReader reader = Framework.getService(AuditReader.class);
        List<LogEntry> trail = reader.getLogEntriesFor(doc.getId(), doc.getRepositoryName());

        assertNotNull(trail);
        assertEquals(2, trail.size());

        LogEntry entry = trail.get(0);
        assertEquals(2L, entry.getId());
        assertEquals("documentModified", entry.getEventId());
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("A modified File", entry.getExtendedInfos().get("title").getValue(String.class));

        entry = trail.get(1);
        assertEquals(1L, entry.getId());
        assertEquals("documentCreated", entry.getEventId());
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("A File", entry.getExtendedInfos().get("title").getValue(String.class));

        LogEntry entryById = reader.getLogEntryByID(entry.getId());
        assertEquals(entry.getId(), entryById.getId());

        entryById = reader.getLogEntryByID(123L);
        assertNull(entryById);

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        AuditBackend backend = audit.getBackend();
        assertEquals(1L, backend.getEventsCount(entry.getEventId()).longValue());
    }

    @Test
    public void shouldSupportMultiCriteriaQueries() throws Exception {

        LogEntryGen.generate("mydoc", "evt", "cat", 9);

        AuditReader reader = Framework.getService(AuditReader.class);

        // simple Query
        String[] evts = { "evt1", "evt2" };
        List<LogEntry> res = reader.queryLogs(evts, null);
        assertNotNull(res);
        assertEquals(2, res.size());

        evts = new String[] { "evt1", };
        res = reader.queryLogs(evts, null);
        assertEquals(1, res.size());

        evts = new String[] { "evt", };
        res = reader.queryLogs(evts, null);
        assertEquals(0, res.size());

        // multi Query
        evts = new String[] { "evt1", "evt2" };
        String[] cats = { "cat1" };
        res = reader.queryLogsByPage(evts, (Date) null, cats, null, 0, 5);
        assertEquals(1, res.size());

        evts = new String[] { "evt1", "evt2" };
        cats = new String[] { "cat1", "cat0" };
        res = reader.queryLogsByPage(evts, (Date) null, cats, null, 0, 5);
        assertEquals(2, res.size());

        // test page size
        res = reader.queryLogsByPage(null, (Date) null, (String[]) null, "/mydoc", 0, 5);
        assertEquals(5, res.size());

        res = reader.queryLogsByPage(null, (Date) null, (String[]) null, "/mydoc", 1, 5);
        assertEquals(4, res.size());

    }

    @Test
    public void shouldSupportNativeQueries() throws Exception {

        LogEntryGen.generate("dummy", "entry", "category", 9);

        AuditReader reader = Framework.getService(AuditReader.class);

        String jsonQuery = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("filtredQuery.json"),
                "UTF-8");
        List<?> res = reader.nativeQuery(jsonQuery, 0, 5);

        assertEquals(2, res.size());

        jsonQuery = IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("filtredQueryWithParams.json"), "UTF-8");

        Map<String, Object> params = new HashMap<>();
        params.put("category", "category1");
        res = reader.nativeQuery(jsonQuery, params, 0, 5);

        assertEquals(1, res.size());

    }

    @Test
    public void testGetLatestLogId() throws Exception {
        String repositoryId = "test";
        AuditReader reader = Framework.getService(AuditReader.class);

        LogEntryGen.generate("mydoc", "documentModified", "cat", 1);
        long id1 = reader.getLatestLogId(repositoryId, "documentModified0");
        assertTrue("id: " + id1, id1 > 0);

        LogEntryGen.generate("mydoc", "documentCreated", "cat", 1);
        long id2 = reader.getLatestLogId(repositoryId, "documentModified0", "documentCreated0");
        assertTrue("id2: " + id2, id2 > 0);
        assertTrue(id2 > id1);

        long id = reader.getLatestLogId(repositoryId, "documentModified0");
        assertEquals(id1, id);
        id = reader.getLatestLogId(repositoryId, "unknown");
        assertEquals(0, id);
    }

    @Test
    public void testGetLogEntriesAfter() throws Exception {
        String repositoryId = "test";
        AuditReader reader = Framework.getService(AuditReader.class);

        LogEntryGen.generate("mydoc", "documentModified", "cat", 1);
        long id1 = reader.getLatestLogId(repositoryId, "documentModified0");

        LogEntryGen.generate("mydoc", "documentModified", "cat", 1);
        long id2 = reader.getLatestLogId(repositoryId, "documentModified0");
        assertTrue(id2 > id1);

        LogEntryGen.generate("mydoc", "documentModified", "cat", 1);
        long id3 = reader.getLatestLogId(repositoryId, "documentModified0");
        assertTrue(id3 > id2);

        LogEntryGen.generate("mydoc", "documentModified", "cat", 1);
        long id4 = reader.getLatestLogId(repositoryId, "documentModified0");
        assertTrue(id4 > id3);

        List<LogEntry> entries = reader.getLogEntriesAfter(id1, 5, repositoryId, "documentCreated0",
                "documentModified0");
        assertEquals(4, entries.size());
        assertEquals(id1, entries.get(0).getId());

        entries = reader.getLogEntriesAfter(id2, 2, repositoryId, "documentCreated0", "documentModified0");
        assertEquals(2, entries.size());
        assertEquals(id2, entries.get(0).getId());
        assertEquals(id3, entries.get(1).getId());
    }

    @Override
    @Test
    public void testStartsWith() throws Exception {
        super.testStartsWith();

        // A partial match is not supported by Elastic
        assertStartsWithCount(0, "/is/eve");
        assertStartsWithCount(0, "/is/od");
    }

    @Override
    protected void flush() throws Exception {
        LogEntryGen.flushAndSync();
    }
}
