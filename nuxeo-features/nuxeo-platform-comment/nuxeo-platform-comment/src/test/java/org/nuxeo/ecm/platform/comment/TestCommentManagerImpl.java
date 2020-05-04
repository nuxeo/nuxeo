/*
 * (C) Copyright 2018-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.platform.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader.applyDirtyPropertyValues;
import static org.nuxeo.ecm.platform.comment.CommentUtils.emptyComment;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;

import java.util.Calendar;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.impl.CommentManagerImpl;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 10.3
 * @deprecated since 10.3, in order to follow the service deprecation
 *             {@link org.nuxeo.ecm.platform.comment.impl.CommentManagerImpl}.
 */
@Deprecated
@Features(RelationCommentFeature.class)
public class TestCommentManagerImpl extends AbstractTestCommentManager {

    public TestCommentManagerImpl() {
        super(CommentManagerImpl.class);
    }

    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this case - permissions check different")
    public void testCreateReply() {
    }

    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this case - permissions check different")
    public void testGetCommentPermissions() {
    }

    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this case - permissions check different")
    public void testGetReply() {
        // mainly due to testCreateReply not supported
    }

    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this case - ordering not supported")
    public void testGetCommentsOrdering() {
    }

    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this case - ordering not supported")
    public void testGetCommentsPaginationOrdering() {
    }

    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this test/case - permissions check different")
    public void testGetCommentsWithReply() {
        // mainly due to testCreateReply not supported
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testUpdateComment() {
        super.testUpdateComment(); // if implemented one day
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testUpdateCommentByItsAuthor() {
        super.testUpdateCommentByItsAuthor(); // if implemented one day
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testUpdateCommentByPowerfulUser() {
        super.testUpdateCommentByPowerfulUser(); // if implemented one day
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testUpdateCommentWithModificationDate() {
        super.testUpdateCommentWithModificationDate(); // if implemented one day
    }

    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this test/case - permissions check different")
    public void testUpdateReply() {
        // mainly due to testCreateReply not supported
    }

    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this case - permissions check different")
    public void testDeleteCommentByItsAuthor() {
    }

    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this case - permissions check different")
    public void testDeleteReply() {
        // mainly due to testCreateReply not supported
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testGetExternalComment() {
        super.testGetExternalComment(); // if implemented one day
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testGetExternalCommentPermissions() {
        super.testGetExternalCommentPermissions(); // if implemented one day
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testUpdateExternalComment() {
        super.testUpdateExternalComment(); // if implemented one day
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testUpdateExternalCommentByItsAuthor() {
        super.testUpdateExternalCommentByItsAuthor(); // if implemented one day
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testUpdateExternalCommentByPowerfulUser() {
        super.testUpdateExternalCommentByPowerfulUser(); // if implemented one day
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testDeleteExternalComment() {
        super.testDeleteExternalComment(); // if implemented one day
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testDeleteExternalCommentByItsAuthor() {
        super.testDeleteExternalCommentByItsAuthor(); // if implemented one day
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testDeleteExternalCommentByPowerfulUser() {
        super.testDeleteExternalCommentByPowerfulUser(); // if implemented one day
    }

    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this case - deprecated implementation")
    public void testCommentsAncestorIds() {
    }

    /*
     * NXP-28719
     */
    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this test/case - deprecated implementation")
    public void testCreateCommentUnderPlacelessDocument() {
    }

    /*
     * NXP-28719
     */
    @Test
    @Override
    @Ignore("CommentManagerImpl doesn't support this test/case - deprecated implementation")
    public void testCreateRepliesUnderPlacelessDocument() {
    }

    // -------------
    // Legacy tests
    // -------------

    @Test
    public void testCreateReadDelete() {
        try (CloseableCoreSession jamesSession = coreFeature.openCoreSession(JAMES)) {
            // Get the document as the user
            DocumentModel userDoc = jamesSession.getDocument(commentedDocModel.getRef());

            // Comment the document as the user
            commentManager.createComment(jamesSession, newComment(userDoc.getId(), "I am a comment!"));

            // Check the comment document can be retrieved by a system session query
            List<DocumentModel> dml = session.query("SELECT * FROM Comment");
            assertEquals(1, dml.size());

            // Check the comment document cannot be retrieved by the user session query
            dml = jamesSession.query("SELECT * FROM Comment");
            assertEquals(0, dml.size());

            // Check the comment can be retrieved by the user via the comment service
            List<Comment> comments = commentManager.getComments(jamesSession, userDoc.getId());
            assertEquals(1, comments.size());
            assertEquals("I am a comment!", comments.get(0).getText());

            // Check the comment was deleted by the user
            commentManager.deleteComment(jamesSession, comments.get(0).getId());
            comments = commentManager.getComments(jamesSession, userDoc.getId());
            assertEquals(0, comments.size());
        }
    }

    @Test
    public void testCreateLocalComment() {
        DocumentModel container = session.createDocumentModel("/domain", "CommentContainer", "Folder");
        session.createDocument(container);
        session.save();

        Comment comment = emptyComment();
        comment.setAuthor("linda");
        comment.setText("I am a comment !");

        // Create a comment in a specific location
        DocumentModel commentModel = session.createDocumentModel(null, "Comment", COMMENT_DOC_TYPE);
        commentModel = session.createDocument(commentModel);
        commentModel.setPropertyValue("dc:created", Calendar.getInstance());
        applyDirtyPropertyValues(comment.getDocument(), commentModel);
        commentModel = commentManager.createLocatedComment(commentedDocModel, commentModel, "/domain/CommentContainer");

        // Check if Comments folder has been created in the given container
        assertThat(session.getChildren(new PathRef("/domain/CommentContainer")).totalSize()).isEqualTo(1);

        assertThat(commentModel.getPathAsString()).contains("/domain/CommentContainer");
    }
}
