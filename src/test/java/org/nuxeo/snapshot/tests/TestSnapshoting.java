package org.nuxeo.snapshot.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.snapshot.Snapshot;
import org.nuxeo.snapshot.Snapshotable;
import org.nuxeo.snapshot.SnapshotableAdapter;

public class TestSnapshoting extends SQLRepositoryTestCase {

    boolean verbose = false;

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
        folder1.setPropertyValue("dc:title", "Folder 1");
        folder1 = session.createDocument(folder1);

        folder2 = session.createDocumentModel(root.getPathAsString(),
                "folder2", "Folder");
        folder2.setPropertyValue("dc:title", "Folder 2");
        folder2 = session.createDocument(folder2);

        folder11 = session.createDocumentModel(folder1.getPathAsString(),
                "folder11", "Folder");
        folder11.setPropertyValue("dc:title", "Folder 11");
        folder11 = session.createDocument(folder11);

        doc12 = session.createDocumentModel(folder1.getPathAsString(), "doc12",
                "File");
        doc12.setPropertyValue("dc:title", "Doc 12");
        doc12 = session.createDocument(doc12);

        folder13 = session.createDocumentModel(folder1.getPathAsString(),
                "folder13", "Folder");
        folder13.setPropertyValue("dc:title", "Folder 13");
        folder13 = session.createDocument(folder13);

        folder131 = session.createDocumentModel(folder13.getPathAsString(),
                "folder131", "Folder");
        folder131.setPropertyValue("dc:title", "Folder 131");
        folder131 = session.createDocument(folder131);

        doc1311 = session.createDocumentModel(folder131.getPathAsString(),
                "doc1311", "File");
        doc1311.setPropertyValue("dc:title", "Doc 1311");
        doc1311 = session.createDocument(doc1311);

        session.checkIn(doc1311.getRef(), VersioningOption.MINOR, null);

        doc1312 = session.createDocumentModel(folder131.getPathAsString(),
                "doc1312", "File");
        doc1312.setPropertyValue("dc:title", "Doc 1312");
        doc1312 = session.createDocument(doc1312);

        session.checkIn(doc1312.getRef(), VersioningOption.MINOR, null);

