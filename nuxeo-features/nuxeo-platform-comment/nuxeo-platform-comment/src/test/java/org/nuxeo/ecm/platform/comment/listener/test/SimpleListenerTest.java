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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.comment.api")
@Deploy("org.nuxeo.ecm.platform.comment")
@Deploy("org.nuxeo.ecm.platform.query.api")
public class SimpleListenerTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected CommentManager commentManager;


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

        // Create a first commentary
        Comment comment = new CommentImpl();
        comment.setText("This is my comment");
        comment.setParentId(doc.getId());
        commentManager.createComment(session, comment);

        // Create a second commentary
        Comment comment2 = new CommentImpl();
        comment2.setText("This is another comment");
        comment2.setParentId(doc.getId());
        commentManager.createComment(session, comment2);
        session.save();
        return doc;
    }

    protected void waitForAsyncExec() {
        Framework.getService(EventService.class).waitForAsyncCompletion();
    }

    @Test
    public void testDocumentRemovedCommentEventListener() throws Exception {
        DocumentModel doc = doCreateADocWithComments();
        assertNotNull(doc);

        List<Comment> comments = commentManager.getComments(session, doc.getId());
        assertFalse(comments.isEmpty());

        // Suppression the documents
        session.removeDocument(doc.getRef());
        session.save();

        // wait for the listener to be called
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        waitForAsyncExec();

        // Did all the comments have been deleted?
        comments = commentManager.getComments(session, doc.getId());
        assertTrue(comments.isEmpty());
    }

    @Test
    public void testCommentRemovedEventListener() throws Exception {
        DocumentModel doc = doCreateADocWithComments();
        assertNotNull(doc);

        List<Comment> comments = commentManager.getComments(session, doc.getId());
        assertEquals(2, comments.size());

        // Delete the first comment
        session.removeDocument(new IdRef(comments.get(0).getId()));
        // Check that the first comment has been deleted
        comments = commentManager.getComments(session, doc.getId());
        assertEquals(1, comments.size());

        // Delete the second comment
        session.removeDocument(new IdRef(comments.get(0).getId()));
        // Check that the second comment has been deleted
        comments = commentManager.getComments(session, doc.getId());
        assertEquals(0, comments.size());
    }

}
