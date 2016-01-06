/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.impl.service.PublisherServiceImpl;
import org.nuxeo.ecm.platform.publisher.remoting.client.ClientRemotePublicationNode;
import org.nuxeo.ecm.platform.publisher.remoting.client.ClientRemotePublicationTree;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.platform.types.api", //
        "org.nuxeo.ecm.platform.types.core", //
        "org.nuxeo.ecm.platform.versioning.api", //
        "org.nuxeo.ecm.platform.versioning", //
        "org.nuxeo.ecm.platform.query.api", //
        "org.nuxeo.ecm.platform.publisher.core.contrib", //
        "org.nuxeo.ecm.platform.publisher.core", //
})
@LocalDeploy("org.nuxeo.ecm.platform.publisher.test:OSGI-INF/publisher-remote-contrib-test.xml")
public class TestFakeRemoting {

    @Inject
    protected CoreSession session;

    @Inject
    protected PublisherService service;

    DocumentModel workspace;

    DocumentModel doc2Publish;

    protected void createInitialDocs() throws Exception {

        DocumentModel wsRoot = session.getDocument(new PathRef("/default-domain/workspaces"));

        workspace = session.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
        workspace.setProperty("dublincore", "title", "test WS");
        workspace = session.createDocument(workspace);

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

        doc2Publish = session.createDocumentModel(workspace.getPathAsString(), "file", "File");
        doc2Publish.setProperty("dublincore", "title", "MyDoc");

        Blob blob = Blobs.createBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        doc2Publish.setProperty("file", "content", blob);

        doc2Publish = session.createDocument(doc2Publish);

        session.save();
    }

    @Test
    public void testCorePublishingWithRemoting() throws Exception {
        createInitialDocs();

        // check service config
        List<String> treeNames = service.getAvailablePublicationTree();
        assertTrue(treeNames.contains("ClientRemoteTree"));

        // check publication tree
        PublicationTree tree = service.getPublicationTree("ClientRemoteTree", session, null);
        assertNotNull(tree);
        assertEquals("ClientRemoteTree", tree.getConfigName());

        List<PublicationNode> nodes = tree.getChildrenNodes();

        assertEquals(2, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        assertEquals("section2", nodes.get(1).getTitle());
        List<PublicationNode> subnodes = nodes.get(0).getChildrenNodes();
        assertEquals(1, subnodes.size());
        assertEquals("section11", subnodes.get(0).getTitle());

        PublicationNode targetNode = subnodes.get(0);

        // check treeconfigName propagation
        assertEquals(tree.getConfigName(), tree.getTreeConfigName());
        assertEquals(tree.getConfigName(), nodes.get(1).getTreeConfigName());

        assertEquals(tree.getSessionId(), nodes.get(1).getSessionId());

        // check publishing
        PublishedDocument pubDoc = tree.publish(doc2Publish, targetNode);
        assertNotNull(pubDoc);
        assertFalse(pubDoc.isPending());
        session.save();

        // check that versioning was done locally
        DocumentModel publishedDocVersion = session.getLastDocumentVersion(doc2Publish.getRef());
        assertNotNull(publishedDocVersion);
        assertTrue(publishedDocVersion.isVersion());
        assertEquals(doc2Publish.getRef().toString(), publishedDocVersion.getSourceId());

        // check tree features about proxy detection
        // List<PublishedDocument> detectedProxies =
        // tree.getExistingPublishedDocument(new
        // DocumentLocationImpl(publishedDocVersion)); XXXXXX! should work too
        List<PublishedDocument> detectedProxies = tree.getExistingPublishedDocument(new DocumentLocationImpl(
                doc2Publish));
        assertTrue(detectedProxies.size() == 1);
        detectedProxies = tree.getPublishedDocumentInNode(nodes.get(0));
        assertTrue(detectedProxies.size() == 0);

        detectedProxies = tree.getExistingPublishedDocument(new DocumentLocationImpl(publishedDocVersion));
        assertTrue(detectedProxies.size() == 1);
        detectedProxies = tree.getPublishedDocumentInNode(nodes.get(0));
        assertTrue(detectedProxies.size() == 0);

        detectedProxies = tree.getPublishedDocumentInNode(subnodes.get(0));
        assertTrue(detectedProxies.size() == 1);
        assertEquals(publishedDocVersion.getRef(), detectedProxies.get(0).getSourceDocumentRef());

        // check publishing 2
        PublicationNode targetNode2 = nodes.get(0);
        PublishedDocument pubDoc2 = tree.publish(doc2Publish, targetNode2);
        assertNotNull(pubDoc2);
        session.save();

        // check tree features about proxy detection
        detectedProxies = tree.getExistingPublishedDocument(new DocumentLocationImpl(publishedDocVersion));
        assertTrue(detectedProxies.size() == 2);

        detectedProxies = tree.getPublishedDocumentInNode(nodes.get(0));
        assertTrue(detectedProxies.size() == 1);
        assertEquals(publishedDocVersion.getRef(), detectedProxies.get(0).getSourceDocumentRef());
        assertEquals("MyLocalServer", detectedProxies.get(0).getSourceServer());

        detectedProxies = tree.getPublishedDocumentInNode(subnodes.get(0));
        assertTrue(detectedProxies.size() == 1);

        // check unpublish
        tree.unpublish(publishedDocVersion, targetNode);
        detectedProxies = tree.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish));
        assertEquals(1, detectedProxies.size());