        doc1312.setPropertyValue("dc:description", "forced checkout");
        doc1312 = session.saveDocument(doc1312);

    }

    protected void dumpDBContent() throws Exception {
        System.out.println("\nDumping Live docs in repository");
        DocumentModelList docs = session.query("select * from Document where ecm:isCheckedInVersion=0 order by ecm:path");
        for (DocumentModel doc : docs) {
            StringBuffer sb = new StringBuffer();
            sb.append(doc.getPathAsString());
            sb.append(" - ");
            sb.append(doc.getVersionLabel());
            sb.append(" -- ");
            sb.append(doc.getTitle());
            System.out.println(sb.toString());
        }
        System.out.println("\n");
    }

    protected void dumpVersionsContent() throws Exception {
        System.out.println("\nDumping versions in repository");
        DocumentModelList docs = session.query("select * from Document where ecm:isCheckedInVersion=1");
        for (DocumentModel doc : docs) {
            StringBuffer sb = new StringBuffer();
            sb.append(doc.getPathAsString());
            sb.append(" - ");
            sb.append(doc.getVersionLabel());
            sb.append(" -- ");
            sb.append(doc.getTitle());
            if (doc.hasSchema(SnapshotableAdapter.SCHEMA)) {
                sb.append(" [ ");
                String[] uuids = (String[]) doc.getPropertyValue(SnapshotableAdapter.CHILDREN_PROP);
                for (String uuid : uuids) {
                    sb.append(uuid);
                    sb.append(",");
                }
                sb.append(" } ");
            }
            System.out.println(sb.toString());
        }
        System.out.println("\n");
    }

    protected String getContentHash() throws Exception {
        DocumentModelList alldocs = session.query("select * from Document where ecm:isCheckedInVersion=0 order by ecm:path");
        StringBuffer sb = new StringBuffer();
        for (DocumentModel doc : alldocs) {
            sb.append(doc.getId());
            sb.append(" -- ");
            sb.append(doc.getTitle());
            sb.append(" -- ");
            sb.append(doc.getVersionLabel());
            if (doc.isFolder()) {
                sb.append(" -- ");
                sb.append(doc.getName()); // not folderish items name is lost
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Test
    public void testSnapShotTreeAndRestore() throws Exception {

        buildTree();
        if (verbose) {
            System.out.println("## Initial Tree");
            dumpDBContent();
        }

        Snapshotable snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);

        Snapshot snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);

        assertNotNull(snapshot);

        if (verbose) {
            System.out.println("## Initial Tree Snapshot");
            System.out.println(snapshot.toString());
        }

        for (Snapshot snap : snapshot.getFlatTree()) {
            DocumentModel doc = snap.getDocument();
            if (doc.getName().equals("doc1312")) {
                assertEquals("0.2", doc.getVersionLabel());
            } else {
                assertEquals("0.1", doc.getVersionLabel());
            }
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

        // dumpVersionsContent();

        // redo a check in : should be identical
        snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);

        snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);
        // System.out.println(snapshot.toString());

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

        session.save();
        if (verbose) {
            System.out.println("## new Tree after updating a doc");
            dumpDBContent();
        }

        // redo a check in : should change versioning of a branch
        snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);

        snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);
        if (verbose) {
            System.out.println("## new Snapshot of the tree");
            System.out.println(snapshot.toString());
        }

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

        // save the content for later test
        String hash02 = getContentHash();

        // dumpVersionsContent();

        session.save();
        // now delete a folder
        session.removeDocument(folder13.getRef());

        if (verbose) {
            System.out.println("## new Tree after cutting a branch");
            dumpDBContent();
        }

        // redo a check in : should change versioning of head
        snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);

        snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);
        String snapshot03 = snapshot.toString();

        if (verbose) {
            System.out.println("## new Snapshot of the tree");
            System.out.println(snapshot03);
        }

        for (Snapshot snap : snapshot.getFlatTree()) {
            DocumentModel doc = snap.getDocument();
            if (doc.getName().equals("folder1")) {
                assertEquals("0.3", doc.getVersionLabel());
            } else {
                assertEquals("0.1", doc.getVersionLabel());
            }
        }
        assertEquals("0.3", snapshot.getDocument().getVersionLabel());

        String hash03 = getContentHash();
        assertFalse(hash02.equals(hash03));

        // now restore
        DocumentModel restored = snapshot.restore("0.2");
        assertNotNull(restored);
        assertFalse(restored.isVersion());
        assertEquals("0.2", restored.getVersionLabel());

        // check new DB content
        String hash02bis = getContentHash();
        assertEquals(hash02, hash02bis);

        // snapshot object should not have changed
        assertEquals(snapshot03, snapshot.toString());

        if (verbose) {
            System.out.println("## new Tree after restore on version 0.2");
            dumpDBContent();
        }

    }

    @Test
    public void testSnapshotableListener() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc1", "SnapshotableFolder");
        doc = session.createDocument(doc);
        session.save();
        assertEquals("", doc.getVersionLabel());
        assertEquals(null, doc.getPropertyValue("dc:title"));

        doc.setPropertyValue("dc:title", "without versioning");
        doc = session.saveDocument(doc);
        session.save();
        assertEquals("", doc.getVersionLabel());
        assertEquals("without versioning", doc.getPropertyValue("dc:title"));

        doc.setPropertyValue("dc:title", "with minor versioning");
        doc.putContextData(ScopeType.REQUEST, "snapshotVersioningOption",
                        "MINOR");
        doc = session.saveDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertEquals("0.1", doc.getVersionLabel());
        assertEquals("with minor versioning", doc.getPropertyValue("dc:title"));

        doc.setPropertyValue("dc:title", "with major versioning");
        doc.putContextData(ScopeType.REQUEST, "snapshotVersioningOption",
                        "MAJOR");
        doc = session.saveDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertEquals("1.0", doc.getVersionLabel());
        assertEquals("with major versioning", doc.getPropertyValue("dc:title"));

        doc.setPropertyValue("dc:title", "with none versioning");
        doc.putContextData(ScopeType.REQUEST, "snapshotVersioningOption",
                        "NONE");
        doc = session.saveDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertEquals("1.0+", doc.getVersionLabel());
        assertEquals("with none versioning", doc.getPropertyValue("dc:title"));

        doc.setPropertyValue("dc:title", "with non existing versioning value");
        doc.putContextData(ScopeType.REQUEST, "snapshotVersioningOption",
                        "NON_EXISTING VALUE");
        doc = session.saveDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertEquals("1.0+", doc.getVersionLabel());
        assertEquals("with non existing versioning value", doc.getPropertyValue("dc:title"));
    }
}
