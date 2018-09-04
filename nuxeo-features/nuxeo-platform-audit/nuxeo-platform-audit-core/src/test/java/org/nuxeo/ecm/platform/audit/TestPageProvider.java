/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditPageProvider;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.document.DocumentHistoryPageProvider;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.provider.LatestCreatedUsersOrGroupsPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests the {@link AuditPageProvider}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
@Deploy("org.nuxeo.ecm.platform.audit:test-audit-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.audit:test-pageprovider-contrib.xml")
public class TestPageProvider {

    protected static final List<String> entriesIdx = Arrays.asList(new String[] { "3", "7", "7", "8", "1", "8", "7",
            "9" });

    protected static final Calendar testDate = Calendar.getInstance();

    @Inject
    AuditFeature audit;

    @Inject
    CoreSession session;

    @Inject
    UserManager userManager;

    @Inject
    TransactionalFeature txFeature;

    public void waitForAsyncCompletion() throws InterruptedException {
        txFeature.nextTransaction(Duration.ofSeconds(20));
    }

    @Before
    public void createTestEntries() {

        AuditReader reader = Framework.getService(AuditReader.class);
        assertNotNull(reader);

        audit.clear();

        AuditLogger logger = Framework.getService(AuditLogger.class);
        assertNotNull(logger);
        List<LogEntry> entries = new ArrayList<LogEntry>();

        for (String suffix : entriesIdx) {
            LogEntry entry = new LogEntryImpl();
            entry.setCategory("category" + suffix);
            entry.setEventId("event" + suffix);
            Calendar eventDate = (Calendar) testDate.clone();
            eventDate.add(Calendar.DAY_OF_YEAR, Integer.parseInt(suffix));
            entry.setEventDate(eventDate.getTime());
            entry.setDocType("docType" + suffix);
            entry.setDocUUID("uuid");

            entries.add(entry);
        }

        logger.addLogEntries(entries);

        List<?> res = reader.nativeQuery("select count(log.eventId) from LogEntry log", 1, 20);
        int count = ((Long) res.get(0)).intValue();
        assertEquals(entries.size(), count);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleProvider() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("GetAllEntries");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(AuditPageProvider.class.getSimpleName(), gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider("GetAllEntries", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>());

        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        assertNotNull(entries);
        assertEquals(5, entries.size());
        assertEquals("category" + entriesIdx.get(0), entries.get(0).getCategory());
        assertEquals("category" + entriesIdx.get(3), entries.get(3).getCategory());
        assertEquals("category" + entriesIdx.get(4), entries.get(4).getCategory());

        long nbPages = pp.getNumberOfPages();

        assertEquals(2, nbPages);

        pp.nextPage();
        entries = (List<LogEntry>) pp.getCurrentPage();

        assertEquals(3, entries.size());
        assertEquals("category" + entriesIdx.get(0 + 5), entries.get(0).getCategory());
        assertEquals("category" + entriesIdx.get(2 + 5), entries.get(2).getCategory());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProviderWithParams() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("GetAllEntriesInCategory");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(AuditPageProvider.class.getSimpleName(), gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider("GetAllEntriesInCategory", null, Long.valueOf(2), Long.valueOf(0),
                new HashMap<String, Serializable>(), "category7");

        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        assertNotNull(entries);
        assertEquals(2, entries.size());
        assertEquals("event" + entriesIdx.get(1), entries.get(0).getEventId());
        assertEquals("category7", entries.get(0).getCategory());
        assertEquals("event" + entriesIdx.get(2), entries.get(1).getEventId());
        assertEquals("category7", entries.get(1).getCategory());

        long nbPages = pp.getNumberOfPages();

        assertEquals(2, nbPages);

        pp.nextPage();
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(1, entries.size());
        assertEquals("event" + entriesIdx.get(6), entries.get(0).getEventId());
        assertEquals("category7", entries.get(0).getCategory());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProviderWithWhereClause() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("GetAllEntriesForDocumentInCategory");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(AuditPageProvider.class.getSimpleName(), gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider("GetAllEntriesForDocumentInCategory", (DocumentModel) null, null,
                Long.valueOf(2), Long.valueOf(0), new HashMap<String, Serializable>(), "uuid");

        DocumentModel searchDoc = session.createDocumentModel("File");
        searchDoc.setPathInfo("/", "dummy");
        searchDoc.setPropertyValue("dc:title", "category7");

        searchDoc = session.createDocument(searchDoc);
        pp.setSearchDocumentModel(searchDoc);

        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        assertNotNull(entries);
        assertEquals(2, entries.size());
        assertEquals("event" + entriesIdx.get(1), entries.get(0).getEventId());
        assertEquals("category7", entries.get(0).getCategory());
        assertEquals("event" + entriesIdx.get(2), entries.get(1).getEventId());
        assertEquals("category7", entries.get(1).getCategory());

        long nbPages = pp.getNumberOfPages();

        assertEquals(2, nbPages);

        pp.nextPage();
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(1, entries.size());
        assertEquals("event" + entriesIdx.get(6), entries.get(0).getEventId());
        assertEquals("category7", entries.get(0).getCategory());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProviderWithWhereClause2() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("GetAllEntriesForDocumentInCategories");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(AuditPageProvider.class.getSimpleName(), gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider("GetAllEntriesForDocumentInCategories", null, Long.valueOf(2),
                Long.valueOf(0), new HashMap<String, Serializable>(), "uuid");

        DocumentModel searchDoc = session.createDocumentModel("File");
        searchDoc.setPathInfo("/", "dummy");
        List<String> cats = new ArrayList<String>();
        cats.add("category7");
        cats.add("category3");
        searchDoc.setPropertyValue("dc:subjects", (Serializable) cats);

        searchDoc = session.createDocument(searchDoc);
        pp.setSearchDocumentModel(searchDoc);

        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        assertNotNull(entries);
        assertEquals(2, entries.size());

        long nbPages = pp.getNumberOfPages();

        assertEquals(2, nbPages);

        pp.nextPage();
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(2, entries.size());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProviderWithBetweenDates() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("GetAllEntriesBetween2Dates");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(AuditPageProvider.class.getSimpleName(), gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider("GetAllEntriesBetween2Dates", null, Long.valueOf(6), Long.valueOf(0),
                new HashMap<String, Serializable>(), "uuid");

        DocumentModel searchDoc = session.createDocumentModel("File");
        searchDoc.setPathInfo("/", "dummy");

        Calendar startDate = (Calendar) testDate.clone();
        startDate.add(Calendar.DAY_OF_YEAR, 7);
        Calendar endDate = (Calendar) testDate.clone();
        endDate.add(Calendar.DAY_OF_YEAR, 9);

        searchDoc.setPropertyValue("dc:issued", startDate);
        searchDoc.setPropertyValue("dc:valid", endDate);

        searchDoc = session.createDocument(searchDoc);
        pp.setSearchDocumentModel(searchDoc);

        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        assertNotNull(entries);
        assertEquals(6, entries.size());

        long nbPages = pp.getNumberOfPages();

        assertEquals(1, nbPages);

        // test with unset params
        searchDoc = session.createDocumentModel("File");
        searchDoc.setPathInfo("/", "dummy2");
        searchDoc = session.createDocument(searchDoc);
        pp.setSearchDocumentModel(searchDoc);

        entries = (List<LogEntry>) pp.getCurrentPage();

        assertNotNull(entries);
        assertEquals(6, entries.size());

        nbPages = pp.getNumberOfPages();

        assertEquals(2, nbPages);

        pp.nextPage();
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertNotNull(entries);
        assertEquals(2, entries.size());

        // test with unset startDate
        searchDoc = session.createDocumentModel("File");
        searchDoc.setPathInfo("/", "dummy3");
        endDate = (Calendar) testDate.clone();
        endDate.add(Calendar.DAY_OF_YEAR, 4);
        searchDoc.setPropertyValue("dc:valid", endDate);
        searchDoc = session.createDocument(searchDoc);
        pp.setSearchDocumentModel(searchDoc);

        entries = (List<LogEntry>) pp.getCurrentPage();

        assertNotNull(entries);
        assertEquals(2, entries.size());

        nbPages = pp.getNumberOfPages();
        assertEquals(1, nbPages);

        // test with unset endDate
        searchDoc = session.createDocumentModel("File");
        searchDoc.setPathInfo("/", "dummy3");
        startDate = (Calendar) testDate.clone();
        startDate.add(Calendar.DAY_OF_YEAR, 4);
        searchDoc.setPropertyValue("dc:issued", startDate);
        searchDoc = session.createDocument(searchDoc);
        pp.setSearchDocumentModel(searchDoc);

        entries = (List<LogEntry>) pp.getCurrentPage();

        assertNotNull(entries);
        assertEquals(6, entries.size());

        nbPages = pp.getNumberOfPages();
        assertEquals(1, nbPages);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDocumentHistoryPageProvider() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("DOCUMENT_HISTORY_PROVIDER");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(DocumentHistoryPageProvider.class.getSimpleName(), gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null, Long.valueOf(6), Long.valueOf(0),
                new HashMap<String, Serializable>(), "uuid");

        DocumentModel searchDoc = session.createDocumentModel("BasicAuditSearch");
        searchDoc.setPathInfo("/", "auditsearch");

        Calendar startDate = (Calendar) testDate.clone();
        startDate.add(Calendar.DAY_OF_YEAR, 7);
        Calendar endDate = (Calendar) testDate.clone();
        endDate.add(Calendar.DAY_OF_YEAR, 9);

        searchDoc.setPropertyValue("bas:startDate", startDate);
        searchDoc.setPropertyValue("bas:endDate", endDate);

        searchDoc = session.createDocument(searchDoc);
        pp.setSearchDocumentModel(searchDoc);

        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        assertNotNull(entries);
        assertEquals(6, entries.size());

        long nbPages = pp.getNumberOfPages();

        assertEquals(1, nbPages);
    }

    /**
     * @since 9.3
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testLatestUserGroupPageProvider() throws Exception {

        DocumentModel userModel = userManager.getBareUserModel();
        String schemaName = userManager.getUserSchemaName();
        userModel.setProperty(schemaName, "username", "Foo");
        userModel = userManager.createUser(userModel);

        DocumentModel userModel2 = userManager.getBareUserModel();
        schemaName = userManager.getUserSchemaName();
        userModel2.setProperty(schemaName, "username", "Bar");
        userModel2 = userManager.createUser(userModel2);

        waitForAsyncCompletion();

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition(
                LatestCreatedUsersOrGroupsPageProvider.LATEST_AUDITED_CREATED_USERS_OR_GROUPS_PROVIDER);
        assertNotNull(ppdef);

        PageProvider<?> pp = pps.getPageProvider(
                LatestCreatedUsersOrGroupsPageProvider.LATEST_CREATED_USERS_OR_GROUPS_PROVIDER, null, Long.valueOf(6),
                Long.valueOf(0), new HashMap<String, Serializable>());

        assertNotNull(pp);

        List<DocumentModel> entries = (List<DocumentModel>) pp.getCurrentPage();

        assertNotNull(entries);
        assertEquals(2, entries.size());
    }

}
