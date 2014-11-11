package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.audit.TestDocumentAuditPageProvider.Pfouh;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.DocumentHistoryReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.document.DocumentHistoryPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test the {@link DocumentHistoryPageProvider}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
@LocalDeploy("org.nuxeo.ecm.platform.audit.tests:test-pageprovider-contrib.xml")
@RepositoryConfig(init=Pfouh.class)
public class TestDocumentAuditPageProvider  {

    protected static final Calendar testDate = Calendar.getInstance();

    @Inject AuditReader reader;

    @Inject CoreSession session;

    protected static Pfouh pfouh;

    public static class Pfouh implements RepositoryInit {

        {
            pfouh = this;
        }

        protected boolean verbose = false;

        DocumentModel doc;

        DocumentModel proxy;

        List<DocumentModel> versions;

        @Override
        public void populate(CoreSession session) throws ClientException {

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
            }

            // create a version
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.MINOR);
            doc = session.saveDocument(doc);
            session.save();

            // do some more updates
            for (int i = 5; i < 10; i++) {
                doc.setPropertyValue("dc:description", "Update " + i);
                doc.getContextData().put("comment", "Update " + i);
                doc = session.saveDocument(doc);
                session.save();
            }


            proxy = session.publishDocument(doc, section);
            session.save();


            // do some more updates
            for (int i = 10; i < 15; i++) {
                doc.setPropertyValue("dc:description", "Update " + i);
                doc.getContextData().put("comment", "Update " + i);
                doc = session.saveDocument(doc);
                session.save();
            }

            versions = session.getVersions(doc.getRef());
            assertEquals(2, versions.size());

        }


    }


    @Test
    public void testDocumentHistoryPageProvider() throws Exception {


        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("DOCUMENT_HISTORY_PROVIDER");
        assertNotNull(ppdef);

        PageProvider<?> pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER",
                null, Long.valueOf(20), Long.valueOf(0),
                new HashMap<String, Serializable>(), pfouh.doc);

        DocumentModel searchDoc = session.createDocumentModel("BasicAuditSearch");
        searchDoc.setPathInfo("/", "auditsearch");

        searchDoc = session.createDocument(searchDoc);
        pp.setSearchDocumentModel(searchDoc);

        assertNotNull(pp);

        // Get Live doc history
        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        // create, 15+1 update , 2 checkin
        assertEquals(19, entries.size());
        long startId = entries.get(entries.size() - 1).getId();

        // Get Proxy history
        pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null,
                Long.valueOf(20), Long.valueOf(0),
                new HashMap<String, Serializable>(), pfouh.proxy);
        pp.setSearchDocumentModel(searchDoc);

        entries = (List<LogEntry>) pp.getCurrentPage();


        // 19 - 5 updates + create + proxyPublished
        int proxyEntriesCount = 19 - 5 + 1 + 1;
        assertEquals(proxyEntriesCount, entries.size());

        assertEquals(Long.valueOf(startId).longValue(), entries.get(proxyEntriesCount - 1).getId());
        assertEquals(
                Long.valueOf(startId + proxyEntriesCount + 1).longValue(),
                entries.get(0).getId());

        // Get version 1 history
        pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null,
                Long.valueOf(20), Long.valueOf(0),
                new HashMap<String, Serializable>(), pfouh.versions.get(0));
        pp.setSearchDocumentModel(searchDoc);
        entries = (List<LogEntry>) pp.getCurrentPage();

        // creation + 5 updates + update + checkin + created
        int version1EntriesCount = 1 + 5 + 1 + 1 + 1;
        if (version1EntriesCount == entries.size()) {
            assertEquals(Long.valueOf(startId).longValue(),
                    entries.get(version1EntriesCount - 1).getId());
            assertEquals(
                    Long.valueOf(startId + version1EntriesCount - 1).longValue(),
                    entries.get(0).getId());
        } else {
            // because update even may be 1ms behind checkin/created !
            assertEquals(version1EntriesCount - 1, entries.size());
        }

        // get version 2 history
        pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null,
                Long.valueOf(20), Long.valueOf(0),
                new HashMap<String, Serializable>(), pfouh.versions.get(1));
        pp.setSearchDocumentModel(searchDoc);

        entries = (List<LogEntry>) pp.getCurrentPage();


        // creation + 5x2 updates + checkin/update + checkin + created
        int versin2EntriesCount = 1 + 5 * 2 + 1 + 1 + 1 + 1;
        assertEquals(versin2EntriesCount, entries.size());
        assertEquals(Long.valueOf(startId).longValue(), entries.get(versin2EntriesCount - 1).getId());
        assertEquals(Long.valueOf(startId + versin2EntriesCount).longValue(),
                entries.get(0).getId());

    }

    @Inject DocumentHistoryReader history;

    @Test
    public void testDocumentHistoryReader() throws Exception {

        List<LogEntry> entries = history.getDocumentHistory(pfouh.versions.get(1), 0,
                20);
        assertNotNull(entries);
        // creation + 5x2 updates + checkin/update + checkin + created
        int versin2EntriesCount = 1 + 5 * 2 + 1 + 1 + 1 + 1;
        assertEquals(versin2EntriesCount, entries.size());

    }

}
