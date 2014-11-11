package org.nuxeo.ecm.core.version.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestVersioningRemovalPolicy extends SQLRepositoryTestCase {

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

    protected DocumentModelList getVersion() throws Exception {
        return session.query("select * from Document where ecm:isCheckedInVersion=1");
    }

    @Test
    public void shouldRemoveOrphanVersions() throws Exception {

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

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        session.save();

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
        session.save();
        DocumentModelList vs = getVersion();
        assertEquals(1, vs.size()); // 1 version remains due to proxu

        // remove proxy second
        session.removeDocument(proxy.getRef());
        session.save();
        waitForAsyncCompletion();
        session.save();
        vs = getVersion();
        assertEquals(0, vs.size()); // version deleted through last proxy
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
        session.save();
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

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        session.save();

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

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        session.save();

        vs = getVersion();
        assertEquals(1, vs.size());
    }
}
