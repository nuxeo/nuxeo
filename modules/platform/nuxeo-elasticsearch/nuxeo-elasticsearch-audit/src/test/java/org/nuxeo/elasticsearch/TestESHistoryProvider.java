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
 *     Thierry Delprat
 */
package org.nuxeo.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(RepositoryElasticSearchFeature.class)
@Deploy("org.nuxeo.ecm.platform.audit.api")
@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.ecm.platform.uidgen.core")
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.elasticsearch.seqgen")
@Deploy("org.nuxeo.elasticsearch.audit")
@Deploy("org.nuxeo.admin.center")
@Deploy("org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:audit-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:es-audit-pageprovider-test-contrib.xml")
public class TestESHistoryProvider {

    private static final Logger log = LogManager.getLogger(TestESHistoryProvider.class);

    public static final String CUSTOM_HISTORY_VIEW = "CUSTOM_HISTORY_VIEW";

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pageProviderService;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected AuditReader auditReader;

    @Inject
    protected AuditLogger auditLogger;

    @Inject
    protected ElasticSearchAdmin esa;

    protected DocumentModel folder;

    protected DocumentModel doc;

    protected DocumentModel proxy;

    protected List<DocumentModel> versions;

    protected Date t1;

    protected Date t2;

    @Before
    public void before() throws Exception {
        LogEntryGen.flushAndSync();
        esa.initIndexes(true);

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

        LogEntryGen.flushAndSync();

        // bonus entry !
        LogEntry createdEntry = new LogEntryImpl();
        createdEntry.setEventId("bonusEvent");
        createdEntry.setCategory("bonusCategory");
        createdEntry.setDocUUID(doc.getId());
        createdEntry.setEventDate(new Date());
        createdEntry.setDocPath(doc.getPathAsString());
        createdEntry.setRepositoryId("test");
        Map<String, ExtendedInfo> extendedInfos = new HashMap<>();
        extendedInfos.put("reason",new ExtendedInfoImpl.StringInfo("test"));
        createdEntry.setExtendedInfos(extendedInfos);

        auditLogger.addLogEntries(List.of(createdEntry));

        LogEntryGen.flushAndSync();
        transactionalFeature.nextTransaction();
        List<LogEntry> logs = auditReader.getLogEntriesFor(doc.getId(), doc.getRepositoryName());
        logs.forEach(entry -> log.trace("LogEntry: {}", entry));
    }

