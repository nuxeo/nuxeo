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
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 10.3
 */
@Deploy("org.nuxeo.ecm.platform.query.api")
public class TestPropertyCommentManager extends AbstractTestCommentManager {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenGettingNonExistingComment() {
        commentManager.getComment(session, "nonExistingCommentId");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenGettingNonExistingExternalComment() {
        commentManager.getExternalComment(session, "nonExistingExternalCommentId");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenCreatingCommentForNonExistingParent() {
        commentManager.createComment(session,
                getSampleComment("nonExistingId", session.getPrincipal().getName(), "some text"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenUpdatingNonExistingComment() {
        commentManager.updateComment(session, "nonExistingCommentId", new CommentImpl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenUpdatingNonExistingExternalComment() {
        commentManager.updateExternalComment(session, "nonExistingExternalCommentId", new CommentImpl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenDeletingNonExistingComment() {
        commentManager.deleteComment(session, "nonExistingCommentId");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenDeletingNonExistingExternalComment() {
        commentManager.deleteExternalComment(session, "nonExistingExternalCommentId");
    }

    @Test
    public void shouldReturnCreatedObjectWhenCreatingComment() {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, "myFile", "File");
        doc = session.createDocument(doc);
        session.save();

        Comment comment = commentManager.createComment(session,
                getSampleComment(doc.getId(), session.getPrincipal().getName(), "some text"));

        assertNotNull(comment);
        assertNotNull(comment.getId());
        assertNotNull(comment.getCreationDate());
        assertNotNull(comment.getAncestorIds());
        assertEquals(1, comment.getAncestorIds().size());
        assertTrue(comment.getAncestorIds().contains(doc.getId()));
        assertEquals("Administrator", comment.getAuthor());
        assertEquals(doc.getId(), comment.getParentId());
        assertEquals("some text", comment.getText());
        assertNull(comment.getModificationDate());
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
        commentManager.updateExternalComment(session, "anEntityId", comment);

        Comment storedComment = newComment(session.getDocument(new IdRef(comment.getId())));
        assertNotNull(storedComment);
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
