/*
 * (C) Copyright 2014-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.audit.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.Route;
import org.nuxeo.audit.service.AuditRouter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test the {@code DOCUMENT_HISTORY_PROVIDER} page provider.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.audit.test.test:OSGI-INF/test-audit-history-pageprovider-route-contrib.xml")
public class TestAuditDocumentHistoryPageProvider {

    @Inject
    protected AuditRouter router;

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pps;

    @Inject
    protected TransactionalFeature transactionalFeature;

    protected DocumentModel doc;

    protected DocumentModel proxy;

    protected List<DocumentModel> versions;

    protected Date t1;

    protected Date t2;

    @Before
    public void createTestEntries() {
        DocumentModel section = session.createDocumentModel("/", "section", "Folder");
        section = session.createDocument(section);

        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "TestDoc");
        doc = session.createDocument(doc);

        transactionalFeature.nextTransaction();

        t1 = new Date();

        // do some updates
        for (int i = 0; i < 5; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.putContextData("comment", "Update " + i);
            doc = session.saveDocument(doc);
        }
        transactionalFeature.nextTransaction();

        t2 = new Date();

        // create a version
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        doc = session.saveDocument(doc);
        session.save();
        transactionalFeature.nextTransaction();

        // do some more updates
        for (int i = 5; i < 10; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.putContextData("comment", "Update " + i);
            doc = session.saveDocument(doc);
            session.save();
        }
        transactionalFeature.nextTransaction();

        proxy = session.publishDocument(doc, section);
        session.save();
        transactionalFeature.nextTransaction();

        // do some more updates
        for (int i = 10; i < 15; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.putContextData("comment", "Update " + i);
            doc = session.saveDocument(doc);
            session.save();
        }
        transactionalFeature.nextTransaction();

        versions = session.getVersions(doc.getRef());
        assertEquals(2, versions.size());

        // bonus entry !
        LogEntry createdEntry = LogEntry.builder("bonusEvent", new Date())
                                        .category("bonusCategory")
                                        .docUUID(doc.getId())
                                        .docPath(doc.getPathAsString())
                                        .repositoryId("test")
                                        .build();
        router.routeToBackends(List.of(createdEntry), List.of(Route.allEventsTo(DEFAULT_AUDIT_BACKEND)));

        transactionalFeature.nextTransaction();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDocumentHistoryPageProvider() {
        PageProvider<?> pp;
        List<LogEntry> entries;
        long startId;

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("DOCUMENT_HISTORY_PROVIDER");
        assertNotNull(ppdef);

        DocumentModel searchDoc = session.createDocumentModel("BasicAuditSearch");
        searchDoc.setPathInfo("/", "auditsearch");
        searchDoc = session.createDocument(searchDoc);

        List<SortInfo> sorts = List.of(new SortInfo(LOG_ID, false));

        pp = pps.getPageProvider(PageProviderSpec.builder("DOCUMENT_HISTORY_PROVIDER")
                                                 .sortInfos(sorts)
                                                 .pageSize(20L)
                                                 .currentPage(0L)
                                                 .parameters(doc)
                                                 .build());
        pp.setSearchDocumentModel(searchDoc);

        // Get Live doc history
        entries = (List<LogEntry>) pp.getCurrentPage();

        // create, 15 update , 2 checkin, 1 bonus
        assertEquals(19, entries.size());
        startId = entries.getLast().getId();

        // filter on eventId
        searchDoc.setPropertyValue("basicauditsearch:eventIds", new String[] { "documentModified" });
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(15, entries.size());

        // filter on category
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", new String[] { "eventDocumentCategory" });
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(18, entries.size());

        // filter on categories
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories",
                new String[] { "eventDocumentCategory", "bonusCategory" });
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(19, entries.size());

        // filter on Date !
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        searchDoc.setPropertyValue("basicauditsearch:startDate", t1);
        searchDoc.setPropertyValue("basicauditsearch:endDate", t2);
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(5, entries.size());

        // reset searchDoc
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        searchDoc.setPropertyValue("basicauditsearch:startDate", null);
        searchDoc.setPropertyValue("basicauditsearch:endDate", null);

        // Get Proxy history
        pp = pps.getPageProvider(PageProviderSpec.builder("DOCUMENT_HISTORY_PROVIDER")
                                                 .sortInfos(sorts)
                                                 .pageSize(20L)
                                                 .currentPage(0L)
                                                 .parameters(proxy)
                                                 .build());
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();

        // 18 - 5 updates + create + proxyPublished
        int proxyEntriesCount = 18 - 5 + 1 + 1;
        assertEquals(proxyEntriesCount, entries.size());

        assertEquals(startId, entries.getLast().getId());
        assertEquals(startId + proxyEntriesCount + 1, entries.getFirst().getId());

        // Get version 1 history
        pp = pps.getPageProvider(PageProviderSpec.builder("DOCUMENT_HISTORY_PROVIDER")
                                                 .sortInfos(sorts)
                                                 .pageSize(20L)
                                                 .currentPage(0L)
                                                 .parameters(versions.getFirst())
                                                 .build());
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();

        // creation + 5 updates + checkin + created
        int version1EntriesCount = 1 + 5 + 1 + 1;
        assertEquals(startId, entries.getLast().getId());
        assertEquals(startId + version1EntriesCount - 1, entries.getFirst().getId());

        // get version 2 history
        pp = pps.getPageProvider(PageProviderSpec.builder("DOCUMENT_HISTORY_PROVIDER")
                                                 .sortInfos(sorts)
                                                 .pageSize(20L)
                                                 .currentPage(0L)
                                                 .parameters(versions.get(1))
                                                 .build());
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();

        // creation + 5x2 updates + checkin + checkin + created
        int version2EntriesCount = 1 + 5 * 2 + 1 + 1 + 1;
        assertEquals(version2EntriesCount, entries.size());
        assertEquals(startId, entries.getLast().getId());
        assertEquals(startId + version2EntriesCount, entries.getFirst().getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDocumentHistoryOldPageProvider() {
        PageProvider<?> pp;
        List<LogEntry> entries;

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("DOCUMENT_HISTORY_PROVIDER_OLD");
        assertNotNull(ppdef);

        DocumentModel searchDoc = session.createDocumentModel("BasicAuditSearch");
        searchDoc.setPathInfo("/", "auditsearch");
        searchDoc = session.createDocument(searchDoc);

        List<SortInfo> sorts = List.of(new SortInfo(LOG_ID, false));

        pp = pps.getPageProvider(PageProviderSpec.builder("DOCUMENT_HISTORY_PROVIDER_OLD")
                                                 .sortInfos(sorts)
                                                 .pageSize(20L)
                                                 .currentPage(0L)
                                                 .parameters(doc.getId())
                                                 .build());
        pp.setSearchDocumentModel(searchDoc);

        // Get Live doc history
        entries = (List<LogEntry>) pp.getCurrentPage();

        // create, 15 update , 2 checkin, 1 bonus
        assertEquals(19, entries.size());

        // filter on eventId
        searchDoc.setPropertyValue("basicauditsearch:eventIds", new String[] { "documentModified" });
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(15, entries.size());

        // filter on category
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", new String[] { "eventDocumentCategory" });
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(18, entries.size());

        // filter on categories
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories",
                new String[] { "eventDocumentCategory", "bonusCategory" });
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(19, entries.size());

        // filter on Date !
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        searchDoc.setPropertyValue("basicauditsearch:startDate", t1);
        searchDoc.setPropertyValue("basicauditsearch:endDate", t2);
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(5, entries.size());
    }
}
