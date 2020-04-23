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
import static org.nuxeo.ecm.platform.comment.CommentUtils.createUser;

import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;

/**
 * @since 11.1
 */
public class TestTreeCommentNotification extends AbstractTestCommentNotification {

    @Override
    protected Class<? extends CommentManager> getType() {
        return TreeCommentManager.class;
    }

    @Test
    public void testAutoSubscribingOnlyOnceToNewComments() {
        String john = "john";
        String johnSubscription = NotificationConstants.USER_PREFIX + john;
        createUser(john);
        List<String> subscriptions = notificationService.getSubscriptionsForUserOnDocument(johnSubscription,
                commentedDocumentModel);
        assertEquals(0, subscriptions.size());
        createComment(commentedDocumentModel, john, "Test message");
        transactionalFeature.nextTransaction();
        commentedDocumentModel.refresh();
        subscriptions = notificationService.getSubscriptionsForUserOnDocument(johnSubscription, commentedDocumentModel);
        assertTrue(subscriptions.contains("CommentAdded"));
        assertTrue(subscriptions.contains("CommentUpdated"));
        for (String subscription : subscriptions) {
            notificationService.removeSubscription(johnSubscription, subscription, commentedDocumentModel);
        }
        createComment(commentedDocumentModel, john, "Test message again");
        transactionalFeature.nextTransaction();
        subscriptions = notificationService.getSubscriptionsForUserOnDocument(johnSubscription, commentedDocumentModel);
        assertTrue(subscriptions.isEmpty());
    }

    protected Comment createComment(DocumentModel commentedDocModel, String author, String text) {
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(commentedDocModel.getId());

        return commentManager.createComment(session, comment);
    }

}
