package org.nuxeo.ecm.platform.audit;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.DocumentHistoryReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.document.DocumentHistoryPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;

/**
 * Test the {@link DocumentHistoryPageProvider}
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class TestDocumentAuditPageProvider extends RepositoryOSGITestCase {

    protected static final Calendar testDate = Calendar.getInstance();

    protected DocumentModel doc;

    protected DocumentModel proxy;

    protected List<DocumentModel> versions;

    protected boolean verbose = false;

    protected void dump(Object ob) {
        System.out.println(ob.toString());
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
        deployBundle("org.nuxeo.ecm.platform.dublincore");
        deployBundle("org.nuxeo.ecm.platform.audit");
        deployBundle("org.nuxeo.ecm.platform.audit.tests");
        deployBundle("org.nuxeo.ecm.platform.query.api");

        deployTestContrib("org.nuxeo.ecm.platform.audit.tests",
                "nxaudit-tests.xml");
        deployTestContrib("org.nuxeo.ecm.platform.audit.tests",
                "test-audit-contrib.xml");

        deployTestContrib("org.nuxeo.ecm.platform.audit.tests",
                "test-pageprovider-contrib.xml");

    }

    @Override
    public void tearDown() throws Exception {
        if (session != null) {
            closeSession();
        }
        super.tearDown();
    }

    protected void createTestEntries() throws Exception {

        AuditReader reader = Framework.getLocalService(AuditReader.class);        
        
        DocumentModel section = session.createDocumentModel("/", "section",
                "Folder");
        section = session.createDocument(section);

        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "TestDoc");

        // create the doc
        doc = session.createDocument(doc);

        // do some updates
        for (int i = 0; i < 5; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.getContextData().put("comment", "Update " + i);
            doc = session.saveDocument(doc);
            session.save();
            waitForEventsDispatched();
        }

        // wait at least 1s to be sure we have a precise timestamp in all DB backend
        Thread.sleep(1100); 
        
        // create a version
        doc.putContextData(VersioningService.VERSIONING_OPTION,
                VersioningOption.MINOR);
        doc = session.saveDocument(doc);
        session.save();
        waitForEventsDispatched();

        // wait at least 1s to be sure we have a precise timestamp in all DB backend
        Thread.sleep(1100); 
        
        // do some more updates
        for (int i = 5; i < 10; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.getContextData().put("comment", "Update " + i);
            doc = session.saveDocument(doc);
            session.save();
            waitForEventsDispatched();
        }

        // wait at least 1s to be sure we have a precise timestamp in all DB backend
        Thread.sleep(1100); 
        
        proxy = session.publishDocument(doc, section);
        session.save();
        waitForEventsDispatched();

        Thread.sleep(1100); // wait at least 1s to be sure we have a precise timestamp in all DB backend
        
        // do some more updates
        for (int i = 10; i < 15; i++) {
            doc.setPropertyValue("dc:description", "Update " + i);
            doc.getContextData().put("comment", "Update " + i);
            doc = session.saveDocument(doc);
            session.save();
            waitForEventsDispatched();
        }

        versions = session.getVersions(doc.getRef());
        assertEquals(2, versions.size());
        if (verbose) {
            for (DocumentModel version : versions) {
                System.out.println("version: " + version.getId());
                System.out.println("version series: "
                        + version.getVersionSeriesId());
                System.out.println("version label: " + version.getVersionLabel());         
            }
        }

        List<LogEntry> entries = (List<LogEntry>) reader.nativeQuery("from LogEntry", 0, 100);
        if (!entries.get(0).getDocUUID().equals(section.getId())) {
            entries.remove(0);
        }
        if (verbose) {
            dump(entries);
        }

    }

    public void testDocumentHistoryPageProvider() throws Exception {

        openRepository();
        createTestEntries();

        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("DOCUMENT_HISTORY_PROVIDER");
        assertNotNull(ppdef);

        PageProvider<?> pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER",
                null, Long.valueOf(20), Long.valueOf(0),
                new HashMap<String, Serializable>(), doc);

        DocumentModel searchDoc = session.createDocumentModel("BasicAuditSearch");
        searchDoc.setPathInfo("/", "auditsearch");

        searchDoc = session.createDocument(searchDoc);
        pp.setSearchDocumentModel(searchDoc);

        assertNotNull(pp);

        // Get Live doc history
        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        if (verbose) {
            System.out.println("Live doc history");
            dump(entries);
        }

        assertEquals(18, entries.size()); // remove section + proxy
        long startIdx = entries.get(0).getId();
        long endIdx = entries.get(17).getId();
        

        // Get Proxy history
        pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null,
                Long.valueOf(20), Long.valueOf(0),
                new HashMap<String, Serializable>(), proxy);
        pp.setSearchDocumentModel(searchDoc);

        entries = (List<LogEntry>) pp.getCurrentPage();
        if (verbose) {
            System.out.println("Proxy doc history");
            dump(entries);
        }

        // - 5 updates + create
        assertEquals(18 - 5 +1 , entries.size()); 

        assertEquals(Long.valueOf(startIdx).longValue(), entries.get(0).getId());
        assertEquals(Long.valueOf(startIdx+13).longValue(),
                entries.get(18 - 5).getId());

        // Get version 1 history
        pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null,
                Long.valueOf(20), Long.valueOf(0),
                new HashMap<String, Serializable>(), versions.get(0));
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();

        if (verbose) {
            System.out.println("Version " + versions.get(0).getVersionLabel()
                    + " doc history");
            dump(entries);
        }

        assertEquals(1 + 5, entries.size()); // creation + 5 updates
        assertEquals(Long.valueOf(startIdx).longValue(), entries.get(0).getId());
        assertEquals(Long.valueOf(startIdx+5).longValue(), entries.get(5).getId());

        // get version 2 history
        pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null,
                Long.valueOf(20), Long.valueOf(0),
                new HashMap<String, Serializable>(), versions.get(1));
        pp.setSearchDocumentModel(searchDoc);
        
        entries = (List<LogEntry>) pp.getCurrentPage();
        
        if (verbose) {
            System.out.println("Version " + versions.get(1).getVersionLabel()
                    + " doc history");
            dump(entries);
        }

        // creation + 5x2 updates + checkin/update
        assertEquals(1 + 5 + 1 + 5 +1, entries.size()); 
        assertEquals(Long.valueOf(startIdx).longValue(), entries.get(0).getId());
        assertEquals(Long.valueOf(startIdx+11).longValue(), entries.get(11).getId());

    }
    
    public void testDocumentHistoryReader() throws Exception {
        
        openRepository();
        createTestEntries();
        
        DocumentHistoryReader reader = Framework.getLocalService(DocumentHistoryReader.class);
        assertNotNull(reader);
        
        List<LogEntry> entries =  reader.getDocumentHistory(versions.get(1), 0, 20);
        assertNotNull(entries);
        // creation + 5x2 updates + checkin/update
        assertEquals(1 + 5 + 1 + 5 +1, entries.size()); 
        
    }

}
