/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = PublishRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.versioning.api")
@Deploy("org.nuxeo.ecm.platform.versioning")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.publisher.core.contrib")
@Deploy("org.nuxeo.ecm.platform.publisher.core")
@Deploy("org.nuxeo.ecm.platform.publisher.task")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.task.core")
@Deploy("org.nuxeo.ecm.platform.task.testing")
@Deploy("org.nuxeo.snapshot")
@Deploy("org.nuxeo.snapshot:relations-default-jena-contrib.xml")
public class TestTreePublication {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected PublisherService publisherService;

    protected DocumentModel root;

    protected DocumentModel folder1;

    protected DocumentModel folder2;

    protected DocumentModel folder11;

    protected DocumentModel doc12;

    protected DocumentModel folder13;

    protected DocumentModel folder131;

    protected DocumentModel doc1311;

    protected DocumentModel doc1312;

    protected final boolean verbose = false;

    private StringBuilder debug = new StringBuilder();

    protected void maybeSleepToNextSecond() {
        coreFeature.getStorageConfiguration().maybeSleepToNextSecond();
    }

    protected void debugCreationDate(DocumentModel doc) {
        debug.append(doc.getName() + "@time=" + System.currentTimeMillis() + ", ");
    }

    protected void buildTree() throws Exception {
        root = session.createDocumentModel("/", "root", "SnapshotableFolder");
        root = session.createDocument(root);
        debugCreationDate(root);

        folder1 = session.createDocumentModel(root.getPathAsString(), "folder1", "Folder");
        folder1.setPropertyValue("dc:title", "Folder 1");
        folder1 = session.createDocument(folder1);
        debugCreationDate(folder1);

        folder2 = session.createDocumentModel(root.getPathAsString(), "folder2", "Folder");
        folder2.setPropertyValue("dc:title", "Folder 2");
        folder2 = session.createDocument(folder2);
        debugCreationDate(folder2);

        folder11 = session.createDocumentModel(folder1.getPathAsString(), "folder11", "Folder");
        folder11.setPropertyValue("dc:title", "Folder 11");
        folder11 = session.createDocument(folder11);
        debugCreationDate(folder11);

        doc12 = session.createDocumentModel(folder1.getPathAsString(), "doc12", "File");
        doc12.setPropertyValue("dc:title", "Doc 12");
        doc12 = session.createDocument(doc12);
        debugCreationDate(doc12);

        folder13 = session.createDocumentModel(folder1.getPathAsString(), "folder13", "Folder");
        folder13.setPropertyValue("dc:title", "Folder 13");
        folder13 = session.createDocument(folder13);
        debugCreationDate(folder13);

        folder131 = session.createDocumentModel(folder13.getPathAsString(), "folder131", "Folder");
        folder131.setPropertyValue("dc:title", "Folder 131");
        folder131 = session.createDocument(folder131);
        debugCreationDate(folder131);

        doc1311 = session.createDocumentModel(folder131.getPathAsString(), "doc1311", "File");
        doc1311.setPropertyValue("dc:title", "Doc 1311");
        doc1311 = session.createDocument(doc1311);
        debugCreationDate(doc1311);

        maybeSleepToNextSecond();

        session.checkIn(doc1311.getRef(), VersioningOption.MINOR, null);

        doc1312 = session.createDocumentModel(folder131.getPathAsString(), "doc1312", "File");
        doc1312.setPropertyValue("dc:title", "Doc 1312");
        doc1312 = session.createDocument(doc1312);
        debugCreationDate(doc1312);

        maybeSleepToNextSecond();

        session.checkIn(doc1312.getRef(), VersioningOption.MINOR, null);

        doc1312.setPropertyValue("dc:description", "forced checkout");
        doc1312 = session.saveDocument(doc1312);

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

    @Test
    public void shouldPublishATree() throws Exception {

        buildTree();

        // setup tree
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(0);
        PublicationTree tree = publisherService.getPublicationTree(defaultTreeName, session, null);

        List<PublicationNode> nodes = tree.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("Section1", nodes.get(0).getTitle());

        PublicationNode targetNode = nodes.get(0);
        assertTrue(tree.canPublishTo(targetNode));

        maybeSleepToNextSecond();

        tree.publish(root, targetNode);
        session.save();
        debug.append("published@time=" + System.currentTimeMillis() + ", ");
        if (verbose) {
            dumpDBContent();
        }

        DocumentModelList docs = session.query("select * from Document where ecm:path STARTSWITH '/default-domain/sections/' order by ecm:path");
        docs.remove(0); // remove head
        assertEquals(9, docs.size()); // 9 proxies
        for (DocumentModel doc : docs) {
            assertTrue(doc.isProxy());
            if (doc.getName().equals("doc1312")) {
                assertEquals("0.2", doc.getVersionLabel());
            } else {
                assertEquals("0.1", doc.getVersionLabel());
            }
        }

        // change a leaf
        doc1311.setPropertyValue("dc:description", "forced checkout");
        doc1311 = session.saveDocument(doc1311);
        session.save();

        // republish
        tree.publish(root, targetNode);
        session.save();
        debug.append("published@time=" + System.currentTimeMillis() + ", ");
        if (verbose) {
            dumpDBContent();
        }

        docs = session.query("select * from Document where ecm:path STARTSWITH '/default-domain/sections/' order by ecm:path");
        docs.remove(0); // remove head
        assertEquals(9, docs.size()); // 9 proxies

        // gather debug info
        for (DocumentModel doc : docs) {
            debug.append(doc.getName() + "=" + doc.getVersionLabel() + ", ");
        }
        for (DocumentModel doc : docs) {
            assertTrue(doc.isProxy());
            if (doc.getName().startsWith("doc131") || doc.getName().startsWith("folder13")
                    || doc.getName().equals("folder1") || doc.getName().equals("root")) {
                assertEquals(debug + " bad version for: " + doc.getName(), "0.2", doc.getVersionLabel());
            } else {
                assertEquals(debug + " bad version for: " + doc.getName(), "0.1", doc.getVersionLabel());
            }
        }
    }

}
