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

import static java.time.temporal.ChronoUnit.MILLIS;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENTS_DIRECTORY_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_SCHEMA;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.junit.Test;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.document.CopyDocument;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.comment.AbstractTestCommentManager;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 11.1
 */
@Deploy("org.nuxeo.ecm.automation.core")
public class TestTreeCommentManager extends AbstractTestCommentManager {

    public static final String COPY_DOC_NAME = "CopyDoc";

    public static final String ROOT = "/";

    public static final String TARGET_PROPERTY_KEY = "target";

    public static final String NAME_PROPERTY_KEY = "name";

    public static final String SPECIAL_CHILD_DOC_NAME = "Comments";

    public static final String REGULAR_CHILD_DOC_NAME = "regularChildDoc";

    public static final String COMMENT_ROOT_TYPE = "CommentRoot";

    public static final String FILE = "File";

    public static final String COMMENT_TEXT = "some text for this comment";

    @Inject
    protected AutomationService automationService;

    @Test
    public void shouldThrowExceptionWhenGettingUnExistingComment() {
        try {
            commentManager.getComment(session, "nonExistingCommentId");
            fail();
        } catch (CommentNotFoundException e) {
            checkStatusAndMessage(e, "The comment nonExistingCommentId does not exist.");
        }
    }

    @Test
    public void shouldThrowExceptionWhenGettingUnExistingExternalComment() {
        try {
            commentManager.getExternalComment(session, "nonExistingExternalCommentId");
            fail();
        } catch (CommentNotFoundException e) {
            checkStatusAndMessage(e, "The external comment nonExistingExternalCommentId does not exist.");
        }
    }

    @Test
    public void shouldThrowExceptionWhenCreatingCommentForUnExistingParent() {
        try {
            commentManager.createComment(session, createSampleComment("nonExistingId"));
            fail();
        } catch (CommentNotFoundException e) {
            checkStatusAndMessage(e, "The comment nonExistingId does not exist.");
        }
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingUnExistingComment() {
        try {
            commentManager.updateComment(session, "nonExistingCommentId", new CommentImpl());
            fail();
        } catch (CommentNotFoundException e) {
            checkStatusAndMessage(e, "The comment nonExistingCommentId does not exist.");
        }
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingUnExistingExternalComment() {
        try {
            commentManager.updateExternalComment(session, "nonExistingExternalCommentId", new CommentImpl());
            fail();
        } catch (CommentNotFoundException e) {
            checkStatusAndMessage(e, "The external comment nonExistingExternalCommentId does not exist.");
        }
    }

    @Test
    public void shouldThrowExceptionWhenDeletingUnExistingComment() {
        try {
            commentManager.deleteComment(session, "nonExistingCommentId");
            fail();
        } catch (CommentNotFoundException e) {
            checkStatusAndMessage(e, "The comment nonExistingCommentId does not exist.");
        }
    }

    @Test
    public void shouldThrowExceptionWhenDeletingUnExistingExternalComment() {
        try {
            commentManager.deleteExternalComment(session, "nonExistingExternalCommentId");
            fail();
        } catch (CommentNotFoundException e) {
            checkStatusAndMessage(e, "The external comment nonExistingExternalCommentId does not exist.");
        }
    }

    @Test
    public void shouldCreateComment() {
        DocumentModel doc = createDocumentModel("anyFile");

        Comment commentToCreate;
        Comment createdComment;
        DocumentModel commentsFolder;
        try (CapturingEventListener listener = new CapturingEventListener(DOCUMENT_CREATED)) {
            commentToCreate = createSampleComment(doc.getId());
            createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

            // We get two event (one for the created Comments folder and one for the created comment)
            // we ensure that the event related to the comments folder is disabled.
            assertEquals(2, listener.getCapturedEvents().size());

            List<Event> events = listener.getCapturedEvents()
                                         .stream()
                                         .filter(e -> BooleanUtils.isTrue(
                                                 (Boolean) e.getContext().getProperty(DISABLE_NOTIFICATION_SERVICE)))
                                         .collect(Collectors.toList());

            assertEquals(1, events.size());

            // Get the unique comments folder
            commentsFolder = getCommentsFolder(doc.getId());

            assertEquals(commentsFolder.getRef(),
                    ((DocumentEventContext) events.get(0).getContext()).getSourceDocument().getRef());
        }

        // Get the comment document model
        DocumentModel commentDocModel = session.getDocument(new IdRef(createdComment.getId()));

        // Check the structure of this comment
        DocumentRef[] parentDocumentRefs = session.getParentDocumentRefs(commentDocModel.getRef());

        // Should be the `Comments` folder
        assertEquals(COMMENTS_DIRECTORY_TYPE, session.getDocument(parentDocumentRefs[0]).getType());
        assertEquals(commentsFolder.getRef(), parentDocumentRefs[0]);

        // Should be the file to comment `anyFile`
        assertEquals(doc.getRef(), parentDocumentRefs[1]);

        // Check the Thread
        DocumentRef topLevelCommentAncestor = commentManager.getTopLevelCommentAncestor(session,
                commentDocModel.getRef());
        assertEquals(doc.getRef(), topLevelCommentAncestor);

        // I can create a comment if i have the right permissions on the document
        ACPImpl acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("james", SecurityConstants.READ, true));
        session.setACP(doc.getRef(), acp, false);
        try (CloseableCoreSession jamesSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "james")) {
            createAndCheckComment(jamesSession, doc, commentToCreate, 1);
        }
    }

