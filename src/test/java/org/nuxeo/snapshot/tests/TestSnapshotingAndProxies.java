package org.nuxeo.snapshot.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.snapshot.Snapshot;
import org.nuxeo.snapshot.Snapshotable;
import org.nuxeo.snapshot.SnapshotableAdapter;

public class TestSnapshotingAndProxies extends SQLRepositoryTestCase {

    boolean verbose = true;

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

    protected DocumentModel rootB;

    protected DocumentModel folderB1;

    protected DocumentModel folderB2;

    protected DocumentModel folderB11;

    protected DocumentModel docB12;

    protected DocumentModel folderB13;

    protected DocumentModel docB131;

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

        // create second tree

        rootB = session.createDocumentModel("/", "rootB", "SnapshotableFolder");
        rootB = session.createDocument(rootB);

        folderB1 = session.createDocumentModel(rootB.getPathAsString(),
                "folderB1", "Folder");
        folderB1.setPropertyValue("dc:title", "Folder B1");
        folderB1 = session.createDocument(folderB1);

        folderB2 = session.createDocumentModel(rootB.getPathAsString(),
                "folderB2", "Folder");
        folderB2.setPropertyValue("dc:title", "Folder B2");
        folderB2 = session.createDocument(folderB2);

        folderB11 = session.createDocumentModel(folderB1.getPathAsString(),
                "folderB11", "Folder");
        folderB11.setPropertyValue("dc:title", "Folder B11");
        folderB11 = session.createDocument(folderB11);

        docB12 = session.createDocumentModel(folderB1.getPathAsString(),
                "docB12", "File");
        docB12.setPropertyValue("dc:title", "Doc B12");
        docB12 = session.createDocument(docB12);

        folderB13 = session.createDocumentModel(folderB1.getPathAsString(),
                "folderB13", "Folder");
        folderB13.setPropertyValue("dc:title", "Folder B13");
        folderB13 = session.createDocument(folderB13);

        docB131 = session.createDocumentModel(folderB13.getPathAsString(),
                "docB13", "File");
        docB131.setPropertyValue("dc:title", "Doc B131");
        docB131 = session.createDocument(docB131);

        session.save();

    }

    // helper method for creating a standard proxy
    protected DocumentModel createStandardProxy(DocumentModel targetDocument,
            DocumentModel parentForProxy) throws Exception {
        DocumentModel proxy = session.publishDocument(targetDocument,
                parentForProxy);
        session.save();
        return proxy;
    }

    // helper method for creating a live proxy
    protected DocumentModel createLiveProxy(DocumentModel targetDocument,
            DocumentModel parentForProxy) throws Exception {
        DocumentModel proxy = session.createProxy(targetDocument.getRef(),
                parentForProxy.getRef());
        session.save();
        return proxy;
    }

    // helper method that provides children listing using different technics
    // in the target solution this should probably be encapsulated in a
    // PageProvider ...
    protected List<DocumentModel> getChildren(DocumentModel parent)
            throws ClientException {

        String query = "select * from Document where ecm:parentId = ";

        if (parent.isProxy()) {
            DocumentModel target = session.getDocument(new IdRef(
                    parent.getSourceId()));
            if (target.isVersion()) {
                // need to use tree versioning
                Snapshot snapshot = target.getAdapter(Snapshot.class);
                if (snapshot != null) {
                    return snapshot.getChildren();
                } else {
                    return Collections.emptyList();
                }
            } else {
                // list children in target Folder
                query = query + "'" + target.getId() + "'";
            }
        } else {
            // normal listing
            query = query + "'" + parent.getId() + "'";
        }

        return session.query(query);

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

            if (doc.isProxy()) {
                sb.append(" [ proxy ");
                IdRef target = new IdRef(doc.getSourceId());
                DocumentModel targetDoc = session.getDocument(target);
                if (targetDoc.isVersion()) {
                    sb.append("version " + targetDoc.getPathAsString());
                } else {
                    sb.append("live " + targetDoc.getPathAsString());
                }
                sb.append(" ]");
            }

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

        createStandardProxy(docB131, folder11);
        createLiveProxy(docB12, folder11);
        if (verbose) {
            System.out.println("## Tree after publish");
            dumpDBContent();
        }
        Snapshotable snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);

        if (true) {
            return; // no need to go further for now
        }

        Snapshot snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);

        assertNotNull(snapshot);

        if (verbose) {
            System.out.println("## Initial Tree Snapshot");
            System.out.println(snapshot.toString());
        }

        // ...
    }
}
