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

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.schema.FacetNames.HIDDEN_IN_NAVIGATION;
import static org.nuxeo.ecm.platform.comment.CommentUtils.checkDocumentEventContext;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_ADDED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_REMOVED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_UPDATED;
import static org.nuxeo.ecm.platform.comment.impl.AbstractCommentManager.COMMENTS_DIRECTORY;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * This abstract contains test code for the {@link org.nuxeo.ecm.platform.comment.impl.TreeCommentManager}
 * implementation which has auto subscriptions feature.
 * 
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationCommentFeature.class)
public abstract class AbstractTestCommentNotification {

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected CommentManager commentManager;

    protected final Class<?> commentManagerUnderTest;

    protected DocumentModel commentedDocModel;

    protected AbstractTestCommentNotification(Class<? extends CommentManager> commentManagerUnderTest) {
        this.commentManagerUnderTest = commentManagerUnderTest;
    }

    @Before
    public void before() {
        assertEquals(commentManagerUnderTest, commentManager.getClass());

        // Create the file under a domain is needed in this test context, due to the control of
        // NotificationEventListener#gatherConcernedUsersForDocument (doc.getPath().segmentCount() > 1)
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        commentedDocModel = session.createDocumentModel("/domain", "test", "File");
        commentedDocModel = session.createDocument(commentedDocModel);
        transactionalFeature.nextTransaction();
    }

    @Test
    public void shouldNotifyEventWhenCreateComment() {
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
            Comment comment = createCommentAndWaitAsync(commentedDocModel.getId());
            DocumentModel commentDocumentModel = comment.getDocument();
            DocumentModel commentParentDocumentModel = session.getDocument(new IdRef(comment.getParentId()));

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, commentDocumentModel, commentParentDocumentModel,
                    commentedDocModel);
        }
    }

    @Test
    public void shouldNotifyEventWhenUpdateComment() {
        Comment comment = createCommentAndWaitAsync(commentedDocModel.getId());
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_UPDATED)) {
            comment.setText("I update the comment");
            comment = commentManager.updateComment(session, comment.getId(), comment);
            transactionalFeature.nextTransaction();
            DocumentModel commentDocumentModel = comment.getDocument();
            DocumentModel commentParentDocumentModel = session.getDocument(new IdRef(comment.getParentId()));

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, commentDocumentModel, commentParentDocumentModel,
                    commentedDocModel);
        }
    }

    @Test
    public void shouldNotifyEventWhenRemoveComment() {
        Comment comment = createCommentAndWaitAsync(commentedDocModel.getId());
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_REMOVED)) {
            DocumentModel commentDocModel = comment.getDocument();
            DocumentModel commentParentDocumentModel = session.getDocument(new IdRef(comment.getParentId()));

            commentManager.deleteComment(session, comment.getId());
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, commentDocModel, commentParentDocumentModel, commentedDocModel);
        }
    }

    @Test
    public void shouldNotifyWithTheRightCommentedDocument() {
        // First comment
        Comment comment = createCommentAndWaitAsync(commentedDocModel.getId());
        // Reply
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
            Comment reply = createCommentAndWaitAsync(comment.getId());
            DocumentModel replyDocumentModel = reply.getDocument();
            DocumentModel commentParentDocumentModel = session.getDocument(new IdRef(reply.getParentId()));

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, replyDocumentModel, commentParentDocumentModel, commentedDocModel);
        }
    }

    @Test
    public void shouldDisableNotificationForCommentsContainer() {
        try (CapturingEventListener listener = new CapturingEventListener(DOCUMENT_CREATED)) {
            Comment comment = createCommentAndWaitAsync(commentedDocModel.getId());
            // we get two events (one for the Comments folder creation and one for the comment creation)
            assertEquals(2, listener.getCapturedEvents().size());
            // we ensure that the event related to the comments folder is disabled.
            DocumentEventContext eventCtx = listener.streamCapturedEventContexts(DocumentEventContext.class)
                                   .filter(c -> c.getSourceDocument().hasFacet(HIDDEN_IN_NAVIGATION))
                                   .findFirst()
                                   .orElseThrow(
                                           () -> new AssertionError("Unable to find comments container created event"));
            assertEquals(COMMENTS_DIRECTORY, eventCtx.getSourceDocument().getName());
            assertEquals(TRUE, eventCtx.getProperty(DISABLE_NOTIFICATION_SERVICE));
            // we ensure that the other event is related to comment creation
            boolean found = listener.streamCapturedEventContexts(DocumentEventContext.class)
                                    .anyMatch(c -> comment.getId().equals(c.getSourceDocument().getId()));
            assertTrue("documentCreated event for comment not found ", found);
        }
    }

    protected Comment createCommentAndWaitAsync(String docIdToComment) {
        Comment comment = newComment(docIdToComment, "any Comment message");
        comment = commentManager.createComment(session, comment);
        transactionalFeature.nextTransaction();
        return comment;
    }
}
