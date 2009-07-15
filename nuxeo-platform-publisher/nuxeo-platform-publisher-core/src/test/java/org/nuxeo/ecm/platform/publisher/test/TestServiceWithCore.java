package org.nuxeo.ecm.platform.publisher.test;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.impl.service.ProxyTree;
import org.nuxeo.ecm.platform.publisher.impl.service.PublisherServiceImpl;
import org.nuxeo.runtime.api.Framework;

import java.util.List;

/**
 *
 * Test the {@link PublicationTree} implementations
 *
 * @author tiry
 *
 */
public class TestServiceWithCore extends AbstractCorePublisherTest {

    public TestServiceWithCore(String name) {
        super(name);
    }

    public void testCorePublishing() throws Exception {

        createInitialDocs();

        // check service config
        PublisherService service = Framework.getLocalService(PublisherService.class);
        List<String> treeNames = service.getAvailablePublicationTree();
        assertTrue(treeNames.contains("DefaultSectionsTree"));

        // check publication tree
        PublicationTree tree = service.getPublicationTree(
                "DefaultSectionsTree", session, null);
        assertNotNull(tree);
        assertEquals("SectionPublicationTree", tree.getTreeType());
        assertEquals("DefaultSectionsTree", tree.getConfigName());

        Boolean isRemotable = false;
        if (tree instanceof ProxyTree) {
            ProxyTree rTree = (ProxyTree) tree;
            isRemotable = true;
        }
        assertTrue(isRemotable);
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
        assertEquals(1, tree.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());
        session.save();

        assertEquals("test", pubDoc.getSourceRepositoryName());
        DocumentModel publishedDocVersion = session.getDocument(pubDoc.getSourceDocumentRef());
        assertNotNull(publishedDocVersion);
        assertTrue(publishedDocVersion.isVersion());
        assertEquals(doc2Publish.getRef().toString(),
                publishedDocVersion.getSourceId());

        // check tree features about proxy detection
        List<PublishedDocument> detectedProxies = tree.getExistingPublishedDocument(new DocumentLocationImpl(
                publishedDocVersion));
        assertTrue(detectedProxies.size() == 1);

        detectedProxies = tree.getPublishedDocumentInNode(nodes.get(0));
        assertTrue(detectedProxies.size() == 0);

        detectedProxies = tree.getPublishedDocumentInNode(subnodes.get(0));
        assertTrue(detectedProxies.size() == 1);
        assertEquals(publishedDocVersion.getRef(),
                detectedProxies.get(0).getSourceDocumentRef());

        // check publishing 2
        PublicationNode targetNode2 = nodes.get(0);
        PublishedDocument pubDoc2 = tree.publish(doc2Publish, targetNode2);
        assertNotNull(pubDoc2);
        assertEquals(2, tree.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());
        session.save();

        assertEquals("test", pubDoc2.getSourceRepositoryName());
        DocumentModel publishedDocVersion2 = session.getDocument(pubDoc2.getSourceDocumentRef());
        assertNotNull(publishedDocVersion2);
        assertTrue(publishedDocVersion2.isVersion());
        assertEquals(doc2Publish.getRef().toString(),
                publishedDocVersion2.getSourceId());

        // check tree features about proxy detection
        detectedProxies = tree.getExistingPublishedDocument(new DocumentLocationImpl(
                publishedDocVersion));
        assertTrue(detectedProxies.size() == 2);

        detectedProxies = tree.getPublishedDocumentInNode(nodes.get(0));
        assertTrue(detectedProxies.size() == 1);
        assertEquals(publishedDocVersion2.getRef(),
                detectedProxies.get(0).getSourceDocumentRef());

        detectedProxies = tree.getPublishedDocumentInNode(subnodes.get(0));
        assertTrue(detectedProxies.size() == 1);

    }

    public void testCleanUp() throws Exception {
        createInitialDocs();

        deployContrib("org.nuxeo.ecm.platform.publisher.test",
                "OSGI-INF/publisher-remote-contrib-test.xml");

        assertEquals(0, PublisherServiceImpl.getLiveTreeCount());

        PublisherService service = Framework.getLocalService(PublisherService.class);

        // get a local tree
        PublicationTree ltree = service.getPublicationTree(
                "DefaultSectionsTree", session, null);
        assertEquals(1, PublisherServiceImpl.getLiveTreeCount());

        // get a remote tree
        PublicationTree rtree = service.getPublicationTree("ClientRemoteTree",
                session, null);
        assertEquals(3, PublisherServiceImpl.getLiveTreeCount());

        // release local tree
        ltree.release();
        assertEquals(2, PublisherServiceImpl.getLiveTreeCount());

        // release remote tree
        rtree.release();
        assertEquals(0, PublisherServiceImpl.getLiveTreeCount());

    }
}
