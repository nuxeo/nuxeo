/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.impl.core.SectionPublicationTree;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.task.test.TaskUTConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public abstract class TestCorePublicationWithWorkflow extends SQLRepositoryTestCase {

    private DirectoryService directoryService;

    private PublisherService publisherService;

    private DocumentModel doc2Publish;

    protected HashMap<String,String> factoryParams = new HashMap<String,String>();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseTransactionEnabled", "false");

        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployBundle("org.nuxeo.ecm.platform.query.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.versioning.api");
        deployBundle("org.nuxeo.ecm.platform.versioning");
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.platform.publisher.core.contrib");
        deployBundle("org.nuxeo.ecm.platform.publisher.core");
        deployBundle("org.nuxeo.ecm.platform.publisher.task");
        deployBundle("org.nuxeo.ecm.platform.publisher.task.test");
        deployBundle(TaskUTConstants.API_BUNDLE_NAME);
        deployBundle(TaskUTConstants.CORE_BUNDLE_NAME);
        deployBundle(TaskUTConstants.TESTING_BUNDLE_NAME);
        fireFrameworkStarted();
        openSession();

        directoryService = Framework.getService(DirectoryService.class);
        publisherService = Framework.getLocalService(PublisherService.class);

        createDocumentToPublish();
        initializeACP();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    private void createDocumentToPublish() throws Exception {
        DocumentModel wsRoot = session.getDocument(new PathRef(
                "default-domain/workspaces"));

        DocumentModel ws = session.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = session.createDocument(ws);

        doc2Publish = session.createDocumentModel(ws.getPathAsString(), "file",
                "File");
        doc2Publish.setProperty("dublincore", "title", "MyDoc");

        Blob blob = new StringBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        blob.setMimeType("text/plain");
        doc2Publish.setProperty("file", "content", blob);

        doc2Publish = session.createDocument(doc2Publish);

        session.save();
    }

    private void initializeACP() throws Exception {
        DocumentModel sectionsRoot = session.getDocument(new PathRef(
                "default-domain/sections"));
        DocumentModel section1 = session.createDocumentModel(
                sectionsRoot.getPathAsString(), "section1", "Section");
        section1.setProperty("dublincore", "title", "section1");
        section1 = session.createDocument(section1);
        session.save();

        DocumentModel root = session.getRootDocument();
        ACP acp = session.getACP(root.getRef());
        ACL existingACL = acp.getOrCreateACL();
        existingACL.clear();
        existingACL.add(new ACE("myuser1", SecurityConstants.READ, true));
        existingACL.add(new ACE("myuser2", SecurityConstants.READ, true));
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

        DocumentModel ws1 = session.getDocument(new PathRef(
                "default-domain/workspaces/ws1"));
        acp = session.getACP(ws1.getRef());
        existingACL = acp.getOrCreateACL();
        existingACL.clear();
        existingACL.add(new ACE("myuser1", SecurityConstants.READ_WRITE, true));
        existingACL.add(new ACE("myuser2", SecurityConstants.EVERYTHING, true));
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
        changeUser("myuser1");
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        PublicationTree treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        PublicationNode targetNode = nodes.get(0);
        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish,
                targetNode);
        session.save();
        assertFalse(treeUser1.canUnpublish(publishedDocument));

        session.save();
        changeUser("myuser4");
        PublicationTree treeUser4 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        assertFalse(treeUser4.canUnpublish(publishedDocument));
        assertFalse(treeUser4.canManagePublishing(publishedDocument));

        session.save();
        changeUser("myuser3");
        PublicationTree treeUser3 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        assertFalse(treeUser3.canUnpublish(publishedDocument));
        assertFalse(treeUser4.canManagePublishing(publishedDocument));

        session.save();
        changeUser("myuser2");
        PublicationTree treeUser2 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        assertTrue(treeUser2.canUnpublish(publishedDocument));
        assertTrue(treeUser2.hasValidationTask(publishedDocument));
    }

    @Test
    public void testApprovePublication() throws Exception {
        changeUser("myuser1");
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        PublicationTree treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);

        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());

        PublicationNode targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));

        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish,
                targetNode);
        assertTrue(publishedDocument.isPending());
        assertEquals(1, treeUser1.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());

        // myuser3 can't see the document waiting for validation
        changeUser("myuser3");
        session.save(); // Save session to get modifications made by other
        // sessions
        PublicationTree treeUser3 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        assertEquals(0, treeUser3.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());

        // myuser2 can see it, it's the validator
        changeUser("myuser2");
        PublicationTree treeUser2 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        List<PublishedDocument> publishedDocuments = treeUser2.getExistingPublishedDocument(new DocumentLocationImpl(
                doc2Publish));
        assertEquals(1, publishedDocuments.size());

        publishedDocument = publishedDocuments.get(0);
        assertTrue(publishedDocument.isPending());

        treeUser2.validatorPublishDocument(publishedDocument, "Approved!");
        assertFalse(publishedDocument.isPending());

        // published so myuser3 can see it
        changeUser("myuser3");
        session.save(); // Save session to get modifications made by other
        // sessions (here, removing workflow ACL)
        assertEquals(1, treeUser3.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());
    }

    @Test
    public void testRejectPublication() throws Exception {
        changeUser("myuser1");
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        PublicationTree treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);

        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());

        PublicationNode targetNode = nodes.get(0);

        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish,
                targetNode);
        assertTrue(publishedDocument.isPending());
        assertEquals(1, treeUser1.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());

        // myuser3 can't see the document waiting for validation
        changeUser("myuser3");
        session.save(); // Save session to get modifications made by other
        // sessions
        PublicationTree treeUser3 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        assertEquals(0, treeUser3.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());

        // myuser2 can see it, it's the validator
        changeUser("myuser2");
        PublicationTree treeUser2 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        List<PublishedDocument> publishedDocuments = treeUser2.getExistingPublishedDocument(new DocumentLocationImpl(
                doc2Publish));
        assertEquals(1, publishedDocuments.size());

        publishedDocument = publishedDocuments.get(0);
        assertTrue(publishedDocument.isPending());

        assertTrue(treeUser2.canManagePublishing(publishedDocument));
        assertTrue(treeUser2.hasValidationTask(publishedDocument));
        treeUser2.validatorRejectPublication(publishedDocument, "Rejected!");
        assertTrue(publishedDocument.isPending());
        // No more document to approve
        assertEquals(0, treeUser2.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());

        // published so myuser3 still can't see it
        changeUser("myuser3");
        session.save(); // Save session to get modifications made by other
        // sessions (here, removing workflow ACL)
        assertEquals(0, treeUser3.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());

        // No more document published for myuser1
        changeUser("myuser1");
        assertEquals(0, treeUser1.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());

        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
        assertFalse(session.exists(proxy.getRef()));
    }

    @Test
    public void testFirstPublicationByValidator() throws Exception {
        changeUser("myuser2");
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        PublicationTree treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);

        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());

        PublicationNode targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));

        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish,
                targetNode);
        assertFalse(publishedDocument.isPending());
        // myuser3 can see the document
        changeUser("myuser3");
        session.save();
        PublicationTree treeUser3 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        assertEquals(1, treeUser3.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());
    }

    @Test
    public void testFirstPublicationByNonValidator() throws Exception {
        changeUser("myuser1");
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        PublicationTree treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);

        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());

        PublicationNode targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));

        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish,
                targetNode);
        assertTrue(publishedDocument.isPending());
        // myuser3 can't see the document
        changeUser("myuser3");
        session.save();
        PublicationTree treeUser3 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        assertEquals(0, treeUser3.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());

    }

    @Test
    public void testPublishOfAlreadyWaitingToBePublishedDocByNonValidator() throws Exception {
        // my user1 ask for publication
        changeUser("myuser1");
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        PublicationTree treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);

        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());

        PublicationNode targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));

        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish,
                targetNode);
        assertTrue(publishedDocument.isPending());
        assertEquals(1, treeUser1.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());
        session.save();
        // my user3 ask for publication
        changeUser("myuser3");
        defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));
        publishedDocument = treeUser1.publish(doc2Publish,
                targetNode);
        assertTrue(publishedDocument.isPending());
        session.save();
        // my user1 can still see the document
        changeUser("myuser1");
        defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);

        nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());

        targetNode = nodes.get(0);
        assertEquals(1, treeUser1.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());
        //my user 2 publish the document
        changeUser("myuser2");
        PublicationTree treeUser2 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        List<PublishedDocument> publishedDocuments = treeUser2.getExistingPublishedDocument(new DocumentLocationImpl(
                doc2Publish));
        assertEquals(1, publishedDocuments.size());

        publishedDocument = publishedDocuments.get(0);
        assertTrue(publishedDocument.isPending());

        treeUser2.validatorPublishDocument(publishedDocument, "Approved!");
        assertFalse(publishedDocument.isPending());

    }

    @Test
    public void testMultiplePublishThenPublishByValidator() throws Exception {
        // my user1 ask for publication
        changeUser("myuser1");
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        PublicationTree treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);

        List<PublicationNode> nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());

        PublicationNode targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));

        PublishedDocument publishedDocument = treeUser1.publish(doc2Publish,
                targetNode);
        assertTrue(publishedDocument.isPending());
        assertEquals(1, treeUser1.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());
        session.save();
        // my user3 ask for publication
        changeUser("myuser3");
        defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());
        targetNode = nodes.get(0);
        assertTrue(treeUser1.canPublishTo(targetNode));
        publishedDocument = treeUser1.publish(doc2Publish,
                targetNode);
        assertTrue(publishedDocument.isPending());
        session.save();
        // my user1 can still see the document
        changeUser("myuser1");
        defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);

        nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());

        targetNode = nodes.get(0);
        assertEquals(1, treeUser1.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());
        // my user4 don't see the document
        changeUser("myuser4");
        defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);

        nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());

        targetNode = nodes.get(0);
        assertEquals(0, treeUser1.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());

        //my user 2 publish the document
        changeUser("myuser2");
        PublicationTree treeUser2 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);
        List<PublishedDocument> publishedDocuments = treeUser2.getExistingPublishedDocument(new DocumentLocationImpl(
                doc2Publish));
        assertEquals(1, publishedDocuments.size());

        publishedDocument = publishedDocuments.get(0);
        assertTrue(publishedDocument.isPending());

        treeUser2.validatorPublishDocument(publishedDocument, "Approved!");
        assertFalse(publishedDocument.isPending());

        // my user4 see the document
        changeUser("myuser4");
        defaultTreeName = publisherService.getAvailablePublicationTree().get(
                0);
        treeUser1 = publisherService.getPublicationTree(
                defaultTreeName, session, factoryParams);

        nodes = treeUser1.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("section1", nodes.get(0).getTitle());

        targetNode = nodes.get(0);
        assertEquals(1, treeUser1.getExistingPublishedDocument(
                new DocumentLocationImpl(doc2Publish)).size());

    }
    private void changeUser(String userName) throws Exception {
        Session userdir = directoryService.open("userDirectory");
        DocumentModel userModel = userdir.getEntry(userName);
        // set it on session
        NuxeoPrincipal originalUser = (NuxeoPrincipal) session.getPrincipal();
        originalUser.setModel(userModel);
        originalUser.setName(userName);
    }

}
