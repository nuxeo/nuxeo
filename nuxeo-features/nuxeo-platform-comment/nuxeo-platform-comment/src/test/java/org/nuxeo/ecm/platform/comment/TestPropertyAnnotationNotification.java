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
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;

/**
 * @since 11.1
 */
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
            DocumentModel annotationDocumentModel = session.getDocument(new IdRef(annotation.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) annotationDocumentModel.getPropertyValue(COMMENT_PARENT_ID)));
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, annotationDocumentModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
        }
    }

    @Test
    @Override
    public void shouldNotifyEventWhenRemoveAnnotation() {
        Annotation annotation = createAnnotation(annotatedDocumentModel);
        DocumentModel annotationDocModel = session.getDocument(new IdRef(annotation.getId()));
        annotationDocModel.detach(true);
        transactionalFeature.nextTransaction();
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_REMOVED)) {
            annotationService.deleteAnnotation(session, annotation.getId());
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) annotationDocModel.getPropertyValue(COMMENT_PARENT_ID)));
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, annotationDocModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
        }
    }

    @Test
    @Override
    public void shouldNotifyWithTheRightAnnotatedDocument() {
        // First comment
        Annotation annotation = createAnnotation(annotatedDocumentModel);
        DocumentModel annotationDocModel = session.getDocument(new IdRef(annotation.getId()));
        // before subscribing, or previous event will be notified as well
        transactionalFeature.nextTransaction();
        // Reply
        addSubscriptions("CommentAdded");
        try (CapturingEventListener listener = new CapturingEventListener(COMMENT_ADDED)) {
            Comment reply = createAnnotation(annotationDocModel);
            DocumentModel replyDocumentModel = session.getDocument(new IdRef(reply.getId()));
            DocumentModel annotationParentDocumentModel = session.getDocument(
                    new IdRef((String) replyDocumentModel.getPropertyValue(COMMENT_PARENT_ID)));
            transactionalFeature.nextTransaction();

            Event expectedEvent = listener.streamCapturedEvents()
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("Event wasn't fired"));
            checkDocumentEventContext(expectedEvent, replyDocumentModel, annotationParentDocumentModel,
                    annotatedDocumentModel);
        }
    }
}
