package org.nuxeo.ecm.core.version.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestVersioningRemovalPolicy {

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreSession session;

    protected void waitForAsyncCompletion() {
        nextTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected DocumentModelList getVersion() throws Exception {
        return session.query("select * from Document where ecm:isCheckedInVersion=1");
    }

    @Test
    public void shouldRemoveOrphanVersions() throws Exception {

        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
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
        assertEquals(0, vs.size());
    }

    @Test
    public void shouldRemoveOrphanVersionsWhenProxyRemovedLast() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
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

    @Test
    public void shouldNotRemoveOrphanVersionsWhenProxyRemovedButLiveRemains() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
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
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-versioning-removal-nullcontrib.xml")
    public void shouldNotRemoveOrphanVersions() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
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
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-versioning-removal-filtercontrib.xml")
    public void shouldRemoveOrphanFileVersionsOnly() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
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

        DocumentModel note = session.createDocumentModel("/", "testnote1", "Note");
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
