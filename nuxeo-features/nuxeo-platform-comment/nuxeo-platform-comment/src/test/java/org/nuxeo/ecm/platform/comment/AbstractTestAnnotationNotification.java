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

import java.time.Instant;

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
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationCommentFeature.class)
public abstract class AbstractTestAnnotationNotification {

    protected static final String COMMENT_ADDED_NOTIFICATION = "CommentAdded";

    protected static final String COMMENT_UPDATED_NOTIFICATION = "CommentUpdated";

    protected static final String ADMINISTRATOR = "Administrator";

    protected static final String ANY_ANNOTATION_MESSAGE = "any Annotation message";

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
            Annotation annotation = createAnnotationAndAddSubscription(COMMENT_ADDED_NOTIFICATION);
            transactionalFeature.nextTransaction();
            DocumentModel annotationDocumentModel = session.getDocument(new IdRef(annotation.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) annotationDocumentModel.getPropertyValue(COMMENT_PARENT_ID)));

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, annotationDocumentModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
        }
    }

    @Test
    public void shouldNotifyEventWhenUpdateAnnotation() {
        Annotation annotation = createAnnotation(annotatedDocumentModel);
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_UPDATED)) {
            annotation.setText("I update the annotation");
            annotationService.updateAnnotation(session, annotation.getId(), annotation);
            transactionalFeature.nextTransaction();
            DocumentModel annotationDocumentModel = session.getDocument(new IdRef(annotation.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) annotationDocumentModel.getPropertyValue(COMMENT_PARENT_ID)));

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, annotationDocumentModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
        }
    }

    @Test
    public void shouldNotifyEventWhenRemoveAnnotation() {
        Annotation annotation = createAnnotation(annotatedDocumentModel);
        transactionalFeature.nextTransaction();
        // Notified by comment added
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_REMOVED)) {
            DocumentModel annotationDocModel = session.getDocument(new IdRef(annotation.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) annotationDocModel.getPropertyValue(COMMENT_PARENT_ID)));
            annotationService.deleteAnnotation(session, annotation.getId());
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, annotationDocModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
        }
    }

    @Test
    public void shouldNotifyWithTheRightAnnotatedDocument() {
        // First annotation
        Annotation annotation = createAnnotation(annotatedDocumentModel, ADMINISTRATOR, ANY_ANNOTATION_MESSAGE);
        DocumentModel annotationDocModel = session.getDocument(new IdRef(annotation.getId()));
        // before subscribing, or previous event will be notified as well
        transactionalFeature.nextTransaction();
        // Reply
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
            Comment reply = createAnnotation(annotationDocModel);
            transactionalFeature.nextTransaction();
            DocumentModel replyDocumentModel = session.getDocument(new IdRef(reply.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) replyDocumentModel.getPropertyValue(COMMENT_PARENT_ID)));

            Event expectedEvent = listener.findFirstCapturedEventOrElseThrow();
            checkDocumentEventContext(expectedEvent, replyDocumentModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
        }
    }

    @Test
    public void testCommentManagerType() {
        assertEquals(getType(), commentManager.getClass());
    }

    @Deprecated
    protected Annotation createAnnotationAndAddSubscription(String... notifications) {
        addSubscriptions(notifications);
        return createAnnotation(annotatedDocumentModel);
    }

    @Deprecated
    protected void addSubscriptions(String... notifications) {
        NuxeoPrincipal principal = session.getPrincipal();
        String subscriber = NotificationConstants.USER_PREFIX + principal.getName();
        for (String notif : notifications) {
            notificationService.addSubscription(subscriber, notif, annotatedDocumentModel, false, principal, notif);
        }
    }

    protected Annotation createAnnotation(DocumentModel annotatedDocModel) {
        return createAnnotation(annotatedDocModel, ADMINISTRATOR, ANY_ANNOTATION_MESSAGE);
    }

    protected Annotation createAnnotation(DocumentModel annotatedDocModel, String author, String text) {
        Annotation annotation = new AnnotationImpl();
        annotation.setAuthor(author);
        annotation.setText(text);
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
