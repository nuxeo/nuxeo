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
import static org.nuxeo.ecm.platform.comment.CommentUtils.createUser;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newAnnotation;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * This isn't present on 11.1.
 * 
 * @since 10.10-HF26
 */
@RunWith(FeaturesRunner.class)
@Features({ NotificationCommentFeature.class, TreeCommentFeature.class })
public class TestTreeAnnotationNotification {

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected AnnotationService annotationService;

    @Inject
    protected NotificationManager notificationManager;

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
