package org.nuxeo.ecm.platform.audit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditPageProvider;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.document.DocumentHistoryPageProvider;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Tests the {@link AuditPageProvider}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class TestPageProvider extends RepositoryOSGITestCase {

    protected static final List<String> entriesIdx = Arrays.asList(new String[] {
            "3", "7", "7", "8", "1", "8", "7", "9" });

    protected static final Calendar testDate = Calendar.getInstance();

    protected void dump(Object ob) {
        // System.out.println(ob.toString());
    }

    protected void dump(List<?> obs) {
        for (Object ob : obs) {
            dump(ob);
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.audit");
        deployBundle("org.nuxeo.ecm.platform.audit.tests");
        deployBundle("org.nuxeo.ecm.platform.query.api");

        deployTestContrib("org.nuxeo.ecm.platform.audit.tests",
                "nxaudit-tests.xml");
        deployTestContrib("org.nuxeo.ecm.platform.audit.tests",
                "test-audit-contrib.xml");

        deployTestContrib("org.nuxeo.ecm.platform.audit.tests",
                "test-pageprovider-contrib.xml");

        createTestEntries();
    }

    @Override
    public void tearDown() throws Exception {
        if (session != null) {
            closeSession();
        }
        super.tearDown();
    }

    protected void createTestEntries() {

        AuditReader reader = Framework.getLocalService(AuditReader.class);
        assertNotNull(reader);

        String query = "select count(log.id) from LogEntry log ";
        List resCount = reader.nativeQuery(query, 1, 20);
        if (((Long) resCount.get(0)).longValue() > 0) {
            // reader.nativeQuery("DELETE FROM LogEntry log where log.docUUID='uuid'",
            // 1, 20);
            return;
        }

        AuditLogger logger = Framework.getLocalService(AuditLogger.class);
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

        List res = reader.nativeQuery(
                "select count(log.eventId) from LogEntry log", 1, 20);
        int count = ((Long) res.get(0)).intValue();
        dump("Audit initialized with " + count + " entries");
        assertEquals(entries.size(), count);

        entries = (List<LogEntry>) reader.nativeQuery("from LogEntry log", 0,
                10);
        dump(entries);

    }

    public void testSimpleProvider() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("GetAllEntries");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(AuditPageProvider.class.getSimpleName(),
                gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider("GetAllEntries", null,
                Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>());

        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        dump(pp);
        dump(entries);

        assertNotNull(entries);
        assertEquals(5, entries.size());
        assertEquals("category" + entriesIdx.get(0),
                entries.get(0).getCategory());
        assertEquals("category" + entriesIdx.get(3),
                entries.get(3).getCategory());
        assertEquals("category" + entriesIdx.get(4),
                entries.get(4).getCategory());

        long nbPages = pp.getNumberOfPages();

        assertEquals(2, nbPages);

        pp.nextPage();
        entries = (List<LogEntry>) pp.getCurrentPage();
        dump(entries);

        assertEquals(3, entries.size());
        assertEquals("category" + entriesIdx.get(0 + 5),
                entries.get(0).getCategory());
        assertEquals("category" + entriesIdx.get(2 + 5),
                entries.get(2).getCategory());

    }

    public void testProviderWithParams() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("GetAllEntriesInCategory");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(AuditPageProvider.class.getSimpleName(),
                gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider("GetAllEntriesInCategory",
                null, Long.valueOf(2), Long.valueOf(0),
                new HashMap<String, Serializable>(), "category7");

        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        dump(pp);
        dump(entries);

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
        dump(entries);
        assertEquals(1, entries.size());
        assertEquals("event" + entriesIdx.get(6), entries.get(0).getEventId());
        assertEquals("category7", entries.get(0).getCategory());

    }

    public void testProviderWithWhereClause() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("GetAllEntriesForDocumentInCategory");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(AuditPageProvider.class.getSimpleName(),
                gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider(
                "GetAllEntriesForDocumentInCategory", null, Long.valueOf(2),
                Long.valueOf(0), new HashMap<String, Serializable>(), "uuid");

        openSession();
        DocumentModel searchDoc = session.createDocumentModel("File");
        searchDoc.setPathInfo("/", "dummy");
        searchDoc.setPropertyValue("dc:title", "category7");

        searchDoc = session.createDocument(searchDoc);
        pp.setSearchDocumentModel(searchDoc);

        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        dump(pp);
        dump(entries);

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
        dump(entries);
        assertEquals(1, entries.size());
        assertEquals("event" + entriesIdx.get(6), entries.get(0).getEventId());
        assertEquals("category7", entries.get(0).getCategory());

    }

    public void testProviderWithWhereClause2() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("GetAllEntriesForDocumentInCategories");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(AuditPageProvider.class.getSimpleName(),
                gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider(
                "GetAllEntriesForDocumentInCategories", null, Long.valueOf(2),
                Long.valueOf(0), new HashMap<String, Serializable>(), "uuid");

        openSession();
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

        dump(pp);
        dump(entries);

        assertNotNull(entries);
        assertEquals(2, entries.size());

        long nbPages = pp.getNumberOfPages();

        assertEquals(2, nbPages);

        pp.nextPage();
        entries = (List<LogEntry>) pp.getCurrentPage();
        dump(entries);
        assertEquals(2, entries.size());

    }

    public void testProviderWithBetweenDates() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("GetAllEntriesBetween2Dates");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(AuditPageProvider.class.getSimpleName(),
                gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider("GetAllEntriesBetween2Dates",
                null, Long.valueOf(6), Long.valueOf(0),
                new HashMap<String, Serializable>(), "uuid");

        openSession();
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

        dump(pp);
        dump(entries);

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

        dump(pp);
        dump(entries);

        assertNotNull(entries);
        assertEquals(6, entries.size());

        nbPages = pp.getNumberOfPages();

        assertEquals(2, nbPages);

        pp.nextPage();
        entries = (List<LogEntry>) pp.getCurrentPage();
        dump(entries);
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

        dump(pp);
        dump(entries);

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

        dump(pp);
        dump(entries);

        assertNotNull(entries);
        assertEquals(6, entries.size());

        nbPages = pp.getNumberOfPages();
        assertEquals(1, nbPages);

    }

    public void testDocumentHistoryPageProvider() throws Exception {

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("DOCUMENT_HISTORY_PROVIDER");
        assertNotNull(ppdef);

        GenericPageProviderDescriptor gppdef = (GenericPageProviderDescriptor) ppdef;
        assertEquals(DocumentHistoryPageProvider.class.getSimpleName(),
                gppdef.getPageProviderClass().getSimpleName());

        PageProvider<?> pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER",
                null, Long.valueOf(6), Long.valueOf(0),
                new HashMap<String, Serializable>(), "uuid");

        openSession();
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

        dump(pp);
        dump(entries);

        assertNotNull(entries);
        assertEquals(6, entries.size());

        long nbPages = pp.getNumberOfPages();

        assertEquals(1, nbPages);
    }
}
