/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *     Nuno Cunha <ncunha@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Features(CommentFeature.class)
public abstract class AbstractTestCommentManager {

    public static final String FOLDER_COMMENT_CONTAINER = "/Folder/CommentContainer";

    @Inject
    protected CoreSession session;

    @Inject
    protected CommentManager commentManager;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected CoreFeature coreFeature;

    public abstract Class<? extends CommentManager> getType();

    @Before
    public void init() {
        DocumentModel domain = session.createDocumentModel("/", "Folder", "Folder");
        domain = session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel(domain.getPathAsString(), "File", "File");
        session.createDocument(doc);
        DocumentModel container = session.createDocumentModel(domain.getPathAsString(), "CommentContainer", "Folder");
        session.createDocument(container);
        session.save();
    }

    @Test
    public void testCreateComment() {

        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId("fakeId");

        try {
            commentManager.createComment(session, comment);
            fail("Creating a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        comment.setParentId(doc.getId());
        comment = commentManager.createComment(session, comment);
        assertEquals(author, comment.getAuthor());
        assertEquals(text, comment.getText());
        assertEquals(doc.getRef(), commentManager.getTopLevelDocumentRef(session, new IdRef(comment.getId())));
    }

    @Test
    public void testGetComment() {

        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(doc.getId());

        comment = commentManager.createComment(session, comment);

        try {
            commentManager.getComment(session, "fakeId");
            fail("Getting a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        comment = commentManager.getComment(session, comment.getId());
        assertEquals(author, comment.getAuthor());
        assertEquals(text, comment.getText());

    }

    @Test
    public void testDeleteComment() {

        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(doc.getId());

        comment = commentManager.createComment(session, comment);
        assertTrue(session.exists(new IdRef(comment.getId())));

        try {
            commentManager.deleteComment(session, "fakeId");
            fail("Deleting a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        commentManager.deleteComment(session, comment.getId());
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    @Test
    public void testCommentableDocumentAdapter() {

        DocumentModel doc = session.getDocument(new PathRef("/Folder/File"));
        CommentableDocument commentableDocument = doc.getAdapter(CommentableDocument.class);
        DocumentModel comment = session.createDocumentModel(COMMENT_DOC_TYPE);
        comment.setPropertyValue(COMMENT_TEXT, "Test");
        comment.setPropertyValue(COMMENT_AUTHOR, "bob");
        comment.setPropertyValue(COMMENT_CREATION_DATE, Calendar.getInstance());

        // Create a comment
        commentableDocument.addComment(comment);
        session.save();

        // Creation check
        assertEquals(1, commentableDocument.getComments().size());
        DocumentModel newComment = commentableDocument.getComments().get(0);
        assertThat(newComment.getPropertyValue(COMMENT_TEXT)).isEqualTo("Test");

        // Deletion check
        commentableDocument.removeComment(newComment);
        assertTrue(commentableDocument.getComments().isEmpty());

    }

    @Test
    public void testGetTopLevelCommentAncestor() {

        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);

        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("james", SecurityConstants.READ, true));
        session.setACP(doc.getRef(), acp, false);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(doc.getId());

        comment = commentManager.createComment(session, comment);
        assertEquals(doc.getRef(), commentManager.getTopLevelDocumentRef(session, new IdRef(comment.getId())));

        try (CloseableCoreSession jamesSession = coreFeature.openCoreSession("james")) {
            assertEquals(doc.getRef(),
                    commentManager.getTopLevelDocumentRef(jamesSession, new IdRef(comment.getId())));
        }

        try (CloseableCoreSession janeSession = coreFeature.openCoreSession("jane")) {
            assertEquals(doc.getRef(),
                    commentManager.getTopLevelDocumentRef(janeSession, new IdRef(comment.getId())));
            fail("jane should not be able to get the top level comment ancestor");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user jane does not have access to the comments of document %s",
                    doc.getId()), cse.getMessage());
        }
    }

    @Test
    public void testGetCommentThread() {

        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(doc.getId());

        comment = commentManager.createComment(session, comment);

        // Add a reply
        Comment reply = new CommentImpl();
        reply.setAuthor(author);
        reply.setText("I am a reply");
        reply.setParentId(comment.getId());
        reply = commentManager.createComment(session, reply);

        // Another reply
        Comment anotherReply = new CommentImpl();
        anotherReply.setAuthor(author);
        anotherReply.setText("I am a 2nd reply");
        anotherReply.setParentId(reply.getId());
        anotherReply = commentManager.createComment(session, anotherReply);

        DocumentModel anotherReplyDocModel = session.getDocument(new IdRef(anotherReply.getId()));
        DocumentRef topLevelCommentAncestor = commentManager.getTopLevelDocumentRef(session,
                anotherReplyDocModel.getRef());
        assertEquals(doc.getRef(), topLevelCommentAncestor);
    }

    @Test
    public void testGetEmptyComments() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        List<Comment> comments = commentManager.getComments(session, doc.getId());
        assertTrue(comments.isEmpty());
    }

    @Test
    public void testCommentManagerType() {
        assertEquals(getType(), commentManager.getClass());
    }

}