    @Test
    public void shouldCreateCommentAndReplies() {
        DocumentModel doc = createDocumentModel("anotherFile");

        // Create a comment
        Comment commentToCreate = createSampleComment(doc.getId());
        Comment createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        // Create a reply
        Comment firstReply = createSampleComment(createdComment.getId());
        Comment firstCreatedReply = createAndCheckComment(session,
                session.getDocument(new IdRef(createdComment.getId())), firstReply, 2);

        // Create another reply
        Comment secondReply = createSampleComment(firstCreatedReply.getId());
        Comment secondCreatedReply = createAndCheckComment(session,
                session.getDocument(new IdRef(firstCreatedReply.getId())), secondReply, 3);

        // Get the unique comments folder
        DocumentModel commentsFolder = getCommentsFolder(doc.getId());

        // Get the comment document model of the second reply
        DocumentModel secondReplyDocModel = session.getDocument(new IdRef(secondCreatedReply.getId()));

        // Check the structure of last comment (second reply)
        DocumentRef[] parentDocumentRefs = session.getParentDocumentRefs(secondReplyDocModel.getRef());

        // Should be the first reply
        assertEquals(COMMENT_DOC_TYPE, session.getDocument(parentDocumentRefs[0]).getType());
        assertEquals(new IdRef(firstCreatedReply.getId()), parentDocumentRefs[0]);

        // Should be the comment
        assertEquals(COMMENT_DOC_TYPE, session.getDocument(parentDocumentRefs[1]).getType());
        assertEquals(new IdRef(createdComment.getId()), parentDocumentRefs[1]);

        // Should be the `Comments` folder
        assertEquals(COMMENTS_DIRECTORY_TYPE, session.getDocument(parentDocumentRefs[2]).getType());
        assertEquals(commentsFolder.getRef(), parentDocumentRefs[2]);

        // Should be the file to comment `anotherFile`
        assertEquals(doc.getRef(), parentDocumentRefs[3]);

        // Check the Thread
        DocumentRef topLevelCommentAncestor = commentManager.getTopLevelCommentAncestor(session,
                secondReplyDocModel.getRef());
        assertEquals(doc.getRef(), topLevelCommentAncestor);
    }

