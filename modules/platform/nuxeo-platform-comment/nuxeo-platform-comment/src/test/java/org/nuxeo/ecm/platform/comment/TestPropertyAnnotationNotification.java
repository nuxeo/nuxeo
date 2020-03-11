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
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.mail.SmtpMailServerFeature.MailMessage;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 11.1
 * @deprecated since 11.1, in order to follow service depreciation
 */
@Deprecated(since = "11.1")
@Features(PropertyCommentFeature.class)
public class TestPropertyAnnotationNotification extends AbstractTestAnnotationNotification {

    @Override
    protected Class<? extends CommentManager> getType() {
        return PropertyCommentManager.class;
    }

    @Test
    @Override
    public void shouldNotifyEventWhenUpdateAnnotation() {
        Annotation annotation = createAnnotationAndAddSubscription("CommentUpdated");
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_UPDATED)) {
            annotation.setText("I update the annotation");
            annotationService.updateAnnotation(session, annotation.getId(), annotation);
            transactionalFeature.nextTransaction();
            DocumentModel annotationDocumentModel = session.getDocument(new IdRef(annotation.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) annotationDocumentModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY)));

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, annotationDocumentModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
            List<MailMessage> mails = emailsResult.getMails();
            assertEquals(1, mails.size());
            String expectedMailContent = getExpectedMailContent(annotationDocumentModel, annotatedDocumentModel,
                    expectedEvent);
            assertEquals(expectedMailContent, getMailContent(mails.get(0)));
        }
    }

    @Test
    @Override
    public void shouldNotifyEventWhenRemoveAnnotation() {
        Annotation annotation = createAnnotation(annotatedDocumentModel);
        transactionalFeature.nextTransaction();
        // This CommentManager implementation doesn't autosubscription on comments.
        assertEquals(0, emailsResult.getMails().size());
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_REMOVED)) {
            DocumentModel annotationDocModel = session.getDocument(new IdRef(annotation.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) annotationDocModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY)));
            annotationService.deleteAnnotation(session, annotation.getId());
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, annotationDocModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
            // This CommentManagerImplementation doesn't autosubscribe to comments.
            assertEquals(0, emailsResult.getMails().size());
        }
    }

    @Test
    @Override
    public void shouldNotifyWithTheRightAnnotatedDocument() {
        // First comment
        Annotation annotation = createAnnotation(annotatedDocumentModel);
        // before subscribing, or previous event will be notified as well
        transactionalFeature.nextTransaction();
        addSubscriptions("CommentAdded");
        // Reply
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
            DocumentModel annotationDocModel = session.getDocument(new IdRef(annotation.getId()));
            Comment reply = createAnnotation(annotationDocModel);
            DocumentModel replyDocumentModel = session.getDocument(new IdRef(reply.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) replyDocumentModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY)));
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, replyDocumentModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
            List<MailMessage> mails = emailsResult.getMails();
            assertEquals(1, mails.size());
            String expectedMailContent = getExpectedMailContent(replyDocumentModel, annotatedDocumentModel,
                    expectedEvent);
            assertEquals(expectedMailContent, getMailContent(mails.get(0)));
        }
    }
}
