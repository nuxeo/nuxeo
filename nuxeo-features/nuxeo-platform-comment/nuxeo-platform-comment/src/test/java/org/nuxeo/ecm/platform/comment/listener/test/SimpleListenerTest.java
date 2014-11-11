/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.listener.test;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.jms.AsyncProcessorConfig;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.comment.service.CommentService;
import org.nuxeo.ecm.platform.comment.service.CommentServiceHelper;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.runtime.api.Framework;

public class SimpleListenerTest extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.relations.api");
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployBundle("org.nuxeo.ecm.platform.comment.api");
        // deployBundle("org.nuxeo.ecm.platform.comment");
        deployBundle("org.nuxeo.ecm.platform.comment.core");

        deployContrib("org.nuxeo.ecm.platform.comment",
                "OSGI-INF/CommentService.xml");
        deployContrib("org.nuxeo.ecm.platform.comment",
                "OSGI-INF/comment-listener-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.comment.tests",
                "OSGI-INF/comment-jena-contrib.xml");

        openRepository();
    }

    protected int getCommentGrahNodesNumber() throws Exception {
        RelationManager rm = Framework.getService(RelationManager.class);

        List<Statement> statementList = rm.getStatements("documentComments");
        return statementList.size();
    }

    protected DocumentModel doCreateADocWithComments() throws Exception {

        DocumentModel domain = getCoreSession().createDocumentModel("Folder");
        domain.setProperty("dublincore", "title", "Domain");
        domain.setPathInfo("/", "domain");
        domain = getCoreSession().createDocument(domain);

        DocumentModel doc = getCoreSession().createDocumentModel("File");

        doc.setProperty("dublincore", "title", "MonTitre");
        doc.setPathInfo("/domain/", "TestFile");

        doc = getCoreSession().createDocument(doc);
        getCoreSession().save();
        AsyncProcessorConfig.setForceJMSUsage(false);

        // Create a first commentary
        CommentableDocument cDoc = doc.getAdapter(CommentableDocument.class);
        DocumentModel comment = getCoreSession().createDocumentModel("Comment");
        comment.setProperty("comment", "text", "This is my comment");
        comment = cDoc.addComment(comment);

        // Create a second commentary
        DocumentModel comment2 = getCoreSession().createDocumentModel("Comment");
        comment2.setProperty("comment", "text", "This is another  comment");
        comment2 = cDoc.addComment(comment);
        return doc;
    }

    protected void waitForAsyncExec() {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    public void testDocumentRemovedCommentEventListener() throws Exception {
        DocumentModel doc = doCreateADocWithComments();
        assertNotNull(doc);

        int nbLinks = getCommentGrahNodesNumber();
        assertTrue(nbLinks > 0);

        // Suppression the documents
        getCoreSession().removeDocument(doc.getRef());
        getCoreSession().save();

        // wait for the listener to be called
        waitForAsyncExec();

        // Did all the relations have been deleted?
        nbLinks = getCommentGrahNodesNumber();
        assertEquals(0, nbLinks);
    }

    public void testCommentRemovedEventListener() throws Exception {
        DocumentModel doc = doCreateADocWithComments();
        assertNotNull(doc);

        int nbLinks = getCommentGrahNodesNumber();
        assertEquals(2, nbLinks);

        // Get the comments
        CommentService commentService = CommentServiceHelper.getCommentService();
        List<DocumentModel> comments = commentService.getCommentManager().getComments(
                doc);

        // Delete the first comment
        getCoreSession().removeDocument(comments.get(0).getRef());
        // Check that the first relation has been deleted
        nbLinks = getCommentGrahNodesNumber();
        assertEquals(1, nbLinks);

        // Delete the second comment
        getCoreSession().removeDocument(comments.get(1).getRef());
        // Check that the second relation has been deleted
        nbLinks = getCommentGrahNodesNumber();
        assertEquals(0, nbLinks);
    }

}
