package org.nuxeo.snapshot.tests;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.snapshot.Snapshot;
import org.nuxeo.snapshot.Snapshotable;
import static org.junit.Assert.*;

public class TestSnapshoting extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.dublincore");
        deployBundle("org.nuxeo.snapshot");
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.waitForAsyncCompletion();
        closeSession();
        super.tearDown();
    }

    protected DocumentModel root;

    protected DocumentModel folder1;

    protected DocumentModel folder2;

    protected DocumentModel folder11;

    protected DocumentModel doc12;

    protected DocumentModel folder13;

    protected DocumentModel folder131;

    protected DocumentModel doc1311;

    protected DocumentModel doc1312;

    protected void buildTree() throws Exception {
        root = session.createDocumentModel("/", "root", "SnapshotableFolder");
        root = session.createDocument(root);

        folder1 = session.createDocumentModel(root.getPathAsString(),
                "folder1", "Folder");
        folder1 = session.createDocument(folder1);

        folder2 = session.createDocumentModel(root.getPathAsString(),
                "folder2", "Folder");
        folder2 = session.createDocument(folder2);

        folder11 = session.createDocumentModel(folder1.getPathAsString(),
                "folder11", "Folder");
        folder11 = session.createDocument(folder11);

        doc12 = session.createDocumentModel(folder1.getPathAsString(), "doc12",
                "File");
        doc12 = session.createDocument(doc12);

        folder13 = session.createDocumentModel(folder1.getPathAsString(),
                "folder13", "Folder");
        folder13 = session.createDocument(folder13);

        folder131 = session.createDocumentModel(folder13.getPathAsString(),
                "folder131", "Folder");
        folder131 = session.createDocument(folder131);

        doc1311 = session.createDocumentModel(folder131.getPathAsString(),
                "doc1311", "File");
        doc1311 = session.createDocument(doc1311);

        session.checkIn(doc1311.getRef(), VersioningOption.MINOR, null);

        doc1312 = session.createDocumentModel(folder131.getPathAsString(),
                "doc1312", "File");
        doc1312 = session.createDocument(doc1312);

        session.checkIn(doc1312.getRef(), VersioningOption.MINOR, null);

        doc1312.setPropertyValue("dc:description", "forced checkout");
        doc1312 = session.saveDocument(doc1312);

    }

    protected void dumpDBContent() throws Exception {
        DocumentModelList docs = session.query("select * from Document where ecm:isCheckedInVersion=0 order by ecm:path");
        for (DocumentModel doc : docs) {
            StringBuffer sb = new StringBuffer();
            sb.append(doc.getPathAsString());
            sb.append(" - ");
            sb.append(doc.getVersionLabel());
            sb.append(" -- ");
            for (String facet : doc.getFacets()) {
                sb.append(facet + ",");
            }
            System.out.println(sb.toString());
        }
    }

    @Test
    public void testSnapShot() throws Exception {

        buildTree();

        Snapshotable snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);

        Snapshot snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);

        assertNotNull(snapshot);

        System.out.println(snapshot.toString());

        for (Snapshot snap : snapshot.getFlatTree()) {
            DocumentModel doc = snap.getDocument();
            if (doc.getName().equals("doc1312")) {
                assertEquals("0.2", doc.getVersionLabel());
            } else {
                assertEquals("0.1", doc.getVersionLabel());
            }
            // System.out.println(doc.getName() + "-" + doc.getVersionLabel());
        }
        assertEquals("0.1", snapshot.getDocument().getVersionLabel());

        // check that the facet has been added for all child
        DocumentModelList docs = session.query("select * from Document where ecm:isCheckedInVersion=1 order by ecm:path");
        for (DocumentModel doc : docs) {
            if (doc.isFolder() && !doc.hasFacet(Snapshot.FACET)) {
                System.out.println("ERR : doc " + doc.getPathAsString()
                        + " has no shapshot schema");
                assertTrue(doc.hasFacet(Snapshot.FACET));
            }
            assertTrue(doc.hasFacet("Versionable"));
        }

        // redo a check in : should be identical
        snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);

        snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);
        System.out.println(snapshot.toString());

        for (Snapshot snap : snapshot.getFlatTree()) {
            DocumentModel doc = snap.getDocument();
            if (doc.getName().equals("doc1312")) {
                assertEquals("0.2", doc.getVersionLabel());
            } else {
                assertEquals("0.1", doc.getVersionLabel());
            }
        }
        assertEquals("0.1", snapshot.getDocument().getVersionLabel());

        // change a leaf
        doc1311.setPropertyValue("dc:description", "forced checkout");
        doc1311 = session.saveDocument(doc1311);

        // redo a check in : should change versioning of a branch
        snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);

        snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);
        System.out.println(snapshot.toString());

        for (Snapshot snap : snapshot.getFlatTree()) {
            DocumentModel doc = snap.getDocument();
            if (doc.getName().startsWith("doc131")
                    || doc.getName().startsWith("folder13")
                    || doc.getName().equals("folder1")) {
                assertEquals("0.2", doc.getVersionLabel());
            } else {
                assertEquals("0.1", doc.getVersionLabel());
            }
        }
        assertEquals("0.2", snapshot.getDocument().getVersionLabel());

        // now delete a folder
        session.removeDocument(folder13.getRef());

        // redo a check in : should change versioning of head
        snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);

        snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);
        System.out.println(snapshot.toString());
        for (Snapshot snap : snapshot.getFlatTree()) {
            DocumentModel doc = snap.getDocument();
            if (doc.getName().equals("folder1")) {
                assertEquals("0.3", doc.getVersionLabel());
            } else {
                assertEquals("0.1", doc.getVersionLabel());
            }
        }
        assertEquals("0.3", snapshot.getDocument().getVersionLabel());

        // now restore
        // snapshot.restore("0.2");
        // System.out.println(snapshot.toString());

        // dumpDBContent();

    }
}
