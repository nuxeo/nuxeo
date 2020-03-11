/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionsManager;
import org.nuxeo.ecm.platform.publisher.test.TestServiceWithCore.Populate;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test the {@link PublicationTree} implementations
 *
 * @author tiry
 */
@RepositoryConfig(cleanup = Granularity.METHOD, init = Populate.class)
@Deploy("org.nuxeo.ecm.platform.versioning")
@Deploy("org.nuxeo.ecm.platform.versioning.api")
public class TestServiceWithCore extends PublisherTestCase {

    public static class Populate implements RepositoryInit {

        @Override
        public void populate(CoreSession session) {
            DocumentModel wsRoot = session.getDocument(new PathRef("/default-domain/workspaces"));

            DocumentModel ws = session.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
            ws.setProperty("dublincore", "title", "test WS");
            ws = session.createDocument(ws);

            DocumentModel sectionsRoot = session.getDocument(new PathRef("/default-domain/sections"));

            DocumentModel section1 = session.createDocumentModel(sectionsRoot.getPathAsString(), "section1", "Section");
            section1.setProperty("dublincore", "title", "section1");
            section1 = session.createDocument(section1);

            DocumentModel section2 = session.createDocumentModel(sectionsRoot.getPathAsString(), "section2", "Section");
            section2.setProperty("dublincore", "title", "section2");
            section2 = session.createDocument(section2);

            DocumentModel section11 = session.createDocumentModel(section1.getPathAsString(), "section11", "Section");
            section11.setProperty("dublincore", "title", "section11");
            section11 = session.createDocument(section11);

            DocumentModel doc2Publish = session.createDocumentModel(ws.getPathAsString(), "file", "File");
            doc2Publish.setProperty("dublincore", "title", "MyDoc");

            Blob blob = Blobs.createBlob("SomeDummyContent");
            blob.setFilename("dummyBlob.txt");
            doc2Publish.setProperty("file", "content", blob);

            session.createDocument(doc2Publish);
        }

    }

