/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.task.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.publisher.impl.core.SectionPublicationTree.CAN_ASK_FOR_PUBLISHING;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.content.template")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.versioning.api")
@Deploy("org.nuxeo.ecm.platform.versioning")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.publisher")
@Deploy("org.nuxeo.ecm.platform.publisher.test")
@Deploy("org.nuxeo.ecm.platform.task.api")
@Deploy("org.nuxeo.ecm.platform.task.core")
@Deploy("org.nuxeo.ecm.platform.task.testing")
public abstract class TestCorePublicationWithWorkflow {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    private PublisherService publisherService;

    private DocumentModel doc2Publish;

    protected HashMap<String, String> factoryParams = new HashMap<>();

    protected String defaultTreeName;

    @Before
    public void setUp() throws Exception {
        defaultTreeName = publisherService.getAvailablePublicationTree().get(0);
        createDocumentToPublish();
        initializeACP();
    }

    private void createDocumentToPublish() throws Exception {
        DocumentModel wsRoot = session.getDocument(new PathRef("/default-domain/workspaces"));

        DocumentModel ws = session.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = session.createDocument(ws);

        doc2Publish = session.createDocumentModel(ws.getPathAsString(), "file", "File");
        doc2Publish.setProperty("dublincore", "title", "MyDoc");

        Blob blob = Blobs.createBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        doc2Publish.setProperty("file", "content", blob);

        doc2Publish = session.createDocument(doc2Publish);

        session.save();
    }

    private void initializeACP() throws Exception {
        DocumentModel sectionsRoot = session.getDocument(new PathRef("/default-domain/sections"));
        DocumentModel section1 = session.createDocumentModel(sectionsRoot.getPathAsString(), "section1", "Section");
        section1.setProperty("dublincore", "title", "section1");
        section1 = session.createDocument(section1);
        session.save();

        DocumentModel root = session.getRootDocument();
        ACP acp = session.getACP(root.getRef());
        ACL existingACL = acp.getOrCreateACL();
        existingACL.clear();
        existingACL.add(new ACE("myuser1", SecurityConstants.READ, true));
        existingACL.add(new ACE("myuser3", SecurityConstants.READ, true));
        existingACL.add(new ACE("myuser4", SecurityConstants.READ, true));
        acp.addACL(existingACL);
        session.setACP(root.getRef(), acp, true);

        // give explicit CanAskForPublishing permission because
        // the users are not in the members group
        acp = session.getACP(sectionsRoot.getRef());
        existingACL = acp.getOrCreateACL();
        existingACL.add(new ACE("myuser1", CAN_ASK_FOR_PUBLISHING, true));
        existingACL.add(new ACE("myuser2", CAN_ASK_FOR_PUBLISHING, true));
        existingACL.add(new ACE("myuser3", CAN_ASK_FOR_PUBLISHING, true));
        existingACL.add(new ACE("myuser4", CAN_ASK_FOR_PUBLISHING, true));
        acp.addACL(existingACL);
        session.setACP(sectionsRoot.getRef(), acp, true);

        DocumentModel ws1 = session.getDocument(new PathRef("/default-domain/workspaces/ws1"));
        acp = session.getACP(ws1.getRef());
        existingACL = acp.getOrCreateACL();
        existingACL.clear();
        existingACL.add(new ACE("myuser1", SecurityConstants.READ_WRITE, true));
        acp.addACL(existingACL);
        session.setACP(ws1.getRef(), acp, true);

        acp = session.getACP(section1.getRef());
        existingACL = acp.getOrCreateACL();
        existingACL.clear();
        existingACL.add(new ACE("myuser1", SecurityConstants.READ, true));
        existingACL.add(new ACE("myuser2", SecurityConstants.EVERYTHING, true));
        acp.addACL(existingACL);
        session.setACP(section1.getRef(), acp, true);

        session.save();
    }

    @Test
    public void testRights() throws Exception {
        // myuser1 requests publishing
        DocumentRef publishedDocumentRef;
        CoreSession sessionUser1 = coreFeature.getCoreSession("myuser1");
        PublicationTree treeUser1 = publisherService.getPublicationTree(defaultTreeName, sessionUser1, factoryParams);
        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        PublicationNode targetNode = nodes.get(0);
        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish, targetNode);
        sessionUser1.save();
        assertFalse(treeUser1.canUnpublish(publishedDocument));
        publishedDocumentRef = new PathRef(publishedDocument.getPath());

        // myuser3 has no rights on proxy
        CoreSession sessionUser3 = coreFeature.getCoreSession("myuser3");
        assertFalse(sessionUser3.exists(publishedDocumentRef));

        // myuser4 has no rights on proxy
        CoreSession sessionUser4 = coreFeature.getCoreSession("myuser4");
        assertFalse(sessionUser4.exists(publishedDocumentRef));

