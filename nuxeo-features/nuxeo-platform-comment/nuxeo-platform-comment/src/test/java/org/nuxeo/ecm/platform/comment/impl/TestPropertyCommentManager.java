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

package org.nuxeo.ecm.platform.comment.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.comment.api.Comments.commentToDocumentModel;
import static org.nuxeo.ecm.platform.comment.api.Comments.newComment;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;

import java.time.Instant;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.comment.AbstractTestCommentManager;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.RandomBug;

/**
 * @since 10.3
 */
@Deploy("org.nuxeo.ecm.platform.query.api")
public class TestPropertyCommentManager extends AbstractTestCommentManager {

    @Test
    public void shouldThrowExceptionWhenGettingNonExistingComment() {
        try {
            commentManager.getComment(session, "nonExistingCommentId");
            fail("This test is expected to fail!");
        } catch (CommentNotFoundException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("The comment nonExistingCommentId does not exist.", e.getMessage());
        }

    }

    @Test
    public void shouldThrowExceptionWhenGettingNonExistingExternalComment() {
        try {
            commentManager.getExternalComment(session, "nonExistingExternalCommentId");
            fail("This test is expected to fail!");
        } catch (CommentNotFoundException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("The external comment nonExistingExternalCommentId does not exist.", e.getMessage());
        }
    }

    @Test
    public void shouldThrowExceptionWhenCreatingCommentForNonExistingParent() {
        try {
            commentManager.createComment(session,
                    getSampleComment("nonExistingId", session.getPrincipal().getName(), "some text"));
            fail("This test is expected to fail!");
        } catch (CommentNotFoundException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("The document or comment nonExistingId does not exist.", e.getMessage());
        }

    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistingComment() {
        try {
            commentManager.updateComment(session, "nonExistingCommentId", new CommentImpl());
            fail("This test is expected to fail!");
        } catch (CommentNotFoundException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("The comment nonExistingCommentId does not exist.", e.getMessage());
        }
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistingExternalComment() {
        try {
            commentManager.updateExternalComment(session, "nonExistingExternalCommentId", new CommentImpl());
            fail("This test is expected to fail!");
        } catch (CommentNotFoundException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("The external comment nonExistingExternalCommentId does not exist.", e.getMessage());
        }
    }

    @Test
    public void shouldThrowExceptionWhenDeletingNonExistingComment() {
        try {
            commentManager.deleteComment(session, "nonExistingCommentId");
            fail("This test is expected to fail!");
        } catch (CommentNotFoundException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("The comment nonExistingCommentId does not exist.", e.getMessage());
        }
    }

    @Test
    public void shouldThrowExceptionWhenDeletingNonExistingExternalComment() {
        try {
            commentManager.deleteExternalComment(session, "nonExistingExternalCommentId");
            fail("This test is expected to fail!");
        } catch (CommentNotFoundException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("The external comment nonExistingExternalCommentId does not exist.", e.getMessage());
        }
    }

    @Test
    public void shouldReturnCreatedObjectWhenCreatingComment() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);
        session.save();

        Comment commentToCreate = getSampleComment(doc.getId(), session.getPrincipal().getName(), "some text");

        Comment createdComment = commentManager.createComment(session, commentToCreate);
        assertNotNull(createdComment);
        assertNotNull(createdComment.getId());
        assertEquals(commentToCreate.getCreationDate(), createdComment.getCreationDate());
        assertNotNull(createdComment.getAncestorIds());
        assertEquals(1, createdComment.getAncestorIds().size());
        assertTrue(createdComment.getAncestorIds().contains(doc.getId()));
        assertEquals(commentToCreate.getAuthor(), createdComment.getAuthor());
        assertEquals(doc.getId(), createdComment.getParentId());
        assertEquals(commentToCreate.getText(), createdComment.getText());
        assertNull(createdComment.getModificationDate());
    }

