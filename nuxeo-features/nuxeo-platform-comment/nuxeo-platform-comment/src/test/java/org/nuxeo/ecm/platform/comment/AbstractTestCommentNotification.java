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
import static org.nuxeo.ecm.platform.comment.CommentUtils.checkDocumentEventContext;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_ADDED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_REMOVED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_UPDATED;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;

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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationCommentFeature.class)
public abstract class AbstractTestCommentNotification {

    protected static final String COMMENT_ADDED_NOTIFICATION = "CommentAdded";

    protected static final String COMMENT_UPDATED_NOTIFICATION = "CommentUpdated";

    protected static final String ADMINISTRATOR = "Administrator";

    protected static final String ANY_COMMENT_MESSAGE = "any Comment message";

    @Inject
    protected NotificationService notificationService;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature transactionalFeature;

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
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
            Comment comment = createComment(commentedDocumentModel);
            transactionalFeature.nextTransaction();
            DocumentModel commentDocumentModel = session.getDocument(new IdRef(comment.getId()));
            DocumentModel commentParentDocumentModel = session.getDocument(
                    new IdRef((String) commentDocumentModel.getPropertyValue(COMMENT_PARENT_ID)));

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, commentDocumentModel, commentParentDocumentModel,
                    commentedDocumentModel);
        }
    }

    @Test
    public void shouldNotifyEventWhenUpdateComment() {
        Comment comment = createComment(commentedDocumentModel);
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_UPDATED)) {
            comment.setText("I update the comment");
            commentManager.updateComment(session, comment.getId(), comment);
            transactionalFeature.nextTransaction();
            DocumentModel commentDocumentModel = session.getDocument(new IdRef(comment.getId()));
            DocumentModel commentParentDocumentModel = session.getDocument(
                    new IdRef((String) commentDocumentModel.getPropertyValue(COMMENT_PARENT_ID)));

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, commentDocumentModel, commentParentDocumentModel,
                    commentedDocumentModel);
        }
    }

    @Test
    public void shouldNotifyEventWhenRemoveComment() {
        Comment comment = createComment(commentedDocumentModel);
        DocumentModel commentDocModel = session.getDocument(new IdRef(comment.getId()));
        commentDocModel.detach(true);
        transactionalFeature.nextTransaction();
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_REMOVED)) {
            commentManager.deleteComment(session, comment.getId());
            DocumentModel commentParentDocumentModel = session.getDocument(
                    new IdRef((String) commentDocModel.getPropertyValue(COMMENT_PARENT_ID)));
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, commentDocModel, commentParentDocumentModel,
                    commentedDocumentModel);
        }
    }

    @Test
    public void shouldNotifyWithTheRightCommentedDocument() {
        // First comment
        Comment comment = createComment(commentedDocumentModel, ADMINISTRATOR, ANY_COMMENT_MESSAGE);
        // before subscribing, or previous event will be notified as well
        transactionalFeature.nextTransaction();
        // Reply
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
            DocumentModel commentDocModel = session.getDocument(new IdRef(comment.getId()));
            Comment reply = createComment(commentDocModel);
            transactionalFeature.nextTransaction();
            DocumentModel replyDocumentModel = session.getDocument(new IdRef(reply.getId()));
            DocumentModel commentParentDocumentModel = session.getDocument(
                    new IdRef((String) replyDocumentModel.getPropertyValue(COMMENT_PARENT_ID)));

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, replyDocumentModel, commentParentDocumentModel,
                    commentedDocumentModel);
        }
    }

    @Deprecated
    protected void addSubscriptions(String... notifications) {
        NuxeoPrincipal principal = session.getPrincipal();
        String subscriber = NotificationConstants.USER_PREFIX + principal.getName();
        for (String notif : notifications) {
            notificationService.addSubscription(subscriber, notif, commentedDocumentModel, false, principal, notif);
        }
    }

    @Test
    public void testCommentManagerType() {
        assertEquals(getType(), commentManager.getClass());
    }

    @Deprecated
    protected Comment createCommentAndAddSubscription(String... notifications) {
        addSubscriptions(notifications);
        return createComment(commentedDocumentModel);
    }

    protected Comment createComment(DocumentModel commentedDocModel) {
        return createComment(commentedDocModel, ADMINISTRATOR, ANY_COMMENT_MESSAGE);
    }

    protected Comment createComment(DocumentModel commentedDocModel, String author, String text) {
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(commentedDocModel.getId());

        return commentManager.createComment(session, comment);
    }

    protected abstract Class<? extends CommentManager> getType();
}
