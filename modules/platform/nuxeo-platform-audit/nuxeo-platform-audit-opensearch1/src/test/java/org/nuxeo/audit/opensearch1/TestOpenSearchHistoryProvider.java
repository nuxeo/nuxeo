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
package org.nuxeo.audit.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ OpenSearchAuditFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.platform.audit.tests:test-audit-contrib.xml")
@Deploy("org.nuxeo.audit.opensearch1.test:OSGI-INF/opensearch-audit-pageprovider-test-contrib.xml")
public class TestOpenSearchHistoryProvider {

    private static final Logger log = LogManager.getLogger(TestOpenSearchHistoryProvider.class);

    public static final String CUSTOM_HISTORY_VIEW = "CUSTOM_HISTORY_VIEW";

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pageProviderService;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected AuditRouter auditRouter;

    protected DocumentModel folder;

    protected DocumentModel doc;

    protected DocumentModel proxy;

    protected List<DocumentModel> versions;

    protected Date t1;

    protected Date t2;

    @Before
    public void before() throws Exception {
        createTestEntries();
    }

    protected void createTestEntries() throws Exception {
        folder = session.createDocumentModel("/", "section", "Folder");
        folder = session.createDocument(folder);

        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "TestDoc");

        // create the doc
        doc = session.createDocument(doc);

        // wait at least 1s to be sure we have a precise timestamp in all DB
        // backend
        Thread.sleep(500);

        t1 = Date.from(Instant.now());

        Thread.sleep(600);

        // do some updates
        for (int i = 0; i < 5; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.putContextData("comment", "Update " + i);
            doc = session.saveDocument(doc);
        }

        // wait at least 1s to be sure we have a precise timestamp in all DB
        // backend
        Thread.sleep(600);

        t2 = Date.from(Instant.now());

        Thread.sleep(500);

        // create a version
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        doc = session.saveDocument(doc);

        // wait at least 1s to be sure we have a precise timestamp in all DB
        // backend
        Thread.sleep(1100);

        // do some more updates
        for (int i = 5; i < 10; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.putContextData("comment", "Update " + i);
            doc = session.saveDocument(doc);
        }

        // wait at least 1s to be sure we have a precise timestamp in all DB
        // backend
        Thread.sleep(1100);

        proxy = session.publishDocument(doc, folder);

        // wait at least 1s to be sure we have a precise timestamp in all DB
        // backend
        Thread.sleep(1100);

        // do some more updates
        for (int i = 10; i < 15; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.putContextData("comment", "Update " + i);
            doc = session.saveDocument(doc);
        }

        Thread.sleep(500);
        versions = session.getVersions(doc.getRef());
        assertEquals(2, versions.size());
        for (DocumentModel version : versions) {
            log.trace("version: {}", version::getId);
            log.trace("version series: {}", version::getVersionSeriesId);
            log.trace("version label: {}", version::getVersionLabel);
            log.trace("version date: {}", () -> ((Calendar) version.getPropertyValue("dc:modified")).getTime());
        }

        transactionalFeature.nextTransaction();

        // bonus entry !
        LogEntry createdEntry = LogEntry.builder("bonusEvent", new Date())
                                        .category("bonusCategory")
                                        .docUUID(doc.getId())
                                        .docPath(doc.getPathAsString())
                                        .repositoryId("test")
                                        .extended("reason", "test")
                                        .build();
        auditRouter.routeToBackends(List.of(createdEntry), List.of(Route.allEventsTo(DEFAULT_AUDIT_BACKEND)));

