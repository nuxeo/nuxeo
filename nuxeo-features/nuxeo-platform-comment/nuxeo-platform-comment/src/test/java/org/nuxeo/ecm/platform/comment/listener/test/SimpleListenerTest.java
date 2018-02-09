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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.listener.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.jms.AsyncProcessorConfig;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.comment.service.CommentService;
import org.nuxeo.ecm.platform.comment.service.CommentServiceHelper;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({"org.nuxeo.ecm.relations.api", //
    "org.nuxeo.ecm.relations", //
    "org.nuxeo.ecm.relations.jena", //
    "org.nuxeo.ecm.platform.comment.api", //
    "org.nuxeo.ecm.platform.comment", //
    })
@Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/comment-jena-contrib.xml")
public class SimpleListenerTest {

    @Inject
    protected CoreSession session;

    protected int getCommentGrahNodesNumber() throws Exception {
        RelationManager rm = Framework.getService(RelationManager.class);

        List<Statement> statementList = rm.getGraphByName("documentComments").getStatements();
        return statementList.size();
    }

    protected DocumentModel doCreateADocWithComments() throws Exception {

        DocumentModel domain = session.createDocumentModel("Folder");
        domain.setProperty("dublincore", "title", "Domain");
        domain.setPathInfo("/", "domain");
        domain = session.createDocument(domain);

        DocumentModel doc = session.createDocumentModel("File");

        doc.setProperty("dublincore", "title", "MonTitre");
        doc.setPathInfo("/domain/", "TestFile");

        doc = session.createDocument(doc);
        session.save();
        AsyncProcessorConfig.setForceJMSUsage(false);

        // Create a first commentary
        CommentableDocument cDoc = doc.getAdapter(CommentableDocument.class);
        DocumentModel comment = session.createDocumentModel(CommentsConstants.COMMENT_DOC_TYPE);
        comment.setProperty("comment", "text", "This is my comment");
        comment = cDoc.addComment(comment);

        // Create a second commentary
        DocumentModel comment2 = session.createDocumentModel(CommentsConstants.COMMENT_DOC_TYPE);
        comment2.setProperty("comment", "text", "This is another  comment");
        comment2 = cDoc.addComment(comment2);
        return doc;
    }

    protected void waitForAsyncExec() {
        Framework.getService(EventService.class).waitForAsyncCompletion();
    }

    @Test
    public void testDocumentRemovedCommentEventListener() throws Exception {
        DocumentModel doc = doCreateADocWithComments();
        assertNotNull(doc);

        int nbLinks = getCommentGrahNodesNumber();
        assertTrue(nbLinks > 0);

        // Suppression the documents
        session.removeDocument(doc.getRef());
        session.save();

        // wait for the listener to be called
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        waitForAsyncExec();

        // Did all the relations have been deleted?
        nbLinks = getCommentGrahNodesNumber();
        assertEquals(0, nbLinks);
    }

    @Test
    public void testCommentRemovedEventListener() throws Exception {
        DocumentModel doc = doCreateADocWithComments();
        assertNotNull(doc);

        int nbLinks = getCommentGrahNodesNumber();
        assertEquals(2, nbLinks);

        // Get the comments
        CommentService commentService = CommentServiceHelper.getCommentService();
        List<DocumentModel> comments = commentService.getCommentManager().getComments(doc);

        // Delete the first comment
        session.removeDocument(comments.get(0).getRef());
        // Check that the first relation has been deleted
        nbLinks = getCommentGrahNodesNumber();
        assertEquals(1, nbLinks);

        // Delete the second comment
        session.removeDocument(comments.get(1).getRef());
        // Check that the second relation has been deleted
        nbLinks = getCommentGrahNodesNumber();
        assertEquals(0, nbLinks);
    }

}
