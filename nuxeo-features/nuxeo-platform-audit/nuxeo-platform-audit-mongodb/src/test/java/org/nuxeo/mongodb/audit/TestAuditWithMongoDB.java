/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.mongodb.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_ID;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Predicates;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(FeaturesRunner.class)
@Features(MongoDBAuditFeature.class)
public class TestAuditWithMongoDB {

    @Inject
    protected CoreSession session;

    @Test
    public void shouldUseMongoDBBackend() throws Exception {

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        assertNotNull(audit);

        AuditBackend backend = audit.getBackend();
        assertTrue(backend instanceof MongoDBAuditBackend);
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

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        Assert.assertTrue(Framework.getService(AuditLogger.class).await(10, TimeUnit.SECONDS));

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

        String jsonQuery;
        AuditReader reader = Framework.getService(AuditReader.class);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("query.json")) {
            jsonQuery = IOUtils.toString(is, "UTF-8");
        }
        List<?> res = reader.nativeQuery(jsonQuery, 0, 5);

        assertEquals(2, res.size());

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("queryWithParams.json")) {
            jsonQuery = IOUtils.toString(is, "UTF-8");
        }

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
        AuditReader reader = Framework.getService(AuditReader.class);

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

        List<LogEntry> entries = reader.getLogEntriesAfter(id1, 5, repositoryId, "documentCreated0",
                "documentModified0");
        Assert.assertEquals(4, entries.size());
        Assert.assertEquals(id1, entries.get(0).getId());

        entries = reader.getLogEntriesAfter(id2, 2, repositoryId, "documentCreated0", "documentModified0");
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(id2, entries.get(0).getId());
        Assert.assertEquals(id3, entries.get(1).getId());
    }

    @Test
    public void testSaveAndScroll() throws Exception {

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        assertNotNull(audit);

        MongoDBAuditBackend mongoDBBackend = (MongoDBAuditBackend) audit.getBackend();

        String idForAuditStorage = "idForAuditStorage";
        ObjectMapper mapper = new ObjectMapper();
        List<String> jsonEntries = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 42; i++) {
            ObjectNode logEntryJson = mapper.createObjectNode();
            logEntryJson.put(LOG_ID, Integer.valueOf(i).longValue());
            logEntryJson.put(LOG_EVENT_ID, idForAuditStorage);
            jsonEntries.add(mapper.writeValueAsString(logEntryJson));
            ids.add(Long.valueOf(i));
        }
        // Save JSON entries into backend
        mongoDBBackend.append(jsonEntries);

        LogEntryGen.flushAndSync();

        // Query all logs
        AuditQueryBuilder builder = new AuditQueryBuilder().predicates(Predicates.eq(LOG_EVENT_ID, idForAuditStorage));
        // builder.predicates()
        List<LogEntry> logs = mongoDBBackend.queryLogs(builder);
        assertEquals(42, logs.size());

        ScrollResult<String> scrollResult = mongoDBBackend.scroll(builder, 5, 10);
        int total = 0;
        while (scrollResult.hasResults()) {
            assertTrue(scrollResult.getResults().size() <= 5);
            jsonEntries = scrollResult.getResults();
            List<LogEntry> entries = jsonEntries.stream().map(json -> {
                try {
                    return mapper.readValue(json, LogEntryImpl.class);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).collect(Collectors.toList());
            for (LogEntry entry : entries) {
                assertTrue(ids.remove(Long.valueOf(entry.getId())));
                assertEquals(idForAuditStorage, entry.getEventId());
            }
            total += entries.size();
            scrollResult = mongoDBBackend.scroll(scrollResult.getScrollId());
        }
        assertEquals(42, total);
        assertTrue(ids.isEmpty());

        // assert we can get a single log entry by its id
        LogEntry logEntry = mongoDBBackend.getLogEntryByID(4);
        assertNotNull(logEntry);
        assertEquals(4, logEntry.getId());
        assertEquals(idForAuditStorage, logEntry.getEventId());
    }

}