        tree.unpublish(pubDoc2);
        detectedProxies = tree.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish));
        assertEquals(0, detectedProxies.size());

        tree.release();

    }

    @Test
    public void testWrappingThroughRemoting() throws Exception {
        createInitialDocs();

        // check proxy publication tree
        PublicationTree proxyTree = service.getPublicationTree("ClientRemoteTree", session, null);
        List<PublicationNode> nodes = proxyTree.getChildrenNodes();
        PublicationNode proxyNode = nodes.get(0);

        //
        String proxyTreeType = proxyTree.getTreeType();
        String proxyTechTreeType = proxyTree.getType();
        String proxyTreeClientId = proxyTree.getSessionId();
        assertEquals("RemoteTree", proxyTreeType);
        assertEquals("ProxyTree", proxyTechTreeType);

        assertEquals("ProxyNode", proxyNode.getType());
        assertEquals("CoreFolderPublicationNode", proxyNode.getNodeType());
        assertEquals(proxyTreeClientId, proxyNode.getSessionId());

        // check client publication tree
        PublicationTree clientTree = ((PublisherServiceImpl) service).getTreeBySid(proxyTreeClientId);
        String clientTreeType = clientTree.getTreeType();
        String clientTechTreeType = clientTree.getType();
        String clientTreeClientId = clientTree.getSessionId();
        assertEquals("RemoteTree", clientTreeType);
        assertEquals("ClientRemotePublicationTree", clientTechTreeType);
        assertEquals(proxyTreeClientId, clientTreeClientId);

        String serverSid = ((ClientRemotePublicationTree) clientTree).getRemoteSessionId();
        assertNotNull(serverSid);
        assertTrue(!serverSid.equals(proxyTreeClientId));

        List<PublicationNode> clientNodes = clientTree.getChildrenNodes();
        PublicationNode clientNode = clientNodes.get(0);

        assertEquals("ClientRemotePublicationNode", clientNode.getType());
        assertEquals("CoreFolderPublicationNode", clientNode.getNodeType());
        assertEquals(clientTreeClientId, clientNode.getSessionId());

        String nodeServerSid = ((ClientRemotePublicationNode) clientNode).getRemoteSessionId();
        assertEquals(serverSid, nodeServerSid);

        // check server publication tree
        PublicationTree serverTree = ((PublisherServiceImpl) service).getTreeBySid(serverSid);
        String serverTreeType = serverTree.getTreeType();
        String serverTechTreeType = serverTree.getType();
        String serverTreeClientId = serverTree.getSessionId();

        assertEquals("CoreTreeWithExternalDocs", serverTreeType);
        assertEquals("CoreTreeWithExternalDocs", serverTechTreeType);
        assertEquals(serverSid, serverTreeClientId);

        List<PublicationNode> serverNodes = serverTree.getChildrenNodes();
        PublicationNode serverNode = serverNodes.get(0);

        assertEquals("CoreFolderPublicationNode", serverNode.getType());
        assertEquals("CoreFolderPublicationNode", serverNode.getNodeType());
        assertEquals(serverTreeClientId, serverNode.getSessionId());

        proxyTree.release();
        clientTree.release();
        serverTree.release();

    }

    @Test
    public void testTitleWithSpaces() throws Exception {
        createInitialDocs();

        DocumentModel doc = session.createDocumentModel(workspace.getPathAsString(), "file2", "File");
        doc.setProperty("dublincore", "title", "A title with spaces");

        Blob blob = Blobs.createBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        doc.setProperty("file", "content", blob);

        doc = session.createDocument(doc);

        PublicationTree clientTree = service.getPublicationTree("ClientRemoteTree", session, null);
        List<PublicationNode> clientNodes = clientTree.getChildrenNodes();
        PublicationNode clientNode = clientNodes.get(0);

        PublishedDocument publisheDocument = clientTree.publish(doc, clientNode);
        assertNotNull(publisheDocument);
        assertTrue(publisheDocument.getPath().endsWith("A title with spaces"));

        clientTree.release();
    }

}
