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
 *     Arnaud Kervern
 */
package org.nuxeo.snapshot;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.snapshot.Snapshot;
import org.nuxeo.snapshot.Snapshotable;
import org.nuxeo.snapshot.SnapshotableAdapter;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(init = PublishRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.snapshot")
public class TestSnapshotingAndProxies extends AbstractTestSnapshot {

    // helper method that provides children listing using different technics
    // in the target solution this should probably be encapsulated in a
    // PageProvider ...
    protected List<DocumentModel> getChildren(DocumentModel parent) {

        String query = "select * from Document where ecm:parentId = ";

        if (parent.isProxy()) {
            DocumentModel target = session.getDocument(new IdRef(parent.getSourceId()));
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

    @Override
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

    @Override
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

        createStandardProxy(docB131, folder11);
        createLiveProxy(docB12, folder11);
        if (verbose) {
            System.out.println("## Tree after publish");
            dumpDBContent();
        }

        session.save();

        Snapshotable snapshotable = root.getAdapter(Snapshotable.class);
        assertNotNull(snapshotable);

        Snapshot snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);
        assertNotNull(snapshot);

        // find folder 1
        DocumentModel tmp = findFirstDocument(snapshot.getChildren(), "folder1");
        assertNotNull(tmp);
        snapshot = tmp.getAdapter(Snapshot.class);
        assertNotNull(snapshot);

        // find folder 11
        tmp = findFirstDocument(snapshot.getChildren(), "folder11");
        assertNotNull(tmp);
        snapshot = tmp.getAdapter(Snapshot.class);
        assertNotNull(snapshot);

        // find docB131
        tmp = findFirstDocument(snapshot.getChildren(), "docB131");
        assertNotNull(tmp);
        assertTrue(tmp.isProxy());

        if (verbose) {
            System.out.println("## Tree after first snapshot");
            dumpDBContent();
        }

        session.createDocument(session.createDocumentModel(root.getPathAsString(), "tmp2", "File"));
        session.save();

        // Create another one
        snapshot = snapshotable.createSnapshot(VersioningOption.MAJOR);

        if (verbose) {
            System.out.println("## Tree after second snapshot");
            dumpDBContent();
        }
    }

    protected DocumentModel findFirstDocument(List<DocumentModel> docs, String nameRegex) {
        for (DocumentModel doc : docs) {
            if (doc.getName().matches(nameRegex)) {
                return doc;
            }
        }
        return null;
    }

    protected DocumentModel rootClassifications, folderClassification1, folderC11, folderClassification11Proxy,
            docClassification111, docClassification112Proxy;

    /**
     * Children added in proxyFolder should be in its targetFolder, and proxyFolder's getChildren return its target
     * folder's. If (probably) Nuxeo wants to keep the current proxy behaviour, it could be done either as (EasySOA-)
     * custom methods, or custom type / facet (?).
     *
     * @throws Exception
     * @author mdutoo Open Wide - EasySOA use case
     */
    @Ignore
    @Test
    public void testAddAndGetProxyFolderChildren() throws Exception {

        buildTree();

        // create third, classification & proxy-only tree

        // root of classifications
        rootClassifications = session.createDocumentModel("/", "rootClassifications", "SnapshotableFolder");
        rootClassifications = session.createDocument(rootClassifications);

        // first classification folder (ex. "my folders" or
        // "business X folders")...
        folderClassification1 = session.createDocumentModel(rootClassifications.getPathAsString(),
                "folderClassification1", "Folder");
        folderClassification1.setPropertyValue("dc:title", "Folder Classification1");
        folderClassification1 = session.createDocument(folderClassification1);

        // containing a proxied folder of the "actual" model
        folderClassification11Proxy = createLiveProxy(folder1, folderClassification1);

        // adding a new doc in this proxy folder
        docClassification111 = session.createDocumentModel(folderClassification11Proxy.getPathAsString(),
                "docClassification111", "File");
        docClassification111.setPropertyValue("dc:title", "Doc Classification111");
        docClassification111 = session.createDocument(docClassification111);

        // adding an (elsewhere) existing doc in this proxy folder
        docClassification112Proxy = createLiveProxy(docB131, folderClassification11Proxy);

        dumpAllContent();

        // checking that the proxy folder contains the new doc :
        assertTrue(contains(session.getChildren(folderClassification11Proxy.getRef()), docClassification111));

        // checking that the proxy folder contains the actual existing doc, and
        // not its proxy :
        assertTrue(contains(session.getChildren(folderClassification11Proxy.getRef()), docB131));
        assertFalse(contains(session.getChildren(folderClassification11Proxy.getRef()), docClassification112Proxy));

        // checking that the new & existing docs is also in the actual folder :
        assertTrue(contains(session.getChildren(folder1.getRef()), docB131));
        assertTrue(contains(session.getChildren(folder1.getRef()), docClassification111));

    }

    private boolean contains(DocumentModelList docList, DocumentModel lookedForDoc) {
        for (DocumentModel child : docList) {
            if (child.getPath().equals(lookedForDoc.getPath())) {
                return true;
            }
        }
        return false;
    }
}
