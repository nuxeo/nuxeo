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

import javax.inject.Inject;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.runtime.api.Framework;

public class AbstractTestSnapshot {
    @Inject
    protected CoreSession session;

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

    boolean verbose = false;

    protected void buildTree() throws Exception {
        root = session.createDocumentModel("/", "root", "SnapshotableFolder");
        root = session.createDocument(root);

        folder1 = session.createDocumentModel(root.getPathAsString(), "folder1", "Folder");
        folder1.setPropertyValue("dc:title", "Folder 1");
        folder1 = session.createDocument(folder1);

        folder2 = session.createDocumentModel(root.getPathAsString(), "folder2", "Folder");
        folder2.setPropertyValue("dc:title", "Folder 2");
        folder2 = session.createDocument(folder2);

        folder11 = session.createDocumentModel(folder1.getPathAsString(), "folder11", "Folder");
        folder11.setPropertyValue("dc:title", "Folder 11");
        folder11 = session.createDocument(folder11);

        doc12 = session.createDocumentModel(folder1.getPathAsString(), "doc12", "File");
        doc12.setPropertyValue("dc:title", "Doc 12");
        doc12 = session.createDocument(doc12);

        folder13 = session.createDocumentModel(folder1.getPathAsString(), "folder13", "Folder");
        folder13.setPropertyValue("dc:title", "Folder 13");
        folder13 = session.createDocument(folder13);

        folder131 = session.createDocumentModel(folder13.getPathAsString(), "folder131", "Folder");
        folder131.setPropertyValue("dc:title", "Folder 131");
        folder131 = session.createDocument(folder131);

        doc1311 = session.createDocumentModel(folder131.getPathAsString(), "doc1311", "File");
        doc1311.setPropertyValue("dc:title", "Doc 1311");
        doc1311 = session.createDocument(doc1311);

        session.checkIn(doc1311.getRef(), VersioningOption.MINOR, null);

        doc1312 = session.createDocumentModel(folder131.getPathAsString(), "doc1312", "File");
        doc1312.setPropertyValue("dc:title", "Doc 1312");
        doc1312 = session.createDocument(doc1312);

        session.checkIn(doc1312.getRef(), VersioningOption.MINOR, null);

        doc1312.setPropertyValue("dc:description", "forced checkout");
        doc1312 = session.saveDocument(doc1312);

        rootB = session.createDocumentModel("/", "rootB", "SnapshotableFolder");
        rootB = session.createDocument(rootB);

        folderB1 = session.createDocumentModel(rootB.getPathAsString(), "folderB1", "Folder");
        folderB1.setPropertyValue("dc:title", "Folder B1");
        folderB1 = session.createDocument(folderB1);

        folderB2 = session.createDocumentModel(rootB.getPathAsString(), "folderB2", "Folder");
        folderB2.setPropertyValue("dc:title", "Folder B2");
        folderB2 = session.createDocument(folderB2);

        folderB11 = session.createDocumentModel(folderB1.getPathAsString(), "folderB11", "Folder");
        folderB11.setPropertyValue("dc:title", "Folder B11");
        folderB11 = session.createDocument(folderB11);

        docB12 = session.createDocumentModel(folderB1.getPathAsString(), "docB12", "File");
        docB12.setPropertyValue("dc:title", "Doc B12");
        docB12 = session.createDocument(docB12);

        folderB13 = session.createDocumentModel(folderB1.getPathAsString(), "folderB13", "Folder");
        folderB13.setPropertyValue("dc:title", "Folder B13");
        folderB13 = session.createDocument(folderB13);

        docB131 = session.createDocumentModel(folderB13.getPathAsString(), "docB131", "File");
        docB131.setPropertyValue("dc:title", "Doc B131");
        docB131 = session.createDocument(docB131);

    }

    protected void dumpDBContent() throws Exception {
        System.out.println("\nDumping Live docs in repository");
        DocumentModelList docs = session.query("select * from Document where ecm:isVersion = 0 order by ecm:path");
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
        DocumentModelList docs = session.query("select * from Document where ecm:isVersion = 1");
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

    // helper method for creating a standard proxy
    protected DocumentModel createStandardProxy(DocumentModel targetDocument, DocumentModel parentForProxy)
            throws Exception {
        DocumentModel proxy = session.publishDocument(targetDocument, parentForProxy);
        Framework.getService(EventService.class).waitForAsyncCompletion();
        session.save();
        return proxy;
    }

    // helper method for creating a live proxy
    protected DocumentModel createLiveProxy(DocumentModel targetDocument, DocumentModel parentForProxy)
            throws Exception {
        DocumentModel proxy = session.createProxy(targetDocument.getRef(), parentForProxy.getRef());
        session.save();
        return proxy;
    }

    protected void dumpAllContent() {
        System.out.println("\nDumping all docs in repository");
        DocumentModelList docs = session.query("select * from Document order by ecm:path");
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
}
