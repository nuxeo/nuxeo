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
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.platform.comment.CommentUtils.checkDocumentEventContext;
import static org.nuxeo.ecm.platform.comment.CommentUtils.checkReceivedMail;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_ADDED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_REMOVED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_UPDATED;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.mail.SmtpMailServerFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ NotificationCommentFeature.class, SmtpMailServerFeature.class })
public abstract class AbstractTestCommentNotification {

    @Inject
    protected NotificationService notificationService;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected SmtpMailServerFeature.MailsResult emailsResult;

    protected CommentManager commentManager;

    protected DocumentModel commentedDocumentModel;

    @Before
    public void before() {
        commentManager = getCommentManager();

        // Create the file under a domain is needed in this test context, due to the control of
        // NotificationEventListener#gatherConcernedUsersForDocument (doc.getPath().segmentCount() > 1)
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        commentedDocumentModel = session.createDocumentModel(domain.getPathAsString(), "test", "File");
        commentedDocumentModel = session.createDocument(commentedDocumentModel);
        transactionalFeature.nextTransaction();
    }

    /**
     * Overrides this method if you want to test the {@link org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager}.
     */
    protected CommentManager getCommentManager() {
        return Framework.getService(CommentManager.class);
    }

    @Test
    public void shouldNotifyEventWhenCreateComment() {
        // We subscribe to the creation document to check that we will not be notified about the comment creation as
        // document (see CommentCreationVeto), only the comment added, and the 'File' document creation
        captureAndVerifyCommentEventNotification(() -> {
            Comment createdComment = createCommentAndAddSubscription("CommentAdded", "Creation");
            return session.getDocument(new IdRef(createdComment.getId()));
        }, COMMENT_ADDED, DOCUMENT_CREATED);
    }

    @Test
    public void shouldNotifyEventWhenUpdateComment() {
        // We subscribe to the update document to check that we will not be notified about the comment updated as
        // document (see CommentModificationVeto), only the comment updated.
        Comment createdComment = createCommentAndAddSubscription("CommentUpdated", "Modification");

        captureAndVerifyCommentEventNotification(() -> {
            createdComment.setText("I update the message");
            commentManager.updateComment(session, createdComment.getId(), createdComment);
            return session.getDocument(new IdRef(createdComment.getId()));
        }, COMMENT_UPDATED, DOCUMENT_UPDATED);
    }

    @Test
    public void shouldNotifyEventWhenRemoveComment() {
        Comment createdComment = createCommentAndAddSubscription("CommentRemoved");
        DocumentModel commentDocModel = session.getDocument(new IdRef(createdComment.getId()));
        commentDocModel.detach(true);

        captureAndVerifyCommentEventNotification(() -> {
            commentManager.deleteComment(session, createdComment.getId());
            return commentDocModel;
        }, COMMENT_REMOVED, DOCUMENT_REMOVED);
    }

    @Test
    public void shouldNotifyWithTheRightCommentedDocument() {
        // First comment
        Comment createdComment = createComment(commentedDocumentModel);
        DocumentModel createdCommentDocModel = session.getDocument(new IdRef(createdComment.getId()));
        // before subscribing, or previous event will be notified as well
        transactionalFeature.nextTransaction();
        // Reply
        captureAndVerifyCommentEventNotification(() -> {
            addSubscriptions("CommentAdded");

            Comment reply = createComment(createdCommentDocModel);
            DocumentModel replyDocumentModel = session.getDocument(new IdRef(reply.getId()));
            return session.getDocument(new IdRef(replyDocumentModel.getId()));
        }, COMMENT_ADDED, DOCUMENT_CREATED);
    }

    private void addSubscriptions(String... notifications) {
        NuxeoPrincipal principal = session.getPrincipal();
        String subscriber = NotificationConstants.USER_PREFIX + principal.getName();
        for (String notif : notifications) {
            notificationService.addSubscription(subscriber, notif, commentedDocumentModel, false, principal, notif);
        }
    }

    protected void captureAndVerifyCommentEventNotification(Supplier<DocumentModel> supplier, String commentEventType,
            String documentEventType) {
        try (CapturingEventListener listener = new CapturingEventListener(commentEventType, documentEventType)) {
            DocumentModel commentDocumentModel = supplier.get();
            DocumentModel commentParentDocumentModel = session.getDocument(new IdRef(
                    (String) commentDocumentModel.getPropertyValue(COMMENT_PARENT_ID)));
            transactionalFeature.nextTransaction();

            assertTrue(listener.hasBeenFired(commentEventType));
            assertTrue(listener.hasBeenFired(documentEventType));

            List<Event> handledEvents = listener.streamCapturedEvents()
                                                .filter(e -> commentEventType.equals(e.getName()))
                                                .collect(Collectors.toList());

            assertEquals(1, handledEvents.size());

            checkDocumentEventContext(handledEvents.get(0), commentDocumentModel, commentParentDocumentModel,
                    commentedDocumentModel);
            checkReceivedMail(emailsResult.getMails(), commentDocumentModel, commentedDocumentModel,
                    handledEvents.get(0), commentEventType);
        }
    }

    @Test
    public void testCommentManagerType() {
        assertEquals(getType(), commentManager.getClass());
    }

    protected Comment createCommentAndAddSubscription(String... notifications) {
        addSubscriptions(notifications);
        return createComment(commentedDocumentModel);
    }

    protected Comment createComment(DocumentModel commentedDocModel) {
        Comment comment = new CommentImpl();
        comment.setAuthor("Administrator");
        comment.setText("any Comment message");
        comment.setParentId(commentedDocModel.getId());

        return commentManager.createComment(session, comment);
    }

    protected abstract Class<? extends CommentManager> getType();
}
