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
 *     Thierry Delprat
 *     Kevin Leturc
 */
package org.nuxeo.mongodb.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Deploy({ "org.nuxeo.ecm.platform.query.api", "org.nuxeo.admin.center" })
@RunWith(FeaturesRunner.class)
@Features(MongoDBAuditFeature.class)
@LocalDeploy({ "org.nuxeo.mongodb.audit.test:OSGI-INF/mongodb-audit-pageprovider-test-contrib.xml" })
@SuppressWarnings("unchecked")
public class TestAuditHistoryProviderWithMongoDB {

    protected DocumentModel doc;

    protected DocumentModel proxy;

    protected List<DocumentModel> versions;

    protected Date t1;

    protected Date t2;

    protected boolean verbose = false;

    protected void dump(Object ob) {
        System.out.println(ob.toString());
    }

    protected void dump(List<?> obs) {
        for (Object ob : obs) {
            dump(ob);
        }
    }

    protected @Inject CoreSession session;

    protected void waitForAsyncCompletion() throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        assertTrue(Framework.getLocalService(AuditLogger.class).await(10, TimeUnit.SECONDS));
    }

    protected void createTestEntries() throws Exception {

        AuditReader reader = Framework.getLocalService(AuditReader.class);

        DocumentModel section = session.createDocumentModel("/", "section", "Folder");
        section = session.createDocument(section);

        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "TestDoc");

        // create the doc
        doc = session.createDocument(doc);

        // wait at least 1s to be sure we have a precise timestamp in all DB
        // backend
        Thread.sleep(500);

        t1 = new Date();

        Thread.sleep(600);

        // do some updates
        for (int i = 0; i < 5; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.putContextData("comment", "Update " + i);
            doc = session.saveDocument(doc);
            waitForAsyncCompletion();
        }

        // wait at least 1s to be sure we have a precise timestamp in all DB
        // backend
        Thread.sleep(600);

        t2 = new Date();

        Thread.sleep(500);
        // create a version
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        doc = session.saveDocument(doc);
        session.save();
        waitForAsyncCompletion();

        // wait at least 1s to be sure we have a precise timestamp in all DB
        // backend
        Thread.sleep(1100);

        // do some more updates
        for (int i = 5; i < 10; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.putContextData("comment", "Update " + i);
            doc = session.saveDocument(doc);
            session.save();
            waitForAsyncCompletion();
        }

        // wait at least 1s to be sure we have a precise timestamp in all DB
        // backend
        Thread.sleep(1100);

        proxy = session.publishDocument(doc, section);
        session.save();
        waitForAsyncCompletion();

        Thread.sleep(1100); // wait at least 1s to be sure we have a precise
                            // timestamp in all DB backend

        // do some more updates
        for (int i = 10; i < 15; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.putContextData("comment", "Update " + i);
            doc = session.saveDocument(doc);
            session.save();
        }

        Thread.sleep(500);

        waitForAsyncCompletion();

        versions = session.getVersions(doc.getRef());
        assertEquals(2, versions.size());
        if (verbose) {
            for (DocumentModel version : versions) {
                System.out.println("version: " + version.getId());
                System.out.println("version series: " + version.getVersionSeriesId());
                System.out.println("version label: " + version.getVersionLabel());
                System.out.println("version date: " + ((Calendar) version.getPropertyValue("dc:modified")).getTime());
            }
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

        List<LogEntry> entries = new ArrayList<>();
        entries.add(createdEntry);
        Framework.getLocalService(AuditLogger.class).addLogEntries(entries);

        LogEntryGen.flushAndSync();
        List<LogEntry> logs = reader.getLogEntriesFor(doc.getId(), doc.getRepositoryName());
        if (verbose) {
            dump(logs);
        }

        if (verbose) {
            String matchAll = "{}";
            logs = (List<LogEntry>) reader.nativeQuery(matchAll, 0, 30);
            System.out.println("Total entries = " + logs.size());
            dump(logs);
        }
    }

    @Test
    public void testDocumentHistoryPageProvider() throws Exception {

        createTestEntries();

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);
        PageProvider<?> pp;
        List<LogEntry> entries;

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("DOCUMENT_HISTORY_PROVIDER");
        assertNotNull(ppdef);
        long startIdx = 0;

        List<SortInfo> si = Collections.singletonList(new SortInfo("id", true));

        DocumentModel searchDoc = session.createDocumentModel("BasicAuditSearch");
        searchDoc.setPathInfo("/", "auditsearch");
        searchDoc = session.createDocument(searchDoc);

        for (String ppName : new String[] { "DOCUMENT_HISTORY_PROVIDER_OLD", "DOCUMENT_HISTORY_PROVIDER" }) {

            if (ppName.endsWith("OLD")) {
                pp = pps.getPageProvider(ppName, si, Long.valueOf(20), Long.valueOf(0), new HashMap<>(), doc.getId());
            } else {
                pp = pps.getPageProvider(ppName, si, Long.valueOf(20), Long.valueOf(0), new HashMap<>(), doc);
            }

            assertNotNull(pp);
            searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
            searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
            searchDoc.setPropertyValue("basicauditsearch:startDate", null);
            searchDoc.setPropertyValue("basicauditsearch:endDate", null);
            pp.setSearchDocumentModel(searchDoc);

            // Get Live doc history
            entries = (List<LogEntry>) pp.getCurrentPage();
            if (verbose) {
                System.out.println("Live doc history");
                dump(entries);
            }

            // create, 15+1 update , 2 checkin, 1 bonus
            assertEquals(20, entries.size());
            startIdx = entries.get(0).getId();

            // filter on eventId
            searchDoc.setPropertyValue("basicauditsearch:eventIds", new String[] { "documentModified" });
            searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
            pp.setSearchDocumentModel(searchDoc);
            entries = (List<LogEntry>) pp.getCurrentPage();
            assertEquals(16, entries.size());

            // filter on category
            searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
            searchDoc.setPropertyValue("basicauditsearch:eventCategories", new String[] { "eventDocumentCategory" });
            pp.setSearchDocumentModel(searchDoc);
            entries = (List<LogEntry>) pp.getCurrentPage();
            assertEquals(19, entries.size());

            // filter on category
            searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
            searchDoc.setPropertyValue("basicauditsearch:eventCategories",
                    new String[] { "eventDocumentCategory", "bonusCategory" });
            pp.setSearchDocumentModel(searchDoc);
            entries = (List<LogEntry>) pp.getCurrentPage();
            assertEquals(20, entries.size());

            // filter on Date !
            searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
            searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
            searchDoc.setPropertyValue("basicauditsearch:startDate", t1);
            searchDoc.setPropertyValue("basicauditsearch:endDate", t2);
            pp.setSearchDocumentModel(searchDoc);
            entries = (List<LogEntry>) pp.getCurrentPage();
            assertEquals(5, entries.size());
        }

        searchDoc.setPropertyValue("basicauditsearch:eventIds", null);
        searchDoc.setPropertyValue("basicauditsearch:eventCategories", null);
        searchDoc.setPropertyValue("basicauditsearch:startDate", null);
        searchDoc.setPropertyValue("basicauditsearch:endDate", null);

        // Get Proxy history

        pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", si, Long.valueOf(30), Long.valueOf(0), new HashMap<>(),
                proxy);
        pp.setSearchDocumentModel(searchDoc);

        entries = (List<LogEntry>) pp.getCurrentPage();
        if (verbose) {
            System.out.println("Proxy doc history");
            dump(entries);
        }

        // 19 - 5 updates + create + proxyPublished
        int proxyEntriesCount = 19 - 5 + 1 + 1;
        assertEquals(proxyEntriesCount, entries.size());

        assertEquals(Long.valueOf(startIdx).longValue(), entries.get(0).getId());
        assertEquals(Long.valueOf(startIdx + proxyEntriesCount + 1).longValue(),
                entries.get(proxyEntriesCount - 1).getId());

        // Get version 1 history
        pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", si, Long.valueOf(20), Long.valueOf(0), new HashMap<>(),
                versions.get(0));
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();

        if (verbose) {
            System.out.println("Version " + versions.get(0).getVersionLabel() + " doc history");
            dump(entries);
        }

        // creation + 5 updates + update + checkin + created
        int version1EntriesCount = 1 + 5 + 1 + 1 + 1;
        if (version1EntriesCount == entries.size()) {
            assertEquals(Long.valueOf(startIdx).longValue(), entries.get(0).getId());
            assertEquals(Long.valueOf(startIdx + version1EntriesCount - 1).longValue(),
                    entries.get(version1EntriesCount - 1).getId());
        } else {
            // because update even may be 1ms behind checkin/created !
            assertEquals(version1EntriesCount - 1, entries.size());
        }

        // get version 2 history
        pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", si, Long.valueOf(20), Long.valueOf(0), new HashMap<>(),
                versions.get(1));
        pp.setSearchDocumentModel(searchDoc);

        entries = (List<LogEntry>) pp.getCurrentPage();

        if (verbose) {
            System.out.println("Version " + versions.get(1).getVersionLabel() + " doc history");
            dump(entries);
        }

        // creation + 5x2 updates + checkin/update + checkin + created
        int versin2EntriesCount = 1 + 5 * 2 + 1 + 1 + 1 + 1;
        assertEquals(versin2EntriesCount, entries.size());
        assertEquals(Long.valueOf(startIdx).longValue(), entries.get(0).getId());
        assertEquals(Long.valueOf(startIdx + versin2EntriesCount).longValue(),
                entries.get(versin2EntriesCount - 1).getId());

    }

}
