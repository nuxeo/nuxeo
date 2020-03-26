/*
 * (C) Copyright 2019-2020 Nuxeo (http://nuxeo.com/) and others.
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
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT_ID;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT_RESOURCES;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PARENT_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_ROOT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.impl.TreeCommentManager.COMMENT_RELATED_TEXT_ID;
import static org.nuxeo.ecm.platform.comment.impl.TreeCommentManager.GET_COMMENT_PAGE_PROVIDER_NAME;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
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
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
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
        assertEquals(COMMENT_ROOT_DOC_TYPE, session.getDocument(parentDocumentRefs[0]).getType());
        assertEquals(commentsFolder.getRef(), parentDocumentRefs[0]);

        // Should be the file to comment `anyFile`
        assertEquals(doc.getRef(), parentDocumentRefs[1]);

        // Check the Thread
        DocumentRef topLevelCommentAncestor = commentManager.getTopLevelDocumentRef(session, commentDocModel.getRef());
        assertEquals(doc.getRef(), topLevelCommentAncestor);

        // I can create a comment if i have the right permissions on the document
        ACPImpl acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("james", SecurityConstants.READ, true));
        session.setACP(doc.getRef(), acp, false);
        CoreSession jamesSession = CoreInstance.getCoreSession(doc.getRepositoryName(), "james");
        createAndCheckComment(jamesSession, doc, commentToCreate, 1);
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
        assertEquals(COMMENT_ROOT_DOC_TYPE, session.getDocument(parentDocumentRefs[2]).getType());
        assertEquals(commentsFolder.getRef(), parentDocumentRefs[2]);

        // Should be the file to comment `anotherFile`
        assertEquals(doc.getRef(), parentDocumentRefs[3]);

        // Check the Thread
        DocumentRef topLevelCommentAncestor = commentManager.getTopLevelDocumentRef(session,
                secondReplyDocModel.getRef());
        assertEquals(doc.getRef(), topLevelCommentAncestor);
    }

    @Test
    public void iCannotCreateComment() {
        DocumentModel doc = createDocumentModel("myOwnFile");

        try {
            CoreSession bobSession = CoreInstance.getCoreSession(doc.getRepositoryName(), "bob");
            Comment commentToCreate = createSampleComment(doc.getId());
            createAndCheckComment(bobSession, doc, commentToCreate, 1);
            fail("bob should not be able to create comment");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user bob can not create comments on document %s", doc.getId()),
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

        CoreSession jamesSession = CoreInstance.getCoreSession(doc.getRepositoryName(), "james");
        assertNotNull(commentManager.getComment(jamesSession, createdComment.getId()));

        retrievedComments = commentManager.getComments(jamesSession, doc.getId());
        assertEquals(2, retrievedComments.size());
        assertNotNull(createdComment.getId(), retrievedComments.get(0).getId());
    }

    @Test
    public void iCannotGetComment() {
        DocumentModel doc = createDocumentModel("anyFile");

        Comment commentToCreate = createSampleComment(doc.getId());
        Comment createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        CoreSession bobSession = CoreInstance.getCoreSession(doc.getRepositoryName(), "bob");
        try {
            commentManager.getComment(bobSession, createdComment.getId());
            fail("bob should not be able to get comment");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user bob does not have access to the comment %s", createdComment.getId()),
                    cse.getMessage());
        }

        try {
            commentManager.getComments(bobSession, doc.getId());
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
        Comment comment = createAndCheckComment(session, doc, commentToCreate, 1);
        String commentId = comment.getId();

        Comment newComment = createSampleComment(doc.getId());
        newComment.setText("This a new text on this comment");

        // I can update the comment if I'm the author
        Comment updatedComment = commentManager.updateComment(session, commentId, newComment);
        verifyCommonsInfo(doc, newComment, updatedComment, 1);
        Instant firstModification = updatedComment.getModificationDate();
        assertNotNull(firstModification);

        // re-init modification date
        newComment.setModificationDate(null);
        // I can update the comment if I'm an administrator
        CoreSession systemSession = coreFeature.getCoreSessionSystem();
        newComment.setText("Can you call me on my phone, please");
        updatedComment = commentManager.updateComment(systemSession, commentId, newComment);
        verifyCommonsInfo(doc, newComment, updatedComment, 1);
        assertNotNull(updatedComment.getModificationDate());
        assertTrue(firstModification.isBefore(updatedComment.getModificationDate()));
    }

    @Test
    public void iCannotUpdateComment() {
        DocumentModel doc = createDocumentModel("anyFile");

        Comment commentToCreate = createSampleComment(doc.getId());
        Comment createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        Comment newComment = createSampleComment(doc.getId());
        newComment.setText("I try to update this comment !");

        try {
            CoreSession bobSession = CoreInstance.getCoreSession(doc.getRepositoryName(), "bob");
            commentManager.updateComment(bobSession, createdComment.getId(), newComment);
            fail("bob should not be able to update a comment");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user bob does not have access to the comment %s", createdComment.getId()),
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
        CoreSession jamesSession = CoreInstance.getCoreSession(doc.getRepositoryName(), "james");
        commentManager.deleteComment(jamesSession, createdComment.getId());
        assertFalse(jamesSession.exists(new IdRef(createdComment.getId())));
        assertFalse(session.exists(new IdRef(createdComment.getId())));

        // Create another comment with another author
        commentToCreate = createSampleComment(doc.getId());
        createdComment = createAndCheckComment(session, doc, commentToCreate, 1);

        // I have an `Everything` permissions on the doc, then i can remove it
        ACPImpl acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("julia", SecurityConstants.EVERYTHING, true));
        session.setACP(doc.getRef(), acp, false);
        transactionalFeature.nextTransaction();
        CoreSession juliaSession = CoreInstance.getCoreSession(doc.getRepositoryName(), "julia");
        commentManager.deleteComment(juliaSession, createdComment.getId());

        try {
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

        try {
            CoreSession bobSession = CoreInstance.getCoreSession(doc.getRepositoryName(), "bob");
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

    /*
     * NXP-28700
     */
    @Test
    public void testGetExternalCommentPageProviderReturnsRightCommentAndNotVersionOnes() {
        DocumentModel docToComment = createDocumentModel("fileToComment");
        var comment = new CommentImpl();
        comment.setParentId(docToComment.getId());
        comment.setText(COMMENT_TEXT);
        comment.setEntityId("foo");

        commentManager.createComment(session, comment);

        docToComment.checkIn(VersioningOption.MINOR, "checkin comment");
        // we now have two external entities with id foo in repository
        PartialList<Map<String, Serializable>> projection = session.queryProjection(
                "SELECT * FROM Comment where externalEntity:entityId='foo'", 10, 0);
        assertEquals(2, projection.size());

        // test external entity retrieval with comment manager
        var externalComment = commentManager.getExternalComment(session, "foo");
        assertEquals(docToComment.getId(), externalComment.getParentId());
        assertEquals(COMMENT_TEXT, externalComment.getText());

        // now test page provider used internally by comment manager
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = singletonMap(CORE_SESSION_PROPERTY, (Serializable) session);
        var pageProvider = ppService.getPageProvider(GET_COMMENT_PAGE_PROVIDER_NAME, Collections.emptyList(), 10L, 0L,
                props, "foo");
        assertEquals(1, pageProvider.getCurrentPageSize());
    }

    /*
     * NXP-28719
     */
    @Test
    public void testCreateCommentsAndRepliesUnderPlacelessDocument() {
        DocumentModel anyFile = session.createDocumentModel(null, "anyFile", "File");
        anyFile = session.createDocument(anyFile);
        transactionalFeature.nextTransaction();

        // first comment
        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(anyFile.getId());

        comment = commentManager.createComment(session, comment);
        DocumentModel commentDocModel = session.getDocument(new IdRef(comment.getId()));
        DocumentModel ecmCommentParent = session.getDocument(commentDocModel.getParentRef());

        assertNotNull(commentDocModel.getParentRef());
        assertEquals(anyFile.getId(), commentDocModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY));
        assertTrue(ecmCommentParent.hasFacet("Folderish"));
        assertTrue(ecmCommentParent.hasFacet("HiddenInNavigation"));
        assertEquals(ecmCommentParent.getRef(), commentDocModel.getParentRef());
        assertEquals("anyFile/Comments", ecmCommentParent.getPathAsString());

        // a reply
        text = "I am a reply !";
        Comment reply = new CommentImpl();
        reply.setAuthor(author);
        reply.setText(text);
        reply.setParentId(comment.getId());

        reply = commentManager.createComment(session, reply);
        DocumentModel replyDocModel = session.getDocument(new IdRef(reply.getId()));
        assertEquals(comment.getId(), commentDocModel.getId());
        assertEquals(comment.getId(), replyDocModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY));
        assertEquals(COMMENT_DOC_TYPE, session.getDocument(replyDocModel.getParentRef()).getType());
        assertEquals( //
                String.format("anyFile/Comments/%s/%s", commentDocModel.getTitle(), replyDocModel.getTitle()),
                replyDocModel.getPathAsString());

        // another reply
        text = "I am a reply !";
        Comment reply2 = new CommentImpl();
        reply2.setAuthor(author);
        reply2.setText(text);
        reply2.setParentId(reply.getId());
        reply2 = commentManager.createComment(session, reply2);
        DocumentModel reply2DocModel = session.getDocument(new IdRef(reply2.getId()));
        assertEquals(COMMENT_DOC_TYPE, session.getDocument(reply2DocModel.getParentRef()).getType());
        String replyDocPath = String.format("anyFile/Comments/%s/%s/%s", //
                commentDocModel.getTitle(), replyDocModel.getTitle(), reply2DocModel.getTitle());
        assertEquals(replyDocPath, reply2DocModel.getPathAsString());
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
        return createSampleComment(parentId, COMMENT_TEXT);
    }

    /**
     * @return the sample comment
     */
    protected Comment createSampleComment(String parentId, String text) {
        Comment comment = new CommentImpl();
        comment.setParentId(parentId);
        comment.setAuthor(session.getPrincipal().getName());
        comment.setText(text);
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
                documentId, NXQL.ECM_PRIMARYTYPE, COMMENT_ROOT_DOC_TYPE);

        DocumentModelList documents = session.query(query);
        assertEquals(1, documents.size());
        return documents.get(0);
    }

    @Test
    public void shouldFindCommentedFileByFullTextSearch() {
        DocumentModel firstDocToComment = createDocumentModel("anotherFile1");
        DocumentModel secondDocToComment = createDocumentModel("anotherFile2");
        Map<DocumentRef, List<Comment>> mapCommentsByDocRef = createCommentsAndRepliesForFullTextSearch(
                firstDocToComment, secondDocToComment);
        transactionalFeature.nextTransaction();

        // One comment and 3 replies
        checkRelatedTextResource(firstDocToComment.getRef(), mapCommentsByDocRef.get(firstDocToComment.getRef()));

        // One comment and no replies
        checkRelatedTextResource(secondDocToComment.getRef(), mapCommentsByDocRef.get(secondDocToComment.getRef()));

        // We make a fulltext query to find the 2 commented files
        makeAndVerifyFullTextSearch("first comment", firstDocToComment, secondDocToComment);

        // We make a fulltext query to find the second commented file
        makeAndVerifyFullTextSearch("secondFile", secondDocToComment);

        // We make a fulltext query to find the first commented file by any reply
        makeAndVerifyFullTextSearch("reply", firstDocToComment);

        // There is no commented file with the provided text
        makeAndVerifyFullTextSearch("UpdatedReply");

        // Get the second reply and update his text
        Comment secondCreatedReply = mapCommentsByDocRef.get(firstDocToComment.getRef()).get(2);
        secondCreatedReply.setText("I am an UpdatedReply");
        commentManager.updateComment(session, secondCreatedReply.getId(), secondCreatedReply);
        transactionalFeature.nextTransaction();

        // Now we should find the document with this updated reply text
        makeAndVerifyFullTextSearch("UpdatedReply", firstDocToComment);

        // Now let's remove this second reply
        commentManager.deleteComment(session, secondCreatedReply.getId());
        transactionalFeature.nextTransaction();
        makeAndVerifyFullTextSearch("UpdatedReply");

        List<Comment> comments = mapCommentsByDocRef.get(firstDocToComment.getRef())
                                                    .stream()
                                                    .filter(c -> !c.getId().equals(secondCreatedReply.getId()))
                                                    .collect(Collectors.toList());
        checkRelatedTextResource(firstDocToComment.getRef(), comments);
    }

    protected Map<DocumentRef, List<Comment>> createCommentsAndRepliesForFullTextSearch(DocumentModel firstDocToComment,
            DocumentModel secondDocToComment) {

        // Create 2 comments on the two files
        Comment firstCommentOfFile1 = createSampleComment(firstDocToComment.getId(),
                "I am the first comment of firstFile");
        firstCommentOfFile1 = commentManager.createComment(session, firstCommentOfFile1);

        Comment firstCommentOfFile2 = createSampleComment(secondDocToComment.getId(),
                "I am the first comment of secondFile");
        firstCommentOfFile2 = commentManager.createComment(session, firstCommentOfFile2);

        // the comment container is created with the atomic CoreSession#getOrCreateDocument operation which commits the
        // transaction (and trigger async actions) - so wait for these actions to complete
        transactionalFeature.nextTransaction();

        // Create first reply on first comment of first file
        Comment firstReply = createSampleComment(firstCommentOfFile1.getId(), "I am the first reply of first comment");
        Comment firstCreatedReply = commentManager.createComment(session, firstReply);

        // Create second reply
        Comment secondReply = createSampleComment(firstCreatedReply.getId(), "I am the second reply of first comment");
        Comment secondCreatedReply = commentManager.createComment(session, secondReply);

        // Create third reply
        Comment thirdReply = createSampleComment(secondCreatedReply.getId(), "I am the third reply of first comment");
        Comment thirdCreatedReply = commentManager.createComment(session, thirdReply);

        return Map.of( //
                firstDocToComment.getRef(),
                List.of(firstCommentOfFile1, firstCreatedReply, secondCreatedReply, thirdCreatedReply), //
                secondDocToComment.getRef(), List.of(firstCommentOfFile2) //
        );
    }

    protected void makeAndVerifyFullTextSearch(String ecmFullText, DocumentModel... expectedDocs) {
        String query = String.format(
                "SELECT * FROM Document WHERE ecm:fulltext = '%s' AND ecm:mixinType != 'HiddenInNavigation'",
                ecmFullText);

        DocumentModelList documents = session.query(query);

        Arrays.sort(expectedDocs, Comparator.comparing(DocumentModel::getId));
        documents.sort(Comparator.comparing(DocumentModel::getId));
        assertArrayEquals(expectedDocs, documents.toArray(new DocumentModel[0]));
    }

    @SuppressWarnings("unchecked")
    protected void checkRelatedTextResource(DocumentRef documentRef, List<Comment> comments) {
        DocumentModel doc = session.getDocument(documentRef);

        List<Map<String, String>> resources = (List<Map<String, String>>) doc.getPropertyValue(RELATED_TEXT_RESOURCES);

        List<String> expectedRelatedTextIds = comments.stream()
                                                      .map(c -> String.format(COMMENT_RELATED_TEXT_ID, c.getId()))
                                                      .sorted()
                                                      .collect(Collectors.toList());

        List<String> expectedRelatedText = comments.stream()
                                                   .map(Comment::getText)
                                                   .sorted()
                                                   .collect(Collectors.toList());

        assertEquals(expectedRelatedTextIds,
                resources.stream().map(m -> m.get(RELATED_TEXT_ID)).sorted().collect(Collectors.toList()));

        assertEquals(expectedRelatedText,
                resources.stream().map(m -> m.get(RELATED_TEXT)).sorted().collect(Collectors.toList()));
    }

    @Override
    public Class<? extends CommentManager> getType() {
        return TreeCommentManager.class;
    }
}