    @Test
    public void iCannotCreateComment() {
        DocumentModel doc = createDocumentModel("myOwnFile");

        try (CloseableCoreSession bobSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "bob")) {
            Comment commentToCreate = createSampleComment(doc.getId());
            createAndCheckComment(bobSession, doc, commentToCreate, 1);
            fail("bob should not be able to create comment");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user bob does not have access to the comments of document %s", doc.getId()),
                    cse.getMessage());
        }
    }

    @Test
    public void shouldGetComments() {
        DocumentModel doc = createDocumentModel("anyFile");

        Comment commentToCreate = createSampleComment(doc.getId());
        Comment createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        // Check retrieving comment by the doc creator
        Comment retrievedComment = commentManager.getComment(session, createdComment.getId());
        assertNotNull(retrievedComment);
        assertNotNull(createdComment.getId(), retrievedComment.getId());

        List<Comment> retrievedComments = commentManager.getComments(session, doc.getId());
        assertEquals(1, retrievedComments.size());
        assertNotNull(createdComment.getId(), retrievedComments.get(0).getId());

        // Add a new comment and some replies
        Comment anotherCreatedComment = createAndCheckComment(session, doc, createSampleComment(doc.getId()), 1);

        Comment reply = createSampleComment(createdComment.getId());
        createAndCheckComment(session, session.getDocument(new IdRef(createdComment.getId())), reply, 2);

        // Now if we get comments, we should only get those of the first level (that means without replies)
        retrievedComments = commentManager.getComments(session, doc.getId());
        assertEquals(2, retrievedComments.size());
        List<String> commentIds = retrievedComments.stream().map(Comment::getId).collect(Collectors.toList());
        assertTrue(commentIds.contains(anotherCreatedComment.getId()));
        assertTrue(commentIds.contains(createdComment.getId()));

        // Check if there is any comments under the reply
        assertTrue(commentManager.getComments(session, anotherCreatedComment.getId()).isEmpty());

        // I can create a comment if i have the right permissions on the document
        ACPImpl acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("james", SecurityConstants.READ, true));
        session.setACP(doc.getRef(), acp, false);
        transactionalFeature.nextTransaction();

        try (CloseableCoreSession jamesSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "james")) {
            assertNotNull(commentManager.getComment(jamesSession, createdComment.getId()));

            retrievedComments = commentManager.getComments(jamesSession, doc.getId());
            assertEquals(2, retrievedComments.size());
            assertNotNull(createdComment.getId(), retrievedComments.get(0).getId());
        }
    }

    @Test
    public void iCannotGetComments() {
        DocumentModel doc = createDocumentModel("anyFile");

        Comment commentToCreate = createSampleComment(doc.getId());
        Comment createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        try (CloseableCoreSession bobSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "bob")) {
            commentManager.getComment(bobSession, createdComment.getId());
            fail("bob should not be able to get comment");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user bob does not have access to the comments of document %s", doc.getId()),
                    cse.getMessage());
        }

        try (CloseableCoreSession bobSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "bob")) {
            commentManager.getComments(bobSession, createdComment.getId());
            fail("bob should not be able to get comments");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user bob does not have access to the comments of document %s", doc.getId()),
                    cse.getMessage());
        }

    }

    @Test
    public void shouldUpdateComment() {
        DocumentModel doc = createDocumentModel("anyFile");

        Comment commentToCreate = createSampleComment(doc.getId());
        Comment createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        Comment newComment = createSampleComment(doc.getId());
        newComment.setText("This a new text on this comment");
        newComment.setAuthor("james");

        Comment updatedComment = commentManager.updateComment(session, createdComment.getId(), newComment);
        verifyCommonsInfo(doc, newComment, updatedComment, 1);
        Instant lastModification = updatedComment.getModificationDate();
        assertNotNull(lastModification);

        // If i am the author of the comment then i can update it
        try (CloseableCoreSession jamesSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "james")) {
            newComment.setText("Can you call me on my phone, please");
            updatedComment = commentManager.updateComment(jamesSession, createdComment.getId(), newComment);
            verifyCommonsInfo(doc, newComment, updatedComment, 1);
            assertNotNull(updatedComment.getModificationDate());
            assertEquals(lastModification, updatedComment.getModificationDate());
        }
    }

    @Test
    public void iCannotUpdateComment() {
        DocumentModel doc = createDocumentModel("anyFile");

        Comment commentToCreate = createSampleComment(doc.getId());
        Comment createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        Comment newComment = createSampleComment(doc.getId());
        newComment.setText("I try to update this comment !");

        try (CloseableCoreSession bobSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "bob")) {
            commentManager.updateComment(bobSession, createdComment.getId(), newComment);
            fail("bob should not be able to update a comment");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user bob cannot edit comments of document %s", doc.getId()),
                    cse.getMessage());
        }
    }

    @Test
    public void shouldDeleteComment() {
        DocumentModel doc = createDocumentModel("anyFile");

        Comment commentToCreate = createSampleComment(doc.getId());
        Comment createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        commentManager.deleteComment(session, createdComment.getId());
        try {
            commentManager.getComment(session, createdComment.getId());
            fail(String.format("The comment %s should not exist.", createdComment.getId()));
        } catch (CommentNotFoundException cnfe) {
            assertNotNull(cnfe);
            assertEquals(String.format("The comment %s does not exist.", createdComment.getId()), cnfe.getMessage());
        }

        // Create another comment with another author
        commentToCreate = createSampleComment(doc.getId());
        commentToCreate.setAuthor("james");
        createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        // If i am the author of the comment then i can delete it
        try (CloseableCoreSession jamesSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "james")) {
            commentManager.deleteComment(jamesSession, createdComment.getId());
            assertFalse(jamesSession.exists(new IdRef(createdComment.getId())));
            assertFalse(session.exists(new IdRef(createdComment.getId())));
        }

        // Create another comment with another author
        commentToCreate = createSampleComment(doc.getId());
        createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        // I have an `Everything` permissions on the doc, then i can remove it
        ACPImpl acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("julia", SecurityConstants.EVERYTHING, true));
        session.setACP(doc.getRef(), acp, false);
        transactionalFeature.nextTransaction();
        try (CloseableCoreSession juliaSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "julia")) {
            commentManager.deleteComment(juliaSession, createdComment.getId());
        }

        try (CloseableCoreSession juliaSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "julia")) {
            commentManager.getComment(juliaSession, createdComment.getId());
            fail(String.format("The comment %s should not exist.", createdComment.getId()));
        } catch (CommentNotFoundException cnfe) {
            assertNotNull(cnfe);
            assertEquals(String.format("The comment %s does not exist.", createdComment.getId()), cnfe.getMessage());
        }
    }

    @Test
    public void iCannotDeleteComment() {
        DocumentModel doc = createDocumentModel("anyFile");

        Comment commentToCreate = createSampleComment(doc.getId());
        Comment createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        try (CloseableCoreSession bobSession = CoreInstance.openCoreSession(doc.getRepositoryName(), "bob")) {
            commentManager.deleteComment(bobSession, createdComment.getId());
            fail("bob should not be able to delete a comment");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user bob cannot delete comments of the document %s", doc.getId()),
                    cse.getMessage());
        }
    }

    @Test
    public void testCommentsExcludedFromCopy() throws OperationException {
        DocumentModel doc = createDocumentModel("anyFile");
        DocumentModel doc2 = createDocumentModel("anyFile");

        DocumentModel regularChildDoc = session.createDocumentModel(doc.getPathAsString(), REGULAR_CHILD_DOC_NAME,
                FILE);
        session.createDocument(regularChildDoc);

        Comment commentToCreate = createSampleComment(doc.getId());
        createAndCheckComment(session, doc, commentToCreate, 1);

        Comment commentToCreate2 = createSampleComment(doc2.getId());
        createAndCheckComment(session, doc2, commentToCreate2, 1);

        DocumentModelList children = session.getChildren(doc.getRef());
        assertEquals(2, children.size());

        try (OperationContext context = new OperationContext(session)) {
            context.setInput(doc);
            Map<String, Serializable> params = new HashMap<>();
            params.put(TARGET_PROPERTY_KEY, ROOT);
            params.put(NAME_PROPERTY_KEY, COPY_DOC_NAME);
            DocumentModel result = (DocumentModel) automationService.run(context, CopyDocument.ID, params);
            result = session.getDocument(result.getRef());
            assertNotEquals(doc.getId(), result.getId());
            assertEquals(COPY_DOC_NAME, result.getName());
            children = session.getChildren(result.getRef());
            // special children shall not be copied
            assertEquals(1, children.size());
            DocumentModel copiedRegularChild = children.get(0);
            assertEquals(REGULAR_CHILD_DOC_NAME, copiedRegularChild.getName());
            assertNotEquals(regularChildDoc.getRef(), copiedRegularChild.getRef());
        }
    }

    @Test
    public void testCommentsWithCheckInAndRestore() {
        DocumentModel doc = createDocumentModel("anyFile");
        DocumentModel doc2 = createDocumentModel("anyFile");

        DocumentModel regularChildDoc = session.createDocumentModel(doc.getPathAsString(), REGULAR_CHILD_DOC_NAME,
                FILE);
        session.createDocument(regularChildDoc);

        Comment commentToCreate = createSampleComment(doc.getId());
        createAndCheckComment(session, doc, commentToCreate, 1);

        Comment commentToCreate2 = createSampleComment(doc2.getId());
        createAndCheckComment(session, doc2, commentToCreate2, 1);

        DocumentModelList children = session.getChildren(doc.getRef());
        assertEquals(2, children.size());

        // test checkin copy, only special children shall be copied
        DocumentRef checkedIn = doc.checkIn(VersioningOption.MAJOR, "JustForFun");
        children = session.getChildren(checkedIn);
        assertEquals(1, children.totalSize());
        DocumentModel versionedChild = children.get(0);
        assertEquals(COMMENT_ROOT_TYPE, versionedChild.getType());
        assertEquals(SPECIAL_CHILD_DOC_NAME, versionedChild.getName());
        assertNotEquals(regularChildDoc.getRef(), versionedChild.getRef());
        children = session.getChildren(versionedChild.getRef());
        // Check the snapshot comment
        assertEquals(1, children.totalSize());
        Comment retrievedComment = commentManager.getComment(session, children.get(0).getId());
        assertEquals(COMMENT_TEXT, retrievedComment.getText());

        // test restore copy. Live document shall keep both special and regular children.
        // No version children shall be added during restore
        DocumentModel restored = session.restoreToVersion(doc.getRef(), checkedIn);
        children = session.getChildren(restored.getRef());
        assertEquals(2, children.totalSize());
    }

    /**
     * Creates a new comment and check his data {@link #verifyCommonsInfo(DocumentModel, Comment, Comment, int)}
     *
     * @return the created comment
     */
    protected Comment createAndCheckComment(CoreSession coreSession, DocumentModel doc, Comment commentToCreate,
            int numberOfAncestor) {
        Comment createdComment = commentManager.createComment(coreSession, commentToCreate);
        transactionalFeature.nextTransaction();

        verifyCommonsInfo(doc, commentToCreate, createdComment, numberOfAncestor);
        assertNull(createdComment.getModificationDate());

        return createdComment;
    }

    /**
     * Verify the commons infos when we create or update a comment
     */
    protected void verifyCommonsInfo(DocumentModel doc, Comment commentToCreate, Comment createdComment,
            int numberOfAncestor) {
        // For backward compatibility we should keep the whole comment properties (`Comment` schema) filled as it's done
        // in 10.10 to avoid breaking any customer code.
        assertNotNull(createdComment);
        assertNotNull(createdComment.getId());
        assertEquals(commentToCreate.getCreationDate(), createdComment.getCreationDate());
        assertNotNull(createdComment.getAncestorIds());
        assertEquals(numberOfAncestor, createdComment.getAncestorIds().size());
        assertTrue(createdComment.getAncestorIds().contains(doc.getId()));
        assertEquals(commentToCreate.getAuthor(), createdComment.getAuthor());
        assertEquals(doc.getId(), createdComment.getParentId());
        assertEquals(commentToCreate.getText(), createdComment.getText());
    }

    /**
     * @return the sample comment
     */
    protected Comment createSampleComment(String parentId) {
        Comment comment = new CommentImpl();
        comment.setParentId(parentId);
        comment.setAuthor(session.getPrincipal().getName());
        comment.setText(COMMENT_TEXT);
        comment.setCreationDate(Instant.now().truncatedTo(MILLIS));

        return comment;
    }

    /**
     * @return the file doc model for the given name
     */
    protected DocumentModel createDocumentModel(String name) {
        DocumentModel doc = session.createDocumentModel(FOLDER_COMMENT_CONTAINER, name, "File");
        doc = session.createDocument(doc);
        transactionalFeature.nextTransaction();
        return doc;
    }

    protected void checkStatusAndMessage(NuxeoException e, String message) {
        assertEquals(NOT_FOUND.getStatusCode(), e.getStatusCode());
        assertEquals(message, e.getMessage());
    }

    protected DocumentModel getCommentsFolder(String documentId) {
        String query = String.format("SELECT * FROM Document WHERE %s = '%s' and %s = '%s'", NXQL.ECM_PARENTID,
                documentId, NXQL.ECM_PRIMARYTYPE, COMMENTS_DIRECTORY_TYPE);

        DocumentModelList documents = session.query(query);
        assertEquals(1, documents.size());
        return documents.get(0);
    }

    @Override
    public Class<? extends CommentManager> getType() {
        return TreeCommentManager.class;
    }

    protected DocumentRef getCommentedDocRef(CoreSession session, DocumentModel commentDocModel) {
        assertTrue(commentDocModel.hasSchema(COMMENT_SCHEMA));
        DocumentModel commentedDocModel = session.getDocument(commentDocModel.getParentRef());
        if (COMMENTS_DIRECTORY_TYPE.equals(commentedDocModel.getType())) {
            commentedDocModel = session.getDocument(commentedDocModel.getParentRef());
        }
        return commentedDocModel.getRef();
    }

    protected DocumentRef getCommentedDocRef(CoreSession session, DocumentModel commentDocModel, boolean reply) {
        if (reply) {
            return commentDocModel.getParentRef();
        }
        return commentManager.getTopLevelCommentAncestor(session, commentDocModel.getRef());
    }
}