        transactionalFeature.nextTransaction();
    }

    @Test
    public void testCustomDocumentHistoryPageProvider() {
        assertNotNull(pageProviderService.getPageProviderDefinition(CUSTOM_HISTORY_VIEW));

        DocumentModel searchDoc = session.createDocumentModel("BasicAuditSearch");
        searchDoc.setPathInfo("/", "auditsearch");
        searchDoc = session.createDocument(searchDoc);
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        searchDoc.setPropertyValue("basicauditsearch:startDate", null);
        searchDoc.setPropertyValue("basicauditsearch:endDate", null);

        PageProvider<LogEntry> pageProvider = getPageProvider(CUSTOM_HISTORY_VIEW, 26, 0, "/");
        List<LogEntry> entries = pageProvider.getCurrentPage();
        entries.forEach(entry -> log.trace("LogEntry: {}", entry));

        // Folder: creation + proxy published + content published + proxy under it => total of 4 => docPath=/section/
        // File: 3 docs created (file + 2 versions), 15 update, 2 checkin, 1 bonus => total of 22 => docPath=/doc
        assertEquals(25, entries.size());

        pageProvider = getPageProvider(CUSTOM_HISTORY_VIEW, 4, 0, "/s");
        entries = pageProvider.getCurrentPage();
        assertEquals(4, entries.size());

        // section doc + proxy
        assertEquals(1, entries.stream().map(LogEntry::getDocUUID).distinct().filter(folder.getId()::equals).count());
        assertEquals(1, entries.stream().map(LogEntry::getDocUUID).distinct().filter(proxy.getId()::equals).count());

        Optional<String> optional = entries.stream().map(LogEntry::getDocUUID).distinct().findAny();
        assertEquals(folder.getId(), optional.get());

        pageProvider = getPageProvider(CUSTOM_HISTORY_VIEW, 26, 0, "/d");
        entries = pageProvider.getCurrentPage();
        assertEquals(21, entries.size());
        // file + 2 versions
        assertEquals(1, entries.stream().map(LogEntry::getDocUUID).distinct().filter(doc.getId()::equals).count());
        assertEquals(1,
                entries.stream().map(LogEntry::getDocUUID).distinct().filter(versions.get(0).getId()::equals).count());
        assertEquals(1,
                entries.stream().map(LogEntry::getDocUUID).distinct().filter(versions.get(1).getId()::equals).count());

        // filter by events ids
        searchDoc.setPropertyValue("basicauditsearch:eventIds", new String[] { "documentModified" });
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        pageProvider.setSearchDocumentModel(searchDoc);
        entries = pageProvider.getCurrentPage();
        assertEquals(15, entries.size());

        // filter on category
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", new String[] { "eventDocumentCategory" });
        pageProvider.setSearchDocumentModel(searchDoc);
        entries = pageProvider.getCurrentPage();
        assertEquals(20, entries.size());

        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", new String[] { "bonusCategory" });
        pageProvider.setSearchDocumentModel(searchDoc);
        entries = pageProvider.getCurrentPage();
        assertEquals(1, entries.size());

        // filter on Date
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        searchDoc.setPropertyValue("basicauditsearch:startDate", t1);
        searchDoc.setPropertyValue("basicauditsearch:endDate", t2);
        pageProvider.setSearchDocumentModel(searchDoc);
        entries = pageProvider.getCurrentPage();
        assertEquals(5, entries.size());
    }

    @Test
    public void testCustomFixedPartDocumentHistoryPageProvider() {
        assertNotNull(pageProviderService.getPageProviderDefinition("FIXED_PART_DOCUMENT_HISTORY_PROVIDER"));
        DocumentModel searchDoc = session.createDocumentModel("BasicAuditSearch");
        searchDoc.setPathInfo("/", "auditsearch");
        searchDoc = session.createDocument(searchDoc);

        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        searchDoc.setPropertyValue("basicauditsearch:startDate", null);
        searchDoc.setPropertyValue("basicauditsearch:endDate", null);

        // test with doc
        PageProvider<LogEntry> pageProvider = getPageProvider("FIXED_PART_DOCUMENT_HISTORY_PROVIDER", 30, 0, doc);
        pageProvider.setSearchDocumentModel(searchDoc);
        assertEquals(1, pageProvider.getCurrentPage().size());

        // test with proxy to check that the doc uuid is correctly set in the fixed part
        pageProvider = getPageProvider("FIXED_PART_DOCUMENT_HISTORY_PROVIDER", 30, 0, proxy);
        pageProvider.setSearchDocumentModel(searchDoc);
        assertTrue(pageProvider.getCurrentPage().isEmpty());
    }

    protected PageProvider<LogEntry> getPageProvider(String name, int pageSize, int currentPage, Object... parameters) {
        List<SortInfo> sorters = List.of(new SortInfo("id", true));
        @SuppressWarnings("unchecked")
        PageProvider<LogEntry> pageProvider = (PageProvider<LogEntry>) pageProviderService.getPageProvider(name,
                sorters, Long.valueOf(pageSize), Long.valueOf(currentPage), Map.of(), parameters);
        return pageProvider;
    }
}
