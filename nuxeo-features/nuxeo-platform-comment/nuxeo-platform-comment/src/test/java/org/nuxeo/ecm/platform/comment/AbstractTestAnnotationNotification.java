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

import java.time.Instant;
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
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationImpl;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.mail.SmtpMailServerFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ NotificationCommentFeature.class, SmtpMailServerFeature.class })
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
        // We subscribe to the creation document to check that we will not be notified about the annotation creation as
        // document (see CommentCreationVeto), only the annotation added, and the 'File' document creation
        captureAndVerifyAnnotationEventNotification(() -> {
            Annotation createdAnnotation = createAnnotationAndAddSubscription("CommentAdded", "Creation");
            return session.getDocument(new IdRef(createdAnnotation.getId()));
        }, COMMENT_ADDED, DOCUMENT_CREATED);
    }

    @Test
    public void shouldNotifyEventWhenUpdateAnnotation() {
        // We subscribe to the update document to check that we will not be notified about the annotation updated as
        // document (see CommentModificationVeto), only the annotation updated.
        Annotation annotation = createAnnotationAndAddSubscription("CommentUpdated", "Modification");

        captureAndVerifyAnnotationEventNotification(() -> {
            annotation.setText("I update the annotation");
            annotationService.updateAnnotation(session, annotation.getId(), annotation);
            return session.getDocument(new IdRef(annotation.getId()));
        }, COMMENT_UPDATED, DOCUMENT_UPDATED);
    }

    @Test
    public void shouldNotifyEventWhenRemoveAnnotation() {
        Annotation createdAnnotation = createAnnotationAndAddSubscription("CommentRemoved");
        DocumentModel annotationDocModel = session.getDocument(new IdRef(createdAnnotation.getId()));
        annotationDocModel.detach(true);

        captureAndVerifyAnnotationEventNotification(() -> {
            annotationService.deleteAnnotation(session, createdAnnotation.getId());
            return annotationDocModel;
        }, COMMENT_REMOVED, DOCUMENT_REMOVED);
    }

    @Test
    public void testCommentManagerType() {
        assertEquals(getType(), commentManager.getClass());
    }

    protected void captureAndVerifyAnnotationEventNotification(Supplier<DocumentModel> supplier,
            String annotationEventType, String documentEventType) {
        try (CapturingEventListener listener = new CapturingEventListener(annotationEventType, documentEventType)) {
            DocumentModel annotationDocumentModel = supplier.get();
            DocumentModel annotationParentDocumentModel = session.getDocument(new IdRef(
                    (String) annotationDocumentModel.getPropertyValue(COMMENT_PARENT_ID)));
            transactionalFeature.nextTransaction();

            assertTrue(listener.hasBeenFired(annotationEventType));
            assertTrue(listener.hasBeenFired(documentEventType));

            List<Event> handledEvents = listener.getCapturedEvents()
                                                .stream()
                                                .filter(e -> annotationEventType.equals(e.getName()))
                                                .collect(Collectors.toList());

            assertEquals(1, handledEvents.size());
            Event expectedEvent = handledEvents.get(0);
            assertEquals(annotationEventType, expectedEvent.getName());

            checkDocumentEventContext(expectedEvent, annotationDocumentModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
            checkReceivedMail(emailsResult.getMails(), annotationDocumentModel, annotatedDocumentModel,
                    handledEvents.get(0), annotationEventType);
        }
    }

    @Test
    public void shouldNotifyWithTheRightAnnotatedDocument() {
        // First comment
        Annotation createdAnnotation = createAnnotation(annotatedDocumentModel);
        DocumentModel createdAnnotationDocModel = session.getDocument(new IdRef(createdAnnotation.getId()));
        // before subscribing, or previous event will be notified as well
        transactionalFeature.nextTransaction();
        // Reply
        captureAndVerifyAnnotationEventNotification(() -> {
            // subscribe to notifications
            addSubscriptions("CommentAdded");

            Comment reply = createAnnotation(createdAnnotationDocModel);
            DocumentModel replyDocumentModel = session.getDocument(new IdRef(reply.getId()));
            return session.getDocument(new IdRef(replyDocumentModel.getId()));
        }, COMMENT_ADDED, DOCUMENT_CREATED);
    }

    protected Annotation createAnnotationAndAddSubscription(String... notifications) {
        addSubscriptions(notifications);
        return createAnnotation(annotatedDocumentModel);
    }

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
