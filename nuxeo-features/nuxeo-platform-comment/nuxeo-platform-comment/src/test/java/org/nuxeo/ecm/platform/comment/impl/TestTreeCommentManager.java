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

package org.nuxeo.ecm.platform.comment.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.schema.FacetNames.FOLDERISH;
import static org.nuxeo.ecm.core.schema.FacetNames.HIDDEN_IN_NAVIGATION;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newExternalComment;
import static org.nuxeo.ecm.platform.comment.impl.AbstractCommentManager.COMMENTS_DIRECTORY;
import static org.nuxeo.ecm.platform.comment.impl.TreeCommentManager.COMMENT_NAME;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENTS_DIRECTORY_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.document.CopyDocument;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.platform.comment.AbstractTestCommentManager;
import org.nuxeo.ecm.platform.comment.TreeCommentFeature;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.runtime.test.runner.Features;

/**
 * This test class shouldn't override abstract because abstract should reflect this implementation.
 * 
 * @since 11.1
 */
@Features(TreeCommentFeature.class)
public class TestTreeCommentManager extends AbstractTestCommentManager {

    @Inject
    protected AutomationService automationService;

    public TestTreeCommentManager() {
        super(TreeCommentManager.class);
    }

    // ----------------
    // Structure tests
    // ----------------

    @Test
    public void _testCommentsStructure() {
        Comment c1 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(session, newComment(c1.getId(), "I am a reply!"));
        Comment c3 = commentManager.createComment(session, newComment(c2.getId(), "Me too!"));

        // in TreeCommentManager: commentedDocModel > Comments (container) > c1 > c2 > c3
        DocumentModel commentContainerDocModel = session.getChild(commentedDocModel.getRef(), COMMENTS_DIRECTORY);
        DocumentRef[] c3ParentDocRefs = session.getParentDocumentRefs(c3.getDocument().getRef());
        assertEquals(c2.getDocument().getRef(), c3ParentDocRefs[0]);
        assertEquals(c1.getDocument().getRef(), c3ParentDocRefs[1]);
        assertEquals(commentContainerDocModel.getRef(), c3ParentDocRefs[2]);
        assertEquals(commentedDocModel.getRef(), c3ParentDocRefs[3]);

        // check paths
        assertEquals("/domain/test", commentedDocModel.getPathAsString());
        assertEquals("/domain/test/Comments", commentContainerDocModel.getPathAsString());
        assertEquals("/domain/test/Comments/comment", c1.getDocument().getPathAsString());
        assertEquals("/domain/test/Comments/comment/comment", c2.getDocument().getPathAsString());
        assertEquals("/domain/test/Comments/comment/comment/comment", c3.getDocument().getPathAsString());

        // check container
        assertEquals(COMMENTS_DIRECTORY_TYPE, commentContainerDocModel.getType());
        assertTrue(commentContainerDocModel.hasFacet(FOLDERISH));
        assertTrue(commentContainerDocModel.hasFacet(HIDDEN_IN_NAVIGATION));
        assertEquals(commentedDocModel.getRef(), commentContainerDocModel.getParentRef());
    }

