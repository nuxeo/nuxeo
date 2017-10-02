/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
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
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Deploy({ "org.nuxeo.runtime.metrics", "org.nuxeo.ecm.platform.audit.api", "org.nuxeo.ecm.platform.audit",
        "org.nuxeo.ecm.platform.uidgen.core", "org.nuxeo.elasticsearch.core", "org.nuxeo.elasticsearch.seqgen",
        "org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml",
        "org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit" })
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:audit-test-contrib.xml" })
public class TestAuditWithElasticSearch {

    protected @Inject CoreSession session;

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
        Assert.assertNotNull(audit);

        AuditBackend backend = audit.getBackend();
        Assert.assertNotNull(backend);

        Assert.assertTrue(backend instanceof ESAuditBackend);
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
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> trail = reader.getLogEntriesFor(doc.getId(), doc.getRepositoryName());

        Assert.assertNotNull(trail);
        Assert.assertEquals(2, trail.size());

        LogEntry entry = trail.get(0);
        Assert.assertEquals(2L, entry.getId());
        Assert.assertEquals("documentModified", entry.getEventId());
        Assert.assertEquals("eventDocumentCategory", entry.getCategory());
        Assert.assertEquals("A modified File", entry.getExtendedInfos().get("title").getValue(String.class));

        entry = trail.get(1);
        Assert.assertEquals(1L, entry.getId());
        Assert.assertEquals("documentCreated", entry.getEventId());
        Assert.assertEquals("eventDocumentCategory", entry.getCategory());
        Assert.assertEquals("A File", entry.getExtendedInfos().get("title").getValue(String.class));


        LogEntry entryById = reader.getLogEntryByID(entry.getId());
        Assert.assertEquals(entry.getId(), entryById.getId());

        entryById = reader.getLogEntryByID(123L);
        Assert.assertNull(entryById);

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        AuditBackend backend = audit.getBackend();
        Assert.assertEquals(1L, backend.getEventsCount(entry.getEventId()).longValue());
    }

    @Test
    public void shouldSupportMultiCriteriaQueries() throws Exception {

        LogEntryGen.generate("mydoc", "evt", "cat", 9);

        AuditReader reader = Framework.getLocalService(AuditReader.class);

        // simple Query
        String[] evts = { "evt1", "evt2" };
        List<LogEntry> res = reader.queryLogs(evts, null);
        Assert.assertNotNull(res);
        Assert.assertEquals(2, res.size());

        evts = new String[] { "evt1", };
        res = reader.queryLogs(evts, null);
        Assert.assertEquals(1, res.size());

        evts = new String[] { "evt", };
        res = reader.queryLogs(evts, null);
        Assert.assertEquals(0, res.size());

        // multi Query
        evts = new String[] { "evt1", "evt2" };
        String[] cats = { "cat1" };
        res = reader.queryLogsByPage(evts, (Date) null, cats, null, 0, 5);
        Assert.assertEquals(1, res.size());

        evts = new String[] { "evt1", "evt2" };
        cats = new String[] { "cat1", "cat0" };
        res = reader.queryLogsByPage(evts, (Date) null, cats, null, 0, 5);
        Assert.assertEquals(2, res.size());

        // test page size
        res = reader.queryLogsByPage((String[]) null, (Date) null, (String[]) null, "/mydoc", 0, 5);
        Assert.assertEquals(5, res.size());

        res = reader.queryLogsByPage((String[]) null, (Date) null, (String[]) null, "/mydoc", 1, 5);
        Assert.assertEquals(4, res.size());

    }

    @Test
    public void shouldSupportNativeQueries() throws Exception {

        LogEntryGen.generate("dummy", "entry", "category", 9);

        AuditReader reader = Framework.getLocalService(AuditReader.class);

        String jsonQuery = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("filtredQuery.json"),
                "UTF-8");
        List<?> res = reader.nativeQuery(jsonQuery, 0, 5);

        Assert.assertEquals(2, res.size());

        jsonQuery = IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("filtredQueryWithParams.json"), "UTF-8");

        Map<String, Object> params = new HashMap<>();
        params.put("category", "category1");
        res = reader.nativeQuery(jsonQuery, params, 0, 5);

        Assert.assertEquals(1, res.size());

    }

    @Test
    public void testGetLatestLogId() throws Exception {
        String repositoryId = "test";
        AuditReader reader = Framework.getLocalService(AuditReader.class);

        LogEntryGen.generate("mydoc", "documentModified", "cat", 1);
        long id1 = reader.getLatestLogId(repositoryId, "documentModified0");
        Assert.assertTrue("id: " + id1, id1 > 0);

        LogEntryGen.generate("mydoc", "documentCreated", "cat", 1);
        long id2 = reader.getLatestLogId(repositoryId, "documentModified0", "documentCreated0");
        Assert.assertTrue("id2: " + id2, id2 > 0);
        Assert.assertTrue(id2 > id1);

        long id = reader.getLatestLogId(repositoryId, "documentModified0");
        Assert.assertEquals(id1, id);
        id = reader.getLatestLogId(repositoryId, "unknown");
        Assert.assertEquals(0, id);
    }


    @Test
    public void testGetLogEntriesAfter() throws Exception {
        String repositoryId = "test";
        AuditReader reader = Framework.getLocalService(AuditReader.class);

        LogEntryGen.generate("mydoc", "documentModified", "cat", 1);
        long id1 = reader.getLatestLogId(repositoryId, "documentModified0");

        LogEntryGen.generate("mydoc", "documentModified", "cat", 1);
        long id2 = reader.getLatestLogId(repositoryId, "documentModified0");
        Assert.assertTrue(id2 > id1);

        LogEntryGen.generate("mydoc", "documentModified", "cat", 1);
        long id3 = reader.getLatestLogId(repositoryId, "documentModified0");
        Assert.assertTrue(id3 > id2);

        LogEntryGen.generate("mydoc", "documentModified", "cat", 1);
        long id4 = reader.getLatestLogId(repositoryId, "documentModified0");
        Assert.assertTrue(id4 > id3);

        List<LogEntry> entries = reader.getLogEntriesAfter(id1, 5, repositoryId, "documentCreated0", "documentModified0");
        Assert.assertEquals(4, entries.size());
        Assert.assertEquals(id1, entries.get(0).getId());

        entries = reader.getLogEntriesAfter(id2, 2, repositoryId, "documentCreated0", "documentModified0");
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(id2, entries.get(0).getId());
        Assert.assertEquals(id3, entries.get(1).getId());
    }


}
