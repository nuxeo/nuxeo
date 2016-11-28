/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

@Deploy({ "org.nuxeo.ecm.platform.audit.api", "org.nuxeo.ecm.platform.audit", "org.nuxeo.ecm.platform.uidgen.core",
        "org.nuxeo.elasticsearch.seqgen",
        "org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit" })
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.audit:elasticsearch-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml",
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

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
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
        List<LogEntry> trail = reader.getLogEntriesFor(doc.getId());

        Assert.assertNotNull(trail);
        Assert.assertEquals(2, trail.size());

        LogEntry entry = trail.get(0);
        Long startId = entry.getId();
        Assert.assertEquals("documentCreated", entry.getEventId());
        Assert.assertEquals("eventDocumentCategory", entry.getCategory());
        Assert.assertEquals("A File", entry.getExtendedInfos().get("title").getValue(String.class));

        Assert.assertEquals(startId + 1, trail.get(1).getId());
        Assert.assertEquals("documentModified", trail.get(1).getEventId());
        Assert.assertEquals("eventDocumentCategory", trail.get(1).getCategory());
        Assert.assertEquals("A modified File", trail.get(1).getExtendedInfos().get("title").getValue(String.class));

        LogEntry entryById = reader.getLogEntryByID(entry.getId());
        Assert.assertEquals(entry.getId(), entryById.getId());

        entryById = reader.getLogEntryByID(123L);
        Assert.assertNull(entryById);

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
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

}
