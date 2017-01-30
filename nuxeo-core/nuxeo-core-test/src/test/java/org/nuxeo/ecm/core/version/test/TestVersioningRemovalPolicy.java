package org.nuxeo.ecm.core.version.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.CoreService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.storage.sql.TXSQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestVersioningRemovalPolicy extends TXSQLRepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Override
    public void waitForAsyncCompletion() {
        nextTransaction();
        super.waitForAsyncCompletion();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected DocumentModelList getVersion() {
        return session.query("select * from Document where ecm:isCheckedInVersion=1");
    }

    @Test
    public void shouldRemoveOrphanVersionWhenLiveRemoved() throws Exception {

        DocumentModel doc = session.createDocumentModel("/", "testfile1",
                "File");
        doc = session.createDocument(doc);
        // create version
        doc.checkIn(VersioningOption.MINOR, null);
        // check out live doc
        doc.checkOut();

        DocumentModelList vs = getVersion();
        assertEquals(1, vs.size());

        // now remove the doc
        session.removeDocument(doc.getRef());
        session.save();
        waitForAsyncCompletion();

        // version should not be found
        vs = getVersion();
        assertEquals(0, vs.size());
    }

    @Test
    public void shouldRemoveOrphanVersionWhenLiveCheckedInRemoved() {
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        // create version, live doc is checked in
        session.checkIn(doc.getRef(), VersioningOption.MINOR, null);
        session.save();

        // version is found
        DocumentModelList vs = getVersion();
        assertEquals(1, vs.size());

        // now remove the doc
        session.removeDocument(doc.getRef());
        session.save();

        waitForAsyncCompletion();

        // version should not be found
        vs = getVersion();
        assertEquals(0, vs.size());
    }

    @Test
    public void shouldRemoveOrphanVersionsWhenProxyRemovedLast()
            throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "testfile1",
                "File");
        doc = session.createDocument(doc);
        DocumentRef ver = doc.checkIn(VersioningOption.MINOR, "");
        DocumentModel proxy = session.createProxy(ver, session.getRootDocument().getRef());

        // remove the doc first
        session.removeDocument(doc.getRef());
        session.save();
        waitForAsyncCompletion();

        DocumentModelList vs = getVersion();
        assertEquals(1, vs.size()); // 1 version remains due to proxu

        // remove proxy second
        session.removeDocument(proxy.getRef());
        session.save();
        waitForAsyncCompletion();

        vs = getVersion();
        assertEquals(0, vs.size()); // version deleted through last proxy
    }

    // NXP-14187
    @Test
    public void shouldRemoveOrphanVersionWhenContainingFolderRemoved() {
        DocumentModel ws = session.createDocumentModel("/", "ws", "Workspace");
        ws = session.createDocument(ws);
        DocumentModel folder = session.createDocumentModel("/ws", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/ws/folder", "file", "File");
        doc = session.createDocument(doc);
        // create first version
        session.checkIn(doc.getRef(), VersioningOption.MINOR, null);
        session.save();
        waitForAsyncCompletion();

        long n = cleanupOrphanVersions();
        assertEquals(0, n);

        // versions found
        DocumentModelList vs = getVersion();
        assertEquals(1, vs.size());

        // delete folder containing the doc
        session.removeDocument(folder.getRef());
        session.save();
        waitForAsyncCompletion();

        n = cleanupOrphanVersions();
        assertEquals(1, n);

        // versions should not be found
        vs = getVersion();
        assertEquals(0, vs.size());
    }

    // same test with more versions that we keep and more that we don't
    // NXP-14187
    @Test
    public void shouldRemoveOrphanVersionWhenContainingFolderRemoved2() {
        DocumentModel ws = session.createDocumentModel("/", "ws", "Workspace");
        ws = session.createDocument(ws);
        DocumentModel folder = session.createDocumentModel("/ws", "folder", "Folder");
        folder = session.createDocument(folder);

        DocumentModel doc = session.createDocumentModel("/ws/folder", "file", "File");
        doc = session.createDocument(doc);
        // create first version
        session.checkIn(doc.getRef(), VersioningOption.MINOR, null);
        // create more versions
        int N = 10;
        for (int i = 0; i < N; i++) {
            session.checkOut(doc.getRef());
            session.checkIn(doc.getRef(), VersioningOption.MINOR, null);
        }
        // also create another live doc and two versions that should not be removed
        DocumentModel doc2 = session.createDocumentModel("/ws", "doc2", "File");
        doc2 = session.createDocument(doc2);
        session.checkIn(doc2.getRef(), VersioningOption.MINOR, null);
        session.checkOut(doc2.getRef());
        session.checkIn(doc2.getRef(), VersioningOption.MINOR, null);

        // also create a proxy to a version and another version in the same history, not removed either
        DocumentModel doc3 = session.createDocumentModel("/ws", "doc3", "File");
        doc3 = session.createDocument(doc3);
        session.checkIn(doc3.getRef(), VersioningOption.MINOR, null);
        session.checkOut(doc3.getRef());
        DocumentRef doc3ver2 = session.checkIn(doc3.getRef(), VersioningOption.MINOR, null);
        session.createProxy(doc3ver2, new PathRef("/"));
        session.removeDocument(doc3.getRef()); // remove live doc, keep only proxy

        session.save();
        waitForAsyncCompletion();

        long n = cleanupOrphanVersions();
        assertEquals(0, n);

        // all versions found
        DocumentModelList vs = getVersion();
        assertEquals((N + 1) + 2 + 2, vs.size());

        // delete folder containing the doc
        session.removeDocument(folder.getRef());
        session.save();
        waitForAsyncCompletion();

        n = cleanupOrphanVersions();
        assertEquals(N + 1, n);

        // some versions (N+1) have been cleaned up
        vs = getVersion();
        assertEquals(2 + 2, vs.size());
    }

    protected long cleanupOrphanVersions() {
        CoreService coreService = Framework.getService(CoreService.class);
        return coreService.cleanupOrphanVersions(5);
    }

    @Test
    public void shouldNotRemoveOrphanVersionsWhenProxyRemovedButLiveRemains()
            throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "testfile1",
                "File");
        doc = session.createDocument(doc);
        DocumentRef ver = doc.checkIn(VersioningOption.MINOR, "");
        DocumentModel proxy = session.createProxy(ver, session.getRootDocument().getRef());

        // remove last proxy, but live doc still remains
        session.removeDocument(proxy.getRef());
        session.save();
        waitForAsyncCompletion();

        DocumentModelList vs = getVersion();
        assertEquals(1, vs.size()); // version not deleted
    }

    @Test
    public void shouldNotRemoveOrphanVersions() throws Exception {

        deployContrib("org.nuxeo.ecm.core.test.tests",
                "test-versioning-removal-nullcontrib.xml");

        DocumentModel doc = session.createDocumentModel("/", "testfile1",
                "File");
        doc = session.createDocument(doc);
        VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);
        assertNotNull(vdoc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", vdoc.getVersionLabel());

        doc.checkIn(VersioningOption.MINOR, "");
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", vdoc.getVersionLabel());

        doc.checkOut();
        assertTrue(doc.isCheckedOut());
        assertEquals("0.1+", vdoc.getVersionLabel());

        List<DocumentModel> versions = session.getVersions(doc.getRef());
        assertEquals(1, versions.size());

        DocumentModelList vs = getVersion();
        assertEquals(1, vs.size());

        // now remove the doc
        session.removeDocument(doc.getRef());
        session.save();

        waitForAsyncCompletion();

        vs = getVersion();
        assertEquals(1, vs.size());
    }

    @Test
    public void shouldRemoveOrphanFileVersionsOnly() throws Exception {

        deployContrib("org.nuxeo.ecm.core.test.tests",
                "test-versioning-removal-filtercontrib.xml");

        DocumentModel doc = session.createDocumentModel("/", "testfile1",
                "File");
        doc = session.createDocument(doc);
        VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);
        assertNotNull(vdoc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", vdoc.getVersionLabel());

        doc.checkIn(VersioningOption.MINOR, "");
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", vdoc.getVersionLabel());

        doc.checkOut();
        assertTrue(doc.isCheckedOut());
        assertEquals("0.1+", vdoc.getVersionLabel());

        DocumentModel note = session.createDocumentModel("/", "testnote1",
                "Note");
        note = session.createDocument(note);
        VersioningDocument vnote = note.getAdapter(VersioningDocument.class);
        assertNotNull(vnote);
        assertTrue(note.isCheckedOut());
        assertEquals("0.0", vnote.getVersionLabel());

        note.checkIn(VersioningOption.MINOR, "");
        assertFalse(note.isCheckedOut());
        assertEquals("0.1", vnote.getVersionLabel());

        note.checkOut();
        assertTrue(note.isCheckedOut());
        assertEquals("0.1+", vnote.getVersionLabel());

        List<DocumentModel> versions = session.getVersions(doc.getRef());
        assertEquals(1, versions.size());

        DocumentModelList vs = getVersion();
        assertEquals(2, vs.size());

        // now remove the doc
        session.removeDocument(doc.getRef());
        session.removeDocument(note.getRef());
        session.save();

        waitForAsyncCompletion();

        vs = getVersion();
        assertEquals(1, vs.size());
    }
}
