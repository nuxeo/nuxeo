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
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.comment.CommentUtils.checkDocumentEventContext;
import static org.nuxeo.ecm.platform.comment.CommentUtils.createUser;
import static org.nuxeo.ecm.platform.comment.CommentUtils.getExpectedMailContent;
import static org.nuxeo.ecm.platform.comment.CommentUtils.getMailContent;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newAnnotation;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_ADDED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_REMOVED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_UPDATED;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.mail.SmtpMailServerFeature;
import org.nuxeo.mail.SmtpMailServerFeature.MailMessage;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * This class contains test code for the {@link org.nuxeo.ecm.platform.comment.impl.TreeCommentManager} implementation
 * which has auto subscriptions feature.
 * 
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ CommentNotificationFeature.class, SmtpMailServerFeature.class })
public class TestAnnotationNotification {

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected AnnotationService annotationService;

    @Inject
    protected NotificationManager notificationManager;

    @Inject
    protected SmtpMailServerFeature.MailsResult emailsResult;

    protected DocumentModel annotatedDocModel;

    @Before
    public void before() {
        // Create the file under a domain is needed in this test context, due to the control of
        // NotificationEventListener#gatherConcernedUsersForDocument (doc.getPath().segmentCount() > 1)
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        annotatedDocModel = session.createDocumentModel(domain.getPathAsString(), "test", "File");
        annotatedDocModel = session.createDocument(annotatedDocModel);
        transactionalFeature.nextTransaction();
    }

    @Test
    public void shouldNotifyEventWhenCreateAnnotation() {
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
            Annotation annotation = createAnnotationAndWaitAsync(annotatedDocModel.getId());

            DocumentModel annotationDocumentModel = annotation.getDocument();
            DocumentModel annotationParentDocumentModel = session.getDocument(new IdRef(annotation.getParentId()));

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, annotationDocumentModel, annotationParentDocumentModel,
                    annotatedDocModel);

            List<MailMessage> mails = emailsResult.getMails();
            assertEquals(1, mails.size());
            String expectedMailContent = getExpectedMailContent(annotationDocumentModel, annotatedDocModel,
                    expectedEvent);
            assertEquals(expectedMailContent, getMailContent(mails.get(0)));
        }
    }

    @Test
    public void shouldNotifyEventWhenUpdateAnnotation() {
        Annotation annotation = createAnnotationAndWaitAsync(annotatedDocModel.getId());
        // clear mail created on comment added
        emailsResult.clearMails();
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_UPDATED)) {
            annotation.setText("I update the annotation");
            annotationService.updateAnnotation(session, annotation.getId(), annotation);
            transactionalFeature.nextTransaction();
            DocumentModel annotationDocumentModel = annotation.getDocument();
            DocumentModel annotationParentDocumentModel = session.getDocument(new IdRef(annotation.getParentId()));

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, annotationDocumentModel, annotationParentDocumentModel,
                    annotatedDocModel);
            List<MailMessage> mails = emailsResult.getMails();
            assertEquals(1, mails.size());
            String expectedMailContent = getExpectedMailContent(annotationDocumentModel, annotatedDocModel,
                    expectedEvent);
            assertEquals(expectedMailContent, getMailContent(mails.get(0)));
        }
    }

    @Test
    public void shouldNotifyEventWhenRemoveAnnotation() {
        Annotation annotation = createAnnotationAndWaitAsync(annotatedDocModel.getId());
        // clear mail created on comment added
        emailsResult.clearMails();
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_REMOVED)) {
            DocumentModel annotationDocModel = annotation.getDocument();
            DocumentModel annotationParentDocumentModel = session.getDocument(new IdRef(annotation.getParentId()));

            annotationService.deleteAnnotation(session, annotation.getId());
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, annotationDocModel, annotationParentDocumentModel,
                    annotatedDocModel);
            // No new mail was sent when comment removed
            List<MailMessage> mails = emailsResult.getMails();
            assertTrue(mails.isEmpty());
        }
    }

    @Test
    public void shouldNotifyWithTheRightAnnotatedDocument() {
        // First comment
        Annotation annotation = createAnnotationAndWaitAsync(annotatedDocModel.getId());
        // clear mail created on comment added
        emailsResult.clearMails();
        // Reply
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {

            Annotation reply = createAnnotationAndWaitAsync(annotation.getId());
            DocumentModel replyDocumentModel = reply.getDocument();
            DocumentModel annotationParentDocumentModel = session.getDocument(new IdRef(reply.getParentId()));

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, replyDocumentModel, annotationParentDocumentModel,
                    annotatedDocModel);

            List<MailMessage> mails = emailsResult.getMails();
            assertEquals(1, mails.size());
            String expectedMailContent = getExpectedMailContent(replyDocumentModel, annotatedDocModel, expectedEvent);
            assertEquals(expectedMailContent, getMailContent(mails.get(0)));
        }
    }

    protected Annotation createAnnotationAndWaitAsync(String docIdToAnnotate) {
        Annotation annotation = newAnnotation(docIdToAnnotate, "file:content", "Any annotation message");
        annotation = annotationService.createAnnotation(session, annotation);
        transactionalFeature.nextTransaction();
        return annotation;
    }

    @Test
    public void testAutoSubscribingOnlyOnceToNewAnnotations() {
        String john = "john";
        String johnSubscription = NotificationConstants.USER_PREFIX + john;
        createUser(john);

        // check subscriptions has not been created
        List<String> subscriptions = notificationManager.getSubscriptionsForUserOnDocument(johnSubscription,
                annotatedDocModel);
        assertEquals(0, subscriptions.size());

        // create an annotation and check auto subscriptions
        createAnnotationAndRefreshAnnotatedDocument(john);
        subscriptions = notificationManager.getSubscriptionsForUserOnDocument(johnSubscription, annotatedDocModel);
        assertTrue(subscriptions.contains("CommentAdded"));
        assertTrue(subscriptions.contains("CommentUpdated"));

        // remove subscriptions, re-create an annotation and check no auto subscriptions
        for (String subscription : subscriptions) {
            notificationManager.removeSubscription(johnSubscription, subscription, annotatedDocModel);
        }
        createAnnotationAndRefreshAnnotatedDocument(john);
        subscriptions = notificationManager.getSubscriptionsForUserOnDocument(johnSubscription, annotatedDocModel);
        assertTrue(subscriptions.isEmpty());
    }

    protected void createAnnotationAndRefreshAnnotatedDocument(String author) {
        Annotation annotation = newAnnotation(annotatedDocModel.getId(), "file:content", "Test message");
        annotation.setAuthor(author);
        annotationService.createAnnotation(session, annotation);
        transactionalFeature.nextTransaction();
        annotatedDocModel.refresh();
    }
}