        // myuser2 can unpublish or validate
        CoreSession sessionUser2 = coreFeature.getCoreSession("myuser2");
        publishedDocument = new SimpleCorePublishedDocument(sessionUser2.getDocument(publishedDocumentRef));
        PublicationTree treeUser2 = publisherService.getPublicationTree(defaultTreeName, sessionUser2, factoryParams);
        assertTrue(treeUser2.canUnpublish(publishedDocument));
        assertTrue(treeUser2.hasValidationTask(publishedDocument));
    }

    @Test
    public void testApprovePublication() throws Exception {
        // myuser1 requests publishing
        CoreSession sessionUser1 = coreFeature.getCoreSession("myuser1");
        PublicationTree treeUser1 = publisherService.getPublicationTree(defaultTreeName, sessionUser1, factoryParams);
        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        PublicationNode targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));
        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish, targetNode);
        assertTrue(publishedDocument.isPending());
        assertEquals(1, treeUser1.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
        sessionUser1.save();

        // myuser3 can't see the document waiting for validation
        CoreSession sessionUser3 = coreFeature.getCoreSession("myuser3");
        PublicationTree treeUser3 = publisherService.getPublicationTree(defaultTreeName, sessionUser3, factoryParams);
        assertEquals(0, treeUser3.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());

        // myuser2 can see it, it's the validator
        CoreSession sessionUser2 = coreFeature.getCoreSession("myuser2");
        PublicationTree treeUser2 = publisherService.getPublicationTree(defaultTreeName, sessionUser2, factoryParams);
        List<PublishedDocument> publishedDocuments = treeUser2.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish));
        assertEquals(1, publishedDocuments.size());
        publishedDocument = publishedDocuments.get(0);
        assertTrue(publishedDocument.isPending());
        // myuser2 must be able to validate without having Read permission on the live document 'doc2publish'
        treeUser2.validatorPublishDocument(publishedDocument, "Approved!");
        assertFalse(publishedDocument.isPending());

        // published so myuser3 can see it
        treeUser3 = publisherService.getPublicationTree(defaultTreeName, sessionUser3, factoryParams);
        assertEquals(1, treeUser3.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
        sessionUser3.save();
    }

    @Test
    public void testRejectPublication() throws Exception {
        // myuser1 requests publishing
        CoreSession sessionUser1 = coreFeature.getCoreSession("myuser1");
        PublicationTree treeUser1 = publisherService.getPublicationTree(defaultTreeName, sessionUser1, factoryParams);
        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        PublicationNode targetNode = nodes.get(0);
        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish, targetNode);
        assertTrue(publishedDocument.isPending());
        assertEquals(1, treeUser1.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
        sessionUser1.save(); // so that canManagePublishing's search works

        // myuser3 can't see the document waiting for validation
        CoreSession sessionUser3 = coreFeature.getCoreSession("myuser3");
        PublicationTree treeUser3 = publisherService.getPublicationTree(defaultTreeName, sessionUser3, factoryParams);
        assertEquals(0, treeUser3.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());

        // myuser2 can see it, it's the validator
        DocumentRef proxyRef;
        CoreSession sessionUser2 = coreFeature.getCoreSession("myuser2");
        PublicationTree treeUser2 = publisherService.getPublicationTree(defaultTreeName, sessionUser2, factoryParams);
        List<PublishedDocument> publishedDocuments = treeUser2.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish));
        assertEquals(1, publishedDocuments.size());
        publishedDocument = publishedDocuments.get(0);
        assertTrue(publishedDocument.isPending());
        assertTrue(treeUser2.canManagePublishing(publishedDocument));
        assertTrue(treeUser2.hasValidationTask(publishedDocument));
        // reject publication
        treeUser2.validatorRejectPublication(publishedDocument, "Rejected!");
        assertTrue(publishedDocument.isPending());
        proxyRef = ((SimpleCorePublishedDocument) publishedDocument).getProxy().getRef();
        // No more document to approve
        assertEquals(0, treeUser2.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());

        // not published so myuser3 still can't see it
        treeUser3 = publisherService.getPublicationTree(defaultTreeName, sessionUser3, factoryParams);
        assertEquals(0, treeUser3.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());

        // No more document published for myuser1
        treeUser1 = publisherService.getPublicationTree(defaultTreeName, sessionUser1, factoryParams);
        assertEquals(0, treeUser1.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
        assertFalse(sessionUser1.exists(proxyRef));
    }

    @Test
    public void testFirstPublicationByValidator() throws Exception {
        // myuser2 directly publishes a doc
        // TODO not sure it should, as it has no read access on the doc...
        CoreSession sessionUser2 = coreFeature.getCoreSession("myuser2");
        PublicationTree treeUser2 = publisherService.getPublicationTree(defaultTreeName, sessionUser2, factoryParams);
        List<PublicationNode> nodes = treeUser2.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        PublicationNode targetNode = nodes.get(0);
        assertTrue(treeUser2.canPublishTo(targetNode));
        PublishedDocument publishedDocument = treeUser2.publish(doc2Publish, targetNode);
        assertFalse(publishedDocument.isPending());

        // myuser3 can see the document
        CoreSession sessionUser3 = coreFeature.getCoreSession("myuser3");
        PublicationTree treeUser3 = publisherService.getPublicationTree(defaultTreeName, sessionUser3, factoryParams);
        assertEquals(1, treeUser3.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
    }

    @Test
    public void testFirstPublicationByNonValidator() throws Exception {
        // myuser1 requests publishing
        CoreSession sessionUser1 = coreFeature.getCoreSession("myuser1");
        PublicationTree treeUser1 = publisherService.getPublicationTree(defaultTreeName, sessionUser1, factoryParams);
        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        PublicationNode targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));
        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish, targetNode);
        assertTrue(publishedDocument.isPending());

        // myuser3 can't see the document
        CoreSession sessionUser3 = coreFeature.getCoreSession("myuser3");
        PublicationTree treeUser3 = publisherService.getPublicationTree(defaultTreeName, sessionUser3, factoryParams);
        assertEquals(0, treeUser3.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
    }

    @Test
    public void testPublishOfAlreadyWaitingToBePublishedDocByNonValidator() throws Exception {
        // my user1 ask for publication
        CoreSession sessionUser1 = coreFeature.getCoreSession("myuser1");
        PublicationTree treeUser1 = publisherService.getPublicationTree(defaultTreeName, sessionUser1, factoryParams);
        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        PublicationNode targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));
        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish, targetNode);
        assertTrue(publishedDocument.isPending());
        assertEquals(1, treeUser1.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
        sessionUser1.save();

        // my user3 ask for publication
        CoreSession sessionUser3 = coreFeature.getCoreSession("myuser3");
        PublicationTree treeUser3 = publisherService.getPublicationTree(defaultTreeName, sessionUser3, factoryParams);
        nodes = treeUser3.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        targetNode = nodes.get(0);
        assertTrue(treeUser3.canPublishTo(targetNode));
        publishedDocument = treeUser3.publish(doc2Publish, targetNode);
        assertTrue(publishedDocument.isPending());
        sessionUser3.save();

        // my user1 can still see the document
        treeUser1 = publisherService.getPublicationTree(defaultTreeName, sessionUser1, factoryParams);
        nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        assertEquals(1, treeUser1.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());

        // my user 2 publish the document
        CoreSession sessionUser2 = coreFeature.getCoreSession("myuser2");
        PublicationTree treeUser2 = publisherService.getPublicationTree(defaultTreeName, sessionUser2, factoryParams);
        List<PublishedDocument> publishedDocuments = treeUser2.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish));
        assertEquals(1, publishedDocuments.size());
        publishedDocument = publishedDocuments.get(0);
        assertTrue(publishedDocument.isPending());
        treeUser2.validatorPublishDocument(publishedDocument, "Approved!");
        assertFalse(publishedDocument.isPending());
        sessionUser2.save();
    }

    @Test
    public void testMultiplePublishThenPublishByValidator() throws Exception {
        // my user1 ask for publication
        CoreSession sessionUser1 = coreFeature.getCoreSession("myuser1");
        PublicationTree treeUser1 = publisherService.getPublicationTree(defaultTreeName, sessionUser1, factoryParams);
        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        PublicationNode targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));
        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish, targetNode);
        assertTrue(publishedDocument.isPending());
        assertEquals(1, treeUser1.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
        sessionUser1.save();

        // my user3 ask for publication
        CoreSession sessionUser3 = coreFeature.getCoreSession("myuser3");
        PublicationTree treeUser3 = publisherService.getPublicationTree(defaultTreeName, sessionUser3, factoryParams);
        nodes = treeUser3.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        targetNode = nodes.get(0);
        assertTrue(treeUser3.canPublishTo(targetNode));
        publishedDocument = treeUser3.publish(doc2Publish, targetNode);
        assertTrue(publishedDocument.isPending());
        sessionUser3.save();

        // my user1 can still see the document
        treeUser1 = publisherService.getPublicationTree(defaultTreeName, sessionUser1, factoryParams);
        nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        assertEquals(1, treeUser1.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());

        // my user4 don't see the document
        CoreSession sessionUser4 = coreFeature.getCoreSession("myuser4");
        PublicationTree treeUser4 = publisherService.getPublicationTree(defaultTreeName, sessionUser4, factoryParams);
        nodes = treeUser4.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        assertEquals(0, treeUser4.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());

        // my user 2 publish the document
        CoreSession sessionUser2 = coreFeature.getCoreSession("myuser2");
        PublicationTree treeUser2 = publisherService.getPublicationTree(defaultTreeName, sessionUser2, factoryParams);
        List<PublishedDocument> publishedDocuments = treeUser2.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish));
        assertEquals(1, publishedDocuments.size());
        publishedDocument = publishedDocuments.get(0);
        assertTrue(publishedDocument.isPending());
        treeUser2.validatorPublishDocument(publishedDocument, "Approved!");
        assertFalse(publishedDocument.isPending());
        sessionUser2.save();

        // my user4 see the document
        treeUser4 = publisherService.getPublicationTree(defaultTreeName, sessionUser4, factoryParams);
        nodes = treeUser4.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        assertEquals(1, treeUser4.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish)).size());
    }

}