    @Test
    public void shouldReturnCreatedObjectWithCreationDateWhenCreatingCommentWithoutCreationDate() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);
        session.save();

        Comment commentToCreate = getSampleComment(doc.getId(), session.getPrincipal().getName(), "some text");
        commentToCreate.setCreationDate(null);

        Comment createdComment = commentManager.createComment(session, commentToCreate);
        assertNotNull(createdComment);
        assertNotNull(createdComment.getId());
        assertNotNull(createdComment.getCreationDate());
        assertNotNull(createdComment.getAncestorIds());
        assertEquals(1, createdComment.getAncestorIds().size());
        assertTrue(createdComment.getAncestorIds().contains(doc.getId()));
        assertEquals(commentToCreate.getAuthor(), createdComment.getAuthor());
        assertEquals(doc.getId(), createdComment.getParentId());
        assertEquals(commentToCreate.getText(), createdComment.getText());
        assertNull(createdComment.getModificationDate());
    }

    @Test
    public void shouldReturnObjectWhenGettingExistingComment() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);
        session.save();

        Comment comment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "some text"));

        Comment storedComment = newComment(session.getDocument(new IdRef(comment.getId())));
        assertNotNull(storedComment);
        assertEquals(comment, storedComment);
    }

    @Test
    public void shouldReturnObjectWhenGettingExistingExternalComment() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment comment = getSampleComment(doc.getId(), session.getPrincipal().getName(), "some text");
        ((CommentImpl) comment).setEntityId("anEntityId");
        ((CommentImpl) comment).setEntity("anEntityByItself");
        ((CommentImpl) comment).setOrigin("anOriginForExternalEntity");

        comment = commentManager.createComment(session, comment);

        session.save();

        Comment storedExternalComment = commentManager.getExternalComment(session, "anEntityId");
        assertNotNull(storedExternalComment);
        assertEquals(comment, storedExternalComment);
    }

    @Test
    public void shouldReflectUpdatedFieldsWhenUpdatingExistingComment() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment comment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "some text"));

        session.save();

        comment.setText("my updated text!");
        comment.setModificationDate(Instant.now());
        commentManager.updateComment(session, comment.getId(), comment);

        Comment storedComment = newComment(session.getDocument(new IdRef(comment.getId())));
        assertEquals(comment, storedComment);
    }

    @Test
    public void shouldReflectUpdatedFieldsWhenUpdatingExistingCommentWithoutProvidingModificationDate() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment comment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "some text"));

        session.save();

        comment.setText("my updated text!");
        commentManager.updateComment(session, comment.getId(), comment);

        Comment storedComment = newComment(session.getDocument(new IdRef(comment.getId())));
        assertEquals(comment.getId(), storedComment.getId());
        assertEquals(comment.getAuthor(), storedComment.getAuthor());
        assertEquals(comment.getCreationDate(), storedComment.getCreationDate());
        assertEquals(comment.getText(), storedComment.getText());
        assertEquals(comment.getParentId(), storedComment.getParentId());
        assertEquals(comment.getAncestorIds(), storedComment.getAncestorIds());
        assertNotNull(storedComment.getModificationDate());
    }

    @Test
    public void shouldReflectUpdatedFieldsWhenUpdatingExistingExternalComment() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment comment = getSampleComment(doc.getId(), session.getPrincipal().getName(), "some text");
        ((CommentImpl) comment).setEntityId("anEntityId");
        ((CommentImpl) comment).setEntity("anEntityByItself");
        ((CommentImpl) comment).setOrigin("anOriginForExternalEntity");

        comment = commentManager.createComment(session, comment);

        session.save();

        comment.setText("my updated text!");
        comment.setModificationDate(Instant.now());
        Comment storedComment = commentManager.updateExternalComment(session, "anEntityId", comment);
        assertEquals(comment, storedComment);
    }

    @Test
    public void shouldNotBeAvailableWhenExistingCommentIsDeleted() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment comment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "some text"));

        session.save();

        commentManager.deleteComment(session, comment.getId());
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    @Test
    public void shouldNotBeAvailableWhenExistingExternalCommentIsDeleted() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment comment = getSampleComment(doc.getId(), session.getPrincipal().getName(), "some text");
        ((CommentImpl) comment).setEntityId("anEntityId");
        ((CommentImpl) comment).setEntity("anEntityByItself");
        ((CommentImpl) comment).setOrigin("anOriginForExternalEntity");

        comment = commentManager.createComment(session, comment);

        session.save();

        commentManager.deleteExternalComment(session, "anEntityId");
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    @Test
    public void shouldReturnEmptyListWhenDocumentHasNoComments() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);
        session.save();

        List<DocumentModel> comments = commentManager.getComments(session, doc);
        assertNotNull(comments);
        assertEquals(0, comments.size());
    }

    @Test
    public void shouldReturnAllCommentsHasDocumentModelsSortedByCreationDateAscendingWhenDocumentHasComments() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment firstComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "first comment"));
        Comment secondComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "second comment"));
        Comment thirdComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "third comment"));
        Comment fourthComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "fourth comment"));

        session.save();

        List<DocumentModel> comments = commentManager.getComments(session, doc);
        assertNotNull(comments);
        assertEquals(4, comments.size());
        assertEquals(newComment(comments.get(0)), firstComment);
        assertEquals(newComment(comments.get(1)), secondComment);
        assertEquals(newComment(comments.get(2)), thirdComment);
        assertEquals(newComment(comments.get(3)), fourthComment);
    }

    @Test
    @RandomBug.Repeat(issue = "NXP-26144")
    public void shouldReturnAllCommentsSortedByCreationDateDescendingWhenDocumentHasComments() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment firstComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "first comment"));
        Comment secondComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "second comment"));
        Comment thirdComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "third comment"));
        Comment fourthComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "fourth comment"));

        session.save();

        List<Comment> comments = commentManager.getComments(session, doc.getId(), false);
        assertNotNull(comments);
        assertEquals(4, comments.size());
        assertEquals(comments.get(0), fourthComment);
        assertEquals(comments.get(1), thirdComment);
        assertEquals(comments.get(2), secondComment);
        assertEquals(comments.get(3), firstComment);
    }

    @Test
    public void shouldReturnCommentsPaginatedAndSortedByCreationDateDescendingWhenDocumentHasComments() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment firstComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "first comment"));
        Comment secondComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "second comment"));
        Comment thirdComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "third comment"));
        Comment fourthComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "fourth comment"));

        session.save();

        List<Comment> firstPage = commentManager.getComments(session, doc.getId(), 2L, 0L, false);
        assertNotNull(firstPage);
        assertEquals(2, firstPage.size());
        assertEquals(firstPage.get(0), fourthComment);
        assertEquals(firstPage.get(1), thirdComment);

        List<Comment> secondPage = commentManager.getComments(session, doc.getId(), 2L, 1L, false);
        assertNotNull(secondPage);
        assertEquals(2, secondPage.size());
        assertEquals(secondPage.get(0), secondComment);
        assertEquals(secondPage.get(1), firstComment);
    }

    @Test
    public void shouldReturnRepliesByCreationDateDescendingWhenCommentHasReplies() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment mainComment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "main comment"));
        Comment firstReply = commentManager.createComment(session,
                getSampleComment(mainComment.getId(), session.getPrincipal().getName(), "first reply"));
        Comment secondReply = commentManager.createComment(session,
                getSampleComment(mainComment.getId(), session.getPrincipal().getName(), "second reply"));

        session.save();

        List<Comment> replies = commentManager.getComments(session, mainComment.getId(), false);
        assertNotNull(replies);
        assertEquals(2, replies.size());
        assertEquals(replies.get(0), secondReply);
        assertEquals(replies.get(1), firstReply);
    }

    @Test
    public void shouldReturnMainCommentWhenSeveralNestedRepliesExist() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment comment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "main comment"));
        Comment firstLevelReply = commentManager.createComment(session,
                getSampleComment(comment.getId(), session.getPrincipal().getName(), "first level reply"));
        Comment secondLevelReply = commentManager.createComment(session,
                getSampleComment(firstLevelReply.getId(), session.getPrincipal().getName(), "second level reply"));
        Comment thirdLevelReply = commentManager.createComment(session,
                getSampleComment(secondLevelReply.getId(), session.getPrincipal().getName(), "third level reply"));
        Comment fourthLevelReply = commentManager.createComment(session,
                getSampleComment(thirdLevelReply.getId(), session.getPrincipal().getName(), "fourth level reply"));

        session.save();

        DocumentModel replyModel = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "Comment", COMMENT_DOC_TYPE);
        commentToDocumentModel(fourthLevelReply, replyModel);
        DocumentModel threadDocumentModel = commentManager.getThreadForComment(replyModel);
        assertNotNull(threadDocumentModel);
        assertEquals(newComment(threadDocumentModel), comment);
    }

    @Test
    public void shouldReturnSameCommentWhenNoRepliesExist() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment comment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "main comment"));

        session.save();

        DocumentModel threadDocumentModel = commentManager.getThreadForComment(
                session.getDocument(new IdRef(comment.getId())));

        assertNotNull(threadDocumentModel);
        assertEquals(comment, newComment(threadDocumentModel));
    }

    @Test
    public void shouldCreateLocatedComment() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);

        Comment comment = getSampleComment(null, session.getPrincipal().getName(), "some text");
        DocumentModel commentModel = session.createDocumentModel(null, "Comment", COMMENT_DOC_TYPE);
        commentToDocumentModel(comment, commentModel);

        session.save();

        commentManager.createLocatedComment(doc, commentModel, FOLDER_COMMENT_CONTAINER);

        DocumentModelList children = session.getChildren(new PathRef(FOLDER_COMMENT_CONTAINER), COMMENT_DOC_TYPE);
        assertNotNull(children);
        assertEquals(1, children.totalSize());
        assertEquals(comment.getAuthor(), children.get(0).getPropertyValue("comment:author"));
        assertEquals(comment.getCreationDate(),
                ((Calendar) children.get(0).getPropertyValue("comment:creationDate")).toInstant());
        assertEquals(comment.getText(), children.get(0).getPropertyValue("comment:text"));
    }

    protected static Comment getSampleComment(String parentId, String author, String text) {
        Comment comment = new CommentImpl();
        comment.setParentId(parentId);
        comment.setAuthor(author);
        comment.setText(text);
        comment.setCreationDate(Instant.now());
        return comment;
    }
}
