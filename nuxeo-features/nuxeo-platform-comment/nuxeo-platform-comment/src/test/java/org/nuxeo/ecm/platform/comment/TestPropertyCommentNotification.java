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

import static org.nuxeo.ecm.platform.comment.CommentUtils.checkDocumentEventContext;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_ADDED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_REMOVED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_UPDATED;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;

/**
 * @since 11.1
 */
public class TestPropertyCommentNotification extends AbstractTestCommentNotification {

    @Override
    protected Class<? extends CommentManager> getType() {
        return PropertyCommentManager.class;
    }

    @Test
    @Override
    public void shouldNotifyEventWhenCreateComment() {
        addSubscriptions("CommentAdded");
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
    @Override
    public void shouldNotifyEventWhenUpdateComment() {
        addSubscriptions("CommentUpdated");
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
    @Override
    public void shouldNotifyEventWhenRemoveComment() {
        Comment comment = createComment(commentedDocumentModel);
        transactionalFeature.nextTransaction();
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_REMOVED)) {
            DocumentModel commentDocModel = session.getDocument(new IdRef(comment.getId()));
            commentManager.deleteComment(session, comment.getId());
            transactionalFeature.nextTransaction();
            DocumentModel commentParentDocumentModel = session.getDocument(
                    new IdRef((String) commentDocModel.getPropertyValue(COMMENT_PARENT_ID)));

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, commentDocModel, commentParentDocumentModel,
                    commentedDocumentModel);
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
        addSubscriptions("CommentAdded");
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
}
