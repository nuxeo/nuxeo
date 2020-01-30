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

import java.time.Instant;
import java.util.List;

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
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationImpl;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.mail.SmtpMailServerFeature;
import org.nuxeo.mail.SmtpMailServerFeature.MailMessage;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ CommentNotificationFeature.class, SmtpMailServerFeature.class })
public abstract class AbstractTestAnnotationNotification {

    @Inject
    protected AnnotationService annotationService;

    @Inject
    protected CommentManager commentManager;

    @Inject
    protected CoreSession session;

    @Inject
    protected NotificationService notificationService;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected SmtpMailServerFeature.MailsResult emailsResult;

    protected DocumentModel annotatedDocumentModel;

    @Before
    public void before() {
        // Create the file under a domain is needed in this test context, due to the control of
        // NotificationEventListener#gatherConcernedUsersForDocument (doc.getPath().segmentCount() > 1)
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        annotatedDocumentModel = session.createDocumentModel(domain.getPathAsString(), "test", "File");
        annotatedDocumentModel = session.createDocument(annotatedDocumentModel);
        transactionalFeature.nextTransaction();
    }

    @Test
    public void shouldNotifyEventWhenCreateAnnotation() {
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
            Annotation annotation = createAnnotationAndAddSubscription("CommentAdded");
            DocumentModel annotationDocumentModel = session.getDocument(new IdRef(annotation.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) annotationDocumentModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY)));
            transactionalFeature.nextTransaction();

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
    public void shouldNotifyEventWhenUpdateAnnotation() {
        Annotation annotation = createAnnotation(annotatedDocumentModel);
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_UPDATED)) {
            annotation.setText("I update the annotation");
            annotationService.updateAnnotation(session, annotation.getId(), annotation);
            DocumentModel annotationDocumentModel = session.getDocument(new IdRef(annotation.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) annotationDocumentModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY)));
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, annotationDocumentModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
            List<MailMessage> mails = emailsResult.getMails();
            assertEquals(2, mails.size());
            String expectedMailContent = getExpectedMailContent(annotationDocumentModel, annotatedDocumentModel,
                    expectedEvent);
            assertEquals(expectedMailContent, getMailContent(mails.get(1)));
        }
    }

    @Test
    public void shouldNotifyEventWhenRemoveAnnotation() {
        Annotation annotation = createAnnotation(annotatedDocumentModel);
        DocumentModel annotationDocModel = session.getDocument(new IdRef(annotation.getId()));
        annotationDocModel.detach(true);
        transactionalFeature.nextTransaction();
        // Notified by comment added
        assertEquals(1, emailsResult.getMails().size());
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_REMOVED)) {
            annotationService.deleteAnnotation(session, annotation.getId());
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) annotationDocModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY)));
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, annotationDocModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
            // No new mail was sent when comment removed
            List<MailMessage> mails = emailsResult.getMails();
            assertEquals(1, mails.size());
        }
    }

    @Test
    public void shouldNotifyWithTheRightAnnotatedDocument() {
        // First comment
        Annotation annotation = createAnnotation(annotatedDocumentModel);
        DocumentModel annotationDocModel = session.getDocument(new IdRef(annotation.getId()));
        // before subscribing, or previous event will be notified as well
        transactionalFeature.nextTransaction();
        // Reply
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
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
            assertEquals(2, mails.size());
            String expectedMailContent = getExpectedMailContent(replyDocumentModel, annotatedDocumentModel,
                    expectedEvent);
            assertEquals(expectedMailContent, getMailContent(mails.get(1)));
        }
    }

    @Test
    public void testCommentManagerType() {
        assertEquals(getType(), commentManager.getClass());
    }

    @Deprecated(since = "11.1")
    protected Annotation createAnnotationAndAddSubscription(String... notifications) {
        addSubscriptions(notifications);
        return createAnnotation(annotatedDocumentModel);
    }

    @Deprecated(since = "11.1")
    protected void addSubscriptions(String... notifications) {
        NuxeoPrincipal principal = session.getPrincipal();
        String subscriber = NotificationConstants.USER_PREFIX + principal.getName();
        for (String notif : notifications) {
            notificationService.addSubscription(subscriber, notif, annotatedDocumentModel, false, principal, notif);
        }
    }

    protected Annotation createAnnotation(DocumentModel annotatedDocModel) {
        Annotation annotation = new AnnotationImpl();
        annotation.setAuthor(session.getPrincipal().getName());
        annotation.setText("Any annotation message");
        annotation.setParentId(annotatedDocModel.getId());
        annotation.setXpath("files:files/0/file");
        annotation.setCreationDate(Instant.now());
        annotation.setModificationDate(Instant.now());
        ExternalEntity externalEntity = (ExternalEntity) annotation;
        externalEntity.setEntityId("foo");
        externalEntity.setOrigin("any origin");
        externalEntity.setEntity("<entity><annotation>bar</annotation></entity>");

        return annotationService.createAnnotation(session, annotation);
    }

    protected abstract Class<? extends CommentManager> getType();
}