    @Test
    public void testCorePublishing() {
        DocumentModel doc2Publish = session.getDocument(new PathRef("/default-domain/workspaces/ws1/file"));

        // check service config
        PublisherService service = Framework.getService(PublisherService.class);
        List<String> treeNames = service.getAvailablePublicationTree();
        assertEquals(1, treeNames.size());

        // check publication tree
        PublicationTree tree = service.getPublicationTree(treeNames.get(0), session, null);
        assertNotNull(tree);
        assertEquals("label.publication.tree.local.sections", tree.getTreeTitle());
        assertEquals("RootSectionsPublicationTree", tree.getTreeType());
        assertTrue(tree.getConfigName().startsWith("DefaultSectionsTree"));

        List<PublicationNode> nodes = tree.getChildrenNodes();

        assertEquals(2, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        assertEquals("section2", nodes.get(1).getTitle());
        List<PublicationNode> subnodes = nodes.get(0).getChildrenNodes();
        assertEquals(1, subnodes.size());
        assertEquals("section11", subnodes.get(0).getTitle());

        PublicationNode targetNode = subnodes.get(0);

        // check tree propagation
        assertEquals(tree, nodes.get(1).getTree());

        // check publishing
        PublishedDocument pubDoc = tree.publish(doc2Publish, targetNode);
        assertNotNull(pubDoc);
        assertEquals(1, tree.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
        session.save();

        assertEquals("test", pubDoc.getSourceRepositoryName());
        DocumentModel publishedDocVersion = session.getDocument(pubDoc.getSourceDocumentRef());
        assertNotNull(publishedDocVersion);
        assertTrue(publishedDocVersion.isVersion());
        assertEquals(doc2Publish.getRef().toString(), publishedDocVersion.getSourceId());

        // check tree features about proxy detection
        List<PublishedDocument> detectedProxies = tree.getExistingPublishedDocument(
                new DocumentLocationImpl(publishedDocVersion));
        assertEquals(1, detectedProxies.size());

        detectedProxies = tree.getPublishedDocumentInNode(nodes.get(0));
        assertEquals(0, detectedProxies.size());

        detectedProxies = tree.getPublishedDocumentInNode(subnodes.get(0));
        assertEquals(1, detectedProxies.size());
        assertEquals(publishedDocVersion.getRef(), detectedProxies.get(0).getSourceDocumentRef());

        // check publishing 2
        PublicationNode targetNode2 = nodes.get(0);
        PublishedDocument pubDoc2 = tree.publish(doc2Publish, targetNode2);
        assertNotNull(pubDoc2);
        assertEquals(2, tree.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
        session.save();

        assertEquals("test", pubDoc2.getSourceRepositoryName());
        DocumentModel publishedDocVersion2 = session.getDocument(pubDoc2.getSourceDocumentRef());
        assertNotNull(publishedDocVersion2);
        assertTrue(publishedDocVersion2.isVersion());
        assertEquals(doc2Publish.getRef().toString(), publishedDocVersion2.getSourceId());

        // check tree features about proxy detection
        detectedProxies = tree.getExistingPublishedDocument(new DocumentLocationImpl(publishedDocVersion));
        assertEquals(2, detectedProxies.size());

        detectedProxies = tree.getPublishedDocumentInNode(nodes.get(0));
        assertEquals(1, detectedProxies.size());
        assertEquals(publishedDocVersion2.getRef(), detectedProxies.get(0).getSourceDocumentRef());

        detectedProxies = tree.getPublishedDocumentInNode(subnodes.get(0));
        assertEquals(1, detectedProxies.size());

    }

    @Inject
    PublisherService service;

    @Test
    public void testWrapToPublicationNode() {
        DocumentModel doc2Publish = session.getDocument(new PathRef("/default-domain/workspaces/ws1/file"));

        PublicationTree tree = service.getPublicationTree(service.getAvailablePublicationTree().get(0), session, null);

        DocumentModel ws1 = session.getDocument(new PathRef("/default-domain/workspaces/ws1"));
        assertFalse(tree.isPublicationNode(ws1));

        DocumentModel section1 = session.getDocument(new PathRef("/default-domain/sections/section1"));
        assertTrue(tree.isPublicationNode(section1));

        PublicationNode targetNode = service.wrapToPublicationNode(section1, session);
        assertNotNull(targetNode);

        PublishedDocument pubDoc = tree.publish(doc2Publish, targetNode);
        assertNotNull(pubDoc);
        assertEquals(1, tree.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
    }

    @Test
    public void testWithRootSections() {
        DocumentModel doc2Publish = session.getDocument(new PathRef("/default-domain/workspaces/ws1/file"));

        RootSectionsManager rootSectionsManager = new RootSectionsManager(session);

        DocumentModel section1 = session.getDocument(new PathRef("/default-domain/sections/section1"));
        DocumentModel ws1 = session.getDocument(new PathRef("/default-domain/workspaces/ws1"));

        assertTrue(rootSectionsManager.canAddSection(section1, ws1));

        rootSectionsManager.addSection(section1.getId(), ws1);
        String[] sectionIdsArray = (String[]) ws1.getPropertyValue(RootSectionsManager.SECTIONS_PROPERTY_NAME);
        assertEquals(1, sectionIdsArray.length);

        PublisherService service = Framework.getService(PublisherService.class);

        PublicationTree tree = service.getPublicationTree(service.getAvailablePublicationTree().get(0), session, null,
                doc2Publish);
        assertNotNull(tree);

        List<PublicationNode> nodes = tree.getChildrenNodes();
        assertEquals(1, nodes.size());

        rootSectionsManager.removeSection(section1.getId(), ws1);
        sectionIdsArray = (String[]) ws1.getPropertyValue(RootSectionsManager.SECTIONS_PROPERTY_NAME);
        assertEquals(0, sectionIdsArray.length);

        DocumentModel section2 = session.getDocument(new PathRef("/default-domain/sections/section2"));
        DocumentModel section11 = session.getDocument(new PathRef("/default-domain/sections/section1/section11"));

        rootSectionsManager.addSection(section2.getId(), ws1);
        rootSectionsManager.addSection(section11.getId(), ws1);

        // "hack" to reset the RootSectionsFinder used by the tree
        // implementation
        tree.setCurrentDocument(doc2Publish);
        nodes = tree.getChildrenNodes();
        assertEquals(2, nodes.size());

        PublicationNode node = nodes.get(1);
        assertEquals(0, node.getChildrenNodes().size());

        assertNotNull(node.getParent());
        assertEquals(tree.getPath(), node.getParent().getPath());
    }

    protected void publishDocAndReopenSession(DocumentModel doc2Publish) {
        RootSectionsManager rootSectionsManager = new RootSectionsManager(session);
        DocumentModel section = session.getDocument(new PathRef("/default-domain/sections/section1"));
        DocumentModel workspace = session.getDocument(new PathRef("/default-domain/workspaces/ws1"));
        rootSectionsManager.addSection(section.getId(), workspace);
        PublisherService srv = Framework.getService(PublisherService.class);
        PublicationTree tree = srv.getPublicationTreeFor(doc2Publish, session);
        PublicationNode target = tree.getNodeByPath("/default-domain/sections/section1");
        srv.publish(doc2Publish, target);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testUnpublish() {
        DocumentModel doc2Publish = session.getDocument(new PathRef("/default-domain/workspaces/ws1/file"));
        publishDocAndReopenSession(doc2Publish);

        PathRef sectionRef = new PathRef("/default-domain/sections/section1");
        PathRef proxyRef = new PathRef("/default-domain/sections/section1/file");
        assertTrue(session.exists(proxyRef));
        PublisherService srv = Framework.getService(PublisherService.class);
        PublicationTree tree = srv.getPublicationTreeFor(doc2Publish, session);
        PublicationNode target = tree.getNodeByPath(sectionRef.value);
        // Unpublish check-in version (SUPNXP-3013)
        DocumentModel publishedDocVersion = session.getSourceDocument(proxyRef);
        tree.unpublish(publishedDocVersion, target);
        assertFalse(session.exists(proxyRef));
    }

}
