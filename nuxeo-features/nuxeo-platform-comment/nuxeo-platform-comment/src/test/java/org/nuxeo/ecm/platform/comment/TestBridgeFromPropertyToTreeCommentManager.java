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
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
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
public class TestBridgeFromPropertyToTreeCommentManager extends AbstractTestBridgeCommentManager {

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
        DocumentRef topLevelCommentAncestor = commentManager.getTopLevelCommentAncestor(session, commentDocModel.getRef());
        assertNotNull(topLevelCommentAncestor);
        assertEquals(FILE_DOC_TYPE, session.getDocument(topLevelCommentAncestor).getType());
        assertEquals(new IdRef((String) commentDocModel.getPropertyValue(COMMENT_PARENT_ID)), topLevelCommentAncestor);
        assertEquals("HiddenFolder", session.getDocument(commentDocModel.getParentRef()).getType());
    }

    @Test
    public void shouldGetThreadCommentAsTree() {
        CommentManager anotherCommentManager = new TreeCommentManager();
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        DocumentRef topLevelCommentAncestor = commentManager.getTopLevelCommentAncestor(session, commentDocModel.getRef());
        assertNotNull(topLevelCommentAncestor);
        assertEquals(FILE_DOC_TYPE, session.getDocument(topLevelCommentAncestor).getType());

        // In this case we have a first level comment, his parent is the `Comments` folder
        DocumentModel commentsFolder = session.getDocument(commentDocModel.getParentRef());
        assertEquals(CommentsConstants.COMMENTS_DIRECTORY_TYPE,
                commentsFolder.getType());
        assertEquals(commentsFolder.getParentRef(), topLevelCommentAncestor);
    }

    @Test
    public void shouldGetTopLevelCommentAncestorAsProperty() {
        CommentManager anotherCommentManager = new PropertyCommentManager();
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        DocumentRef topLevelCommentAncestor = commentManager.getTopLevelCommentAncestor(session, commentDocModel.getRef());
        assertNotNull(topLevelCommentAncestor);
        assertEquals(getCommentedDocument().getRef(), topLevelCommentAncestor);
    }

    @Test
    public void shouldGetTopLevelCommentAncestorAsTree() {
        CommentManager anotherCommentManager = new TreeCommentManager();
        DocumentModel commentDocModel = createComment(anotherCommentManager);
        DocumentRef topLevelCommentAncestor = commentManager.getTopLevelCommentAncestor(session, commentDocModel.getRef());
        assertNotNull(topLevelCommentAncestor);
        assertEquals(getCommentedDocument().getRef(), topLevelCommentAncestor);
    }

}