    /*
     * NXP-28719
     */
    @Test
    public void _testCommentsStructureOnPlaceless() {
        DocumentModel placeless = session.createDocumentModel(null, "placeless", "File");
        placeless = session.createDocument(placeless);
        transactionalFeature.nextTransaction();

        Comment c1 = commentManager.createComment(session, newComment(placeless.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(session, newComment(c1.getId(), "I am a reply!"));
        Comment c3 = commentManager.createComment(session, newComment(c2.getId(), "Me too!"));

        // in TreeCommentManager: placeless > Comments (container) > c1 > c2 > c3
        DocumentModel commentContainerDocModel = session.getChild(placeless.getRef(), COMMENTS_DIRECTORY);
        DocumentRef[] c3ParentDocRefs = session.getParentDocumentRefs(c3.getDocument().getRef());
        assertEquals(c2.getDocument().getRef(), c3ParentDocRefs[0]);
        assertEquals(c1.getDocument().getRef(), c3ParentDocRefs[1]);
        assertEquals(commentContainerDocModel.getRef(), c3ParentDocRefs[2]);
        assertEquals(placeless.getRef(), c3ParentDocRefs[3]);

        // check paths
        assertEquals("placeless", placeless.getPathAsString());
        assertEquals("placeless/Comments", commentContainerDocModel.getPathAsString());
        assertEquals("placeless/Comments/comment", c1.getDocument().getPathAsString());
        assertEquals("placeless/Comments/comment/comment", c2.getDocument().getPathAsString());
        assertEquals("placeless/Comments/comment/comment/comment", c3.getDocument().getPathAsString());
    }

    // --------------
    // Feature tests
    // --------------

    @Test
    public void _testCommentsExcludedFromCopy() throws OperationException {
        // create a regular child to check it is copied
        DocumentModel regularChildDoc = session.createDocumentModel(commentedDocModel.getPathAsString(), "regularChild",
                "File");
        session.createDocument(regularChildDoc);
        // document type Comments are considered as special children
        commentManager.createComment(session, newComment(commentedDocModel.getId()));

        assertEquals(2, session.getChildren(commentedDocModel.getRef()).size());

        try (OperationContext context = new OperationContext(session)) {
            context.setInput(commentedDocModel);
            Map<String, Serializable> params = new HashMap<>();
            params.put("target", "/");
            params.put("name", "CopyDoc");
            DocumentModel copyDocModel = (DocumentModel) automationService.run(context, CopyDocument.ID, params);
            copyDocModel = session.getDocument(copyDocModel.getRef());

            assertNotEquals(commentedDocModel.getId(), copyDocModel.getId());
            assertEquals("CopyDoc", copyDocModel.getName());
            // special children shall not be copied
            assertEquals(1, session.getChildren(copyDocModel.getRef()).size());
            DocumentModel copiedChild = session.getChild(copyDocModel.getRef(), "regularChild");
            assertNotEquals(regularChildDoc.getRef(), copiedChild.getRef());
        }
    }

    @Test
    public void _testCommentsWithCheckInAndRestore() {
        // create a regular child to check it is not copied during checkin
        DocumentModel regularChildDoc = session.createDocumentModel(commentedDocModel.getPathAsString(), "regularChild",
                "File");
        session.createDocument(regularChildDoc);
        // document type Comments are considered as special children and they should be copied during checkin
        commentManager.createComment(session, newComment(commentedDocModel.getId(), "I am a comment !"));

        assertEquals(2, session.getChildren(commentedDocModel.getRef()).size());

        DocumentModel commentsDirectory = session.getChild(commentedDocModel.getRef(), COMMENTS_DIRECTORY);

        // check only special children are copied
        DocumentRef checkedIn = commentedDocModel.checkIn(VersioningOption.MAJOR, "JustForFun");
        assertEquals(1, session.getChildren(checkedIn).size());
        DocumentModel versionedChild = session.getChild(checkedIn, COMMENTS_DIRECTORY);
        assertEquals(COMMENTS_DIRECTORY_TYPE, versionedChild.getType());
        assertNotEquals(commentsDirectory.getRef(), versionedChild.getRef());

        // Check the snapshot comment
        assertEquals(1, session.getChildren(versionedChild.getRef()).size());
        DocumentModel retrievedComment = session.getChild(versionedChild.getRef(), COMMENT_NAME);
        assertEquals("I am a comment !", retrievedComment.getPropertyValue(COMMENT_TEXT));

        // test restore copy. Live document shall keep both special and regular children.
        // No version children shall be added during restore
        DocumentModel restored = session.restoreToVersion(commentedDocModel.getRef(), checkedIn);
        assertEquals(2, session.getChildren(restored.getRef()).size());
    }

    /*
     * NXP-28700 / NXP-28964 (only for TreeCommentManager, others don't copy comments on checkIn)
     */
    @Test
    public void _testCommentsAndVersioning() {
        // create a comment + reply on original document
        Comment comment = commentManager.createComment(session,
                newExternalComment(commentedDocModel.getId(), "foo", "<entity/>", "I am a comment!"));
        commentManager.createComment(session, newComment(comment.getId(), "I am a reply!"));
        // version the document with its comments
        DocumentRef versionRef = commentedDocModel.checkIn(VersioningOption.MINOR, "checkin comment");
        String versionId = versionRef.reference().toString();

        // we now have two external entities with id foo in repository
        assertEquals(2, session.query("SELECT * FROM Comment where externalEntity:entityId='foo'").size());

        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        // test external entity retrieval with comment manager
        Comment externalComment = commentManager.getExternalComment(session, commentedDocModel.getId(), "foo");
        assertEquals(commentedDocModel.getId(), externalComment.getParentId());
        assertEquals("I am a comment!", externalComment.getText());

        // test external entity retrieval with comment manager for version
        externalComment = commentManager.getExternalComment(session, versionId, "foo");
        assertEquals(versionId, externalComment.getParentId());
        assertEquals("I am a comment!", externalComment.getText());

        // test ancestor ids
        Comment versionComment = commentManager.getExternalComment(session, versionId, "foo");
        assertEquals(new HashSet<>(Collections.singletonList(versionId)),
                new HashSet<>(versionComment.getAncestorIds()));

        List<Comment> versionReplies = commentManager.getComments(session, versionComment.getId());
        assertEquals(1, versionReplies.size());
        Comment versionReply = versionReplies.get(0);
        assertEquals(new HashSet<>(Arrays.asList(versionId, versionComment.getId())),
                new HashSet<>(versionReply.getAncestorIds()));
    }
}
