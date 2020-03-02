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

import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;

/**
 * @since 11.1
 */
public class TestTreeAnnotationNotification extends AbstractTestAnnotationNotification {

    @Override
    protected Class<? extends CommentManager> getType() {
        return TreeCommentManager.class;
    }

    @Test
    public void testAutoSubscribingOnlyOnceToNewAnnotations() {
        String john = "john";
        String johnSubscription = NotificationConstants.USER_PREFIX + john;
        createUser(john);
        List<String> subscriptions = notificationService.getSubscriptionsForUserOnDocument(johnSubscription,
                annotatedDocumentModel);
        assertEquals(0, subscriptions.size());
        createAnnotation(annotatedDocumentModel, john, "Test message");
        transactionalFeature.nextTransaction();
        annotatedDocumentModel = session.getDocument(annotatedDocumentModel.getRef());
        subscriptions = notificationService.getSubscriptionsForUserOnDocument(johnSubscription, annotatedDocumentModel);
        assertTrue(subscriptions.contains("CommentAdded"));
        assertTrue(subscriptions.contains("CommentUpdated"));
        for (String subscription : subscriptions) {
            notificationService.removeSubscription(johnSubscription, subscription, annotatedDocumentModel);
        }
        createAnnotation(annotatedDocumentModel, john, "Test message again");
        transactionalFeature.nextTransaction();
        subscriptions = notificationService.getSubscriptionsForUserOnDocument(johnSubscription, annotatedDocumentModel);
        assertTrue(subscriptions.isEmpty());
    }

}
