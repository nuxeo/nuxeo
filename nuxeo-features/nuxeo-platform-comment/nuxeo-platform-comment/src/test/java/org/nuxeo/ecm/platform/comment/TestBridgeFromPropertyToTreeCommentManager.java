/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;

/**
 * @since 11.1
 */
public class TestBridgeFromPropertyToTreeCommentManager extends TestBridgeCommentManager {

    @Override
    protected BridgeCommentManager getBridgeCommentManager() {
        return new BridgeCommentManager(new PropertyCommentManager(), new TreeCommentManager());
    }

    @Test
    public void testDeleteCommentAsProperty() {
        // Use the comment as property
        CommentManager anotherCommentManager = new PropertyCommentManager();
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        Comment comment = anotherCommentManager.getComment(session, commentDocModel.getId());
        assertNotNull(comment);
        assertNotNull(comment.getParentId());
        assertNotEquals(0, comment.getParentId().length());

        // Delete this property comment using the Bridge
        commentManager.deleteComment(session, commentDocModel.getId());
        try {
            anotherCommentManager.getComment(session, commentDocModel.getId());
            fail();
        } catch (CommentNotFoundException cfe) {
            assertNotNull(cfe);
            assertNotNull(cfe.getMessage());
        }
    }

    @Test
    public void testDeleteCommentAsTree() {
        // Use the comment as tree
        CommentManager anotherCommentManager = new TreeCommentManager();
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        Comment comment = anotherCommentManager.getComment(session, commentDocModel.getId());
        assertNotNull(comment);
        assertNotNull(comment.getParentId());
        assertNotEquals(0, comment.getParentId().length());

        // Delete this tree comment using the Bridge
        commentManager.deleteComment(session, commentDocModel.getId());
        try {
            anotherCommentManager.getComment(session, commentDocModel.getId());
            fail();
        } catch (CommentNotFoundException cfe) {
            assertNotNull(cfe);
            assertNotNull(cfe.getMessage());
        }
    }

    @Test
    public void shouldGetThreadCommentAsProperty() {
        CommentManager anotherCommentManager = new PropertyCommentManager();
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        DocumentModel threadForComment = commentManager.getThreadForComment(commentDocModel);
        assertNotNull(threadForComment);
        assertTrue(threadForComment.hasSchema(CommentsConstants.COMMENT_SCHEMA));
        assertEquals(commentDocModel.getRef(), threadForComment.getRef());
        assertEquals("HiddenFolder", session.getDocument(commentDocModel.getParentRef()).getType());
    }

    @Test
    public void shouldGetThreadCommentAsTree() {
        CommentManager anotherCommentManager = new TreeCommentManager();
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        DocumentModel threadForComment = commentManager.getThreadForComment(commentDocModel);
        assertNotNull(threadForComment);
        assertTrue(threadForComment.hasSchema(CommentsConstants.COMMENT_SCHEMA));
        assertEquals(commentDocModel.getRef(), threadForComment.getRef());
        assertEquals(CommentsConstants.COMMENTS_DIRECTORY_TYPE,
                session.getDocument(commentDocModel.getParentRef()).getType());
    }

    @Test
    public void shouldGetTopLevelCommentAncestorAsProperty() {
        CommentManager anotherCommentManager = new PropertyCommentManager();
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        DocumentRef commentAncestor = commentManager.getTopLevelCommentAncestor(session, commentDocModel.getRef());
        assertNotNull(commentAncestor);
        assertEquals(getCommentedDocument().getRef(), commentAncestor);
    }

    @Test
    public void shouldGetTopLevelCommentAncestorAsTree() {
        CommentManager anotherCommentManager = new TreeCommentManager();
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        DocumentRef commentAncestor = commentManager.getTopLevelCommentAncestor(session, commentDocModel.getRef());
        assertNotNull(commentAncestor);
        assertEquals(getCommentedDocument().getRef(), commentAncestor);
    }

}
