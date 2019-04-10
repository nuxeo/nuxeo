/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(init = PublishRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.snapshot")
public class TestSnapshoting extends AbstractTestSnapshot {

    @Inject
    protected EventService eventService;

    protected String getContentHash() throws Exception {
        DocumentModelList alldocs = session.query("select * from Document where ecm:isVersion = 0 order by ecm:path");
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
        DocumentModelList docs = session.query("select * from Document where ecm:isVersion = 1 order by ecm:path");
        for (DocumentModel doc : docs) {
            if (doc.isFolder() && !doc.hasFacet(Snapshot.FACET)) {
                System.out.println("ERR : doc " + doc.getPathAsString() + " has no shapshot schema");
                assertTrue(doc.hasFacet(Snapshot.FACET));
            }
            assertTrue(doc.hasFacet("Versionable"));
        }

        // dumpVersionsContent();

        // refetch the root document after snapshot
        session.save();
        root = session.getDocument(root.getRef());

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
            if (doc.getName().startsWith("doc131") || doc.getName().startsWith("folder13")
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
        doc.putContextData("snapshotVersioningOption", "MINOR");
        doc = session.saveDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertEquals("0.1", doc.getVersionLabel());
        assertEquals("with minor versioning", doc.getPropertyValue("dc:title"));

        doc.setPropertyValue("dc:title", "with major versioning");
        doc.putContextData("snapshotVersioningOption", "MAJOR");
        doc = session.saveDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertEquals("1.0", doc.getVersionLabel());
        assertEquals("with major versioning", doc.getPropertyValue("dc:title"));

        doc.setPropertyValue("dc:title", "with none versioning");
        doc.putContextData("snapshotVersioningOption", "NONE");
        doc = session.saveDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertEquals("1.0+", doc.getVersionLabel());
        assertEquals("with none versioning", doc.getPropertyValue("dc:title"));

        doc.setPropertyValue("dc:title", "with non existing versioning value");
        doc.putContextData("snapshotVersioningOption", "NON_EXISTING VALUE");
        doc = session.saveDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        assertEquals("1.0+", doc.getVersionLabel());
        assertEquals("with non existing versioning value", doc.getPropertyValue("dc:title"));
    }

    @Test
    public void testSnapshotableVersionRemovalPolicy() throws Exception {
        root = session.createDocumentModel("/", "root", "SnapshotableFolder");
        root = session.createDocument(root);

        folder1 = session.createDocumentModel(root.getPathAsString(), "folder1", "Folder");
        folder1.setPropertyValue("dc:title", "Folder 1");
        folder1 = session.createDocument(folder1);

        // 1 file created under /folder1 -> snapshot in 1.0
        Snapshotable snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);
        DocumentModel doc1 = session.createDocumentModel(folder1.getPathAsString(), "doc1", "File");
        doc1.setPropertyValue("dc:title", "Doc 1");
        session.createDocument(doc1);
        Snapshot snapshot = snapshotable.createSnapshot(VersioningOption.MAJOR);
        assertNotNull(snapshot);

        // 1 other file created under /folder1 -> snapshot in 2.0
        DocumentModel doc2 = session.createDocumentModel(folder1.getPathAsString(), "doc2", "File");
        doc2.setPropertyValue("dc:title", "Doc 2");
        session.createDocument(doc2);
        // folder1 children kept in memory for final check
        DocumentModelList children = session.getChildren(new IdRef(folder1.getId()));
        snapshot = snapshotable.createSnapshot(VersioningOption.MAJOR);
        assertNotNull(snapshot);

        // root restored in 1.0 -> 1 other file created -> snapshot in 3.0
        DocumentModel restored = snapshot.restore("1.0");
        assertNotNull(restored);
        snapshotable = restored.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);
        DocumentModel doc3 = session.createDocumentModel(folder1.getPathAsString(), "doc3", "File");
        doc3.setPropertyValue("dc:title", "Doc 3");
        session.createDocument(doc3);
        snapshot = snapshotable.createSnapshot(VersioningOption.MAJOR);
        assertNotNull(snapshot);

        // root restore in 2.0 to check if the version of 'file2' has been kept
        restored = snapshot.restore("2.0");
        assertNotNull(restored);
        // final check
        waitForAsyncCompletion();
        DocumentModelList sameChildren = session.getChildren(new IdRef(folder1.getId()));
        assertEquals(children.size(), sameChildren.size());
    }

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
}
