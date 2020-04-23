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

package org.nuxeo.ecm.platform.comment;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.platform.comment.CommentUtils.checkDocumentEventContext;
import static org.nuxeo.ecm.platform.comment.CommentUtils.getExpectedMailContent;
import static org.nuxeo.ecm.platform.comment.CommentUtils.getMailContent;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PARENT_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_ADDED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_REMOVED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_UPDATED;

import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;
import org.nuxeo.mail.SmtpMailServerFeature.MailMessage;

/**
 * @since 11.1
 */
public class TestBridgeFromPropertyToTreeCommentNotification extends AbstractTestCommentNotification {

    @Override
    @SuppressWarnings("deprecation")
    protected CommentManager getCommentManager() {
        return new BridgeCommentManager(new PropertyCommentManager(), new TreeCommentManager());
    }

    @Override
    protected Class<? extends CommentManager> getType() {
        return BridgeCommentManager.class;
    }

    @Test
    @Override
    public void shouldNotifyEventWhenUpdateComment() {
        // We subscribe to the update document to check that we will not be notified about the comment updated as
        // document (see CommentModificationVeto), only the comment updated.
        Comment comment = createComment(commentedDocumentModel);
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_UPDATED)) {
            comment.setText("I update the comment");
            commentManager.updateComment(session, comment.getId(), comment);
            transactionalFeature.nextTransaction();
            DocumentModel commentDocumentModel = session.getDocument(new IdRef(comment.getId()));
            DocumentModel commentParentDocumentModel = session.getDocument(
                    new IdRef((String) commentDocumentModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY)));
            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, commentDocumentModel, commentParentDocumentModel,
                    commentedDocumentModel);

            List<MailMessage> mails = emailsResult.getMails();
            assertEquals(2, mails.size());
            String expectedMailContent = getExpectedMailContent(commentDocumentModel, commentedDocumentModel,
                    expectedEvent);
            assertEquals(expectedMailContent, getMailContent(mails.get(1)));
        }
    }

    @Test
    @Override
    public void shouldNotifyEventWhenRemoveComment() {
        Comment comment = createComment(commentedDocumentModel);
        transactionalFeature.nextTransaction();
        // There is already 1 mail for comment added with autosubscription
        List<MailMessage> mails = emailsResult.getMails();
        assertEquals(1, mails.size());
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_REMOVED)) {
            DocumentModel commentDocModel = session.getDocument(new IdRef(comment.getId()));
            commentManager.deleteComment(session, comment.getId());
            transactionalFeature.nextTransaction();
            DocumentModel commentParentDocumentModel = session.getDocument(
                    new IdRef((String) commentDocModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY)));

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, commentDocModel, commentParentDocumentModel,
                    commentedDocumentModel);
            // No additional mail was sent
            mails = emailsResult.getMails();
            assertEquals(1, mails.size());
        }
    }

    @Test
    @Override
    public void shouldNotifyWithTheRightCommentedDocument() {
        // First comment
        Comment comment = createComment(commentedDocumentModel);
        // before subscribing, or previous event will be notified as well
        transactionalFeature.nextTransaction();
        // Reply
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
            DocumentModel commentDocModel = session.getDocument(new IdRef(comment.getId()));
            Comment reply = createComment(commentDocModel);
            DocumentModel replyDocumentModel = session.getDocument(new IdRef(reply.getId()));
            DocumentModel commentParentDocumentModel = session.getDocument(
                    new IdRef((String) replyDocumentModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY)));
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, replyDocumentModel, commentParentDocumentModel,
                    commentedDocumentModel);
            List<MailMessage> mails = emailsResult.getMails();
            assertEquals(2, mails.size());
            String expectedMailContent = getExpectedMailContent(replyDocumentModel, commentedDocumentModel,
                    expectedEvent);
            assertEquals(expectedMailContent, getMailContent(mails.get(1)));
        }
    }

}
