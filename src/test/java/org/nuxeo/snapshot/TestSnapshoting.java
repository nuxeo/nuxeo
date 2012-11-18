package org.nuxeo.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.snapshot.Snapshot;
import org.nuxeo.snapshot.Snapshotable;
import org.nuxeo.snapshot.SnapshotableAdapter;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(type = BackendType.H2, init = PublishRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.snapshot" })
public class TestSnapshoting extends AbstractTestSnapshot {

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
        DocumentModel doc = session.createDocumentModel("/", "doc1",
                "SnapshotableFolder");
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
        assertEquals("with non existing versioning value",
                doc.getPropertyValue("dc:title"));
    }
}