    @Test
    public void testDocumentHistoryPageProvider() {
        assertNotNull(pageProviderService.getPageProviderDefinition("DOCUMENT_HISTORY_PROVIDER"));
        long startIdx = 0;

        DocumentModel searchDoc = session.createDocumentModel("BasicAuditSearch");
        searchDoc.setPathInfo("/", "auditsearch");
        searchDoc = session.createDocument(searchDoc);

        PageProvider<LogEntry> pageProvider;
        List<LogEntry> entries;
        var pageProviderConf = Map.of("DOCUMENT_HISTORY_PROVIDER_OLD", doc.getId(), "DOCUMENT_HISTORY_PROVIDER", doc);
        for (var entry : pageProviderConf.entrySet()) {
            pageProvider = getPageProvider(entry.getKey(), 20, 0, entry.getValue());
            assertNotNull(pageProvider);

            // find all (create, 15+1 update , 2 checkin, 1 bonus)
            searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
            searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
            searchDoc.setPropertyValue("basicauditsearch:startDate", null);
            searchDoc.setPropertyValue("basicauditsearch:endDate", null);
            pageProvider.setSearchDocumentModel(searchDoc);

            // Get Live doc history
            entries = pageProvider.getCurrentPage();
            log.trace("Live doc history");
            entries.forEach(e -> log.trace("LogEntry: {}", e));

            // create, 15+1 update , 2 checkin, 1 bonus
            assertEquals(20, entries.size());
            startIdx = entries.get(0).getId();

            // filter on eventId
            searchDoc.setPropertyValue("basicauditsearch:eventIds", new String[] { "documentModified" });
            searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
            pageProvider.setSearchDocumentModel(searchDoc);
            entries = pageProvider.getCurrentPage();
            assertEquals(16, entries.size());

            // filter on category
            searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
            searchDoc.setPropertyValue("basicauditsearch:eventCategories", new String[] { "eventDocumentCategory" });
            pageProvider.setSearchDocumentModel(searchDoc);
            entries = pageProvider.getCurrentPage();
            assertEquals(19, entries.size());

            // filter on category
            searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
            searchDoc.setPropertyValue("basicauditsearch:eventCategories",
                    new String[] { "eventDocumentCategory", "bonusCategory" });
            pageProvider.setSearchDocumentModel(searchDoc);
            entries = pageProvider.getCurrentPage();
            assertEquals(20, entries.size());

            // filter on date !
            searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
            searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
            searchDoc.setPropertyValue("basicauditsearch:startDate", t1);
            searchDoc.setPropertyValue("basicauditsearch:endDate", t2);
            pageProvider.setSearchDocumentModel(searchDoc);
            entries = pageProvider.getCurrentPage();
            assertEquals(5, entries.size());
        }

        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        searchDoc.setPropertyValue("basicauditsearch:startDate", null);
        searchDoc.setPropertyValue("basicauditsearch:endDate", null);

        // Get Proxy history
        pageProvider = getPageProvider("DOCUMENT_HISTORY_PROVIDER", 30, 0, proxy);
        pageProvider.setSearchDocumentModel(searchDoc);

        entries = pageProvider.getCurrentPage();
        log.trace("Proxy doc history");
        entries.forEach(entry -> log.trace("LogEntry: {}", entry));

        // 19 - 5 updates + create + proxyPublished
        int proxyEntriesCount = 19 - 5 + 1 + 1;
        assertEquals(proxyEntriesCount, entries.size());

        assertEquals(Long.valueOf(startIdx).longValue(), entries.get(0).getId());
        assertEquals(Long.valueOf(startIdx + proxyEntriesCount + 1).longValue(),
                entries.get(proxyEntriesCount - 1).getId());

        // Get version 1 history
        pageProvider = getPageProvider("DOCUMENT_HISTORY_PROVIDER", 20, 0, versions.get(0));
        pageProvider.setSearchDocumentModel(searchDoc);
        entries = pageProvider.getCurrentPage();

        log.trace("Version {} doc history", () -> versions.get(0).getVersionLabel());
        entries.forEach(e -> log.trace("LogEntry: {}", e));

        // creation + 5 updates + update + checkin + created
        int version1EntriesCount = 1 + 5 + 1 + 1 + 1;
        if (version1EntriesCount == entries.size()) {
            assertEquals(Long.valueOf(startIdx).longValue(), entries.get(0).getId());
            assertEquals(Long.valueOf(startIdx + version1EntriesCount - 1).longValue(),
                    entries.get(version1EntriesCount - 1).getId());
        } else {
            // because update even may be 1ms behind checkin/created
            assertEquals(version1EntriesCount - 1, entries.size());
        }

        // get version 2 history
        pageProvider = getPageProvider("DOCUMENT_HISTORY_PROVIDER", 20, 0, versions.get(1));
        pageProvider.setSearchDocumentModel(searchDoc);

        entries = pageProvider.getCurrentPage();

        log.trace("Version {} doc history", () -> versions.get(1).getVersionLabel());
        entries.forEach(e -> log.trace("LogEntry: {}", e));

        // creation + 5x2 updates + checkin/update + checkin + created
        int version2EntriesCount = 1 + 5 * 2 + 1 + 1 + 1 + 1;
        assertEquals(version2EntriesCount, entries.size());
        assertEquals(Long.valueOf(startIdx).longValue(), entries.get(0).getId());
        assertEquals(Long.valueOf(startIdx + version2EntriesCount).longValue(),
                entries.get(version2EntriesCount - 1).getId());
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
        // File: 3 docs created (file + 2 versions), 15+1 update, 2 checkin, 1 bonus => total of 22 => docPath=/doc
        assertEquals(26, entries.size());

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
        assertEquals(22, entries.size());
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
        assertEquals(16, entries.size());

        // filter on category
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", new String[] { "eventDocumentCategory" });
        pageProvider.setSearchDocumentModel(searchDoc);
        entries = pageProvider.getCurrentPage();
        assertEquals(21, entries.size());

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

        PageProvider<LogEntry> pageProvider;
        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        searchDoc.setPropertyValue("basicauditsearch:startDate", null);
        searchDoc.setPropertyValue("basicauditsearch:endDate", null);

        //test with doc
        pageProvider = getPageProvider("FIXED_PART_DOCUMENT_HISTORY_PROVIDER", 30, 0, doc);
        pageProvider.setSearchDocumentModel(searchDoc);
        assertEquals(1,pageProvider.getCurrentPage().size());

        //test with proxy to check that the doc uuid is correctly set in the fixed part
        pageProvider = getPageProvider("FIXED_PART_DOCUMENT_HISTORY_PROVIDER", 30, 0, proxy);
        pageProvider.setSearchDocumentModel(searchDoc);
        assertEquals(0,pageProvider.getCurrentPage().size());
    }

    protected PageProvider<LogEntry> getPageProvider(String name, int pageSize, int currentPage, Object... parameters) {
        List<SortInfo> sorters = List.of(new SortInfo("id", true));
        @SuppressWarnings("unchecked")
        PageProvider<LogEntry> pageProvider = (PageProvider<LogEntry>) pageProviderService.getPageProvider(name,
                sorters, Long.valueOf(pageSize), Long.valueOf(currentPage), Map.of(), parameters);
        return pageProvider;
    }
}
