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
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;

/**
 * @since 11.1
 */
public class TestTreeCommentNotification extends AbstractTestCommentNotification {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected NotificationManager notificationManager;

    public TestTreeCommentNotification() {
        super(TreeCommentManager.class);
    }

    @Test
    public void testAutoSubscribingOnlyOnceToNewComments() {
        String john = "john";
        String johnSubscription = NotificationConstants.USER_PREFIX + john;
        createUser(john);
        // give permission to comment to john
        ACP acp = commentedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("john", SecurityConstants.READ, true));
        session.setACP(commentedDocModel.getRef(), acp, false);
        session.save();

        CoreSession johnSession = coreFeature.getCoreSession("john");

        // check no subscriptions
        List<String> subscriptions = notificationManager.getSubscriptionsForUserOnDocument(johnSubscription,
                commentedDocModel);
        assertEquals(0, subscriptions.size());

        // create a comment and check auto subscriptions
        commentManager.createComment(johnSession, newComment(commentedDocModel.getId(), "Test message"));
        // wait for auto subscriptions
        transactionalFeature.nextTransaction();
        commentedDocModel.refresh();
        subscriptions = notificationManager.getSubscriptionsForUserOnDocument(johnSubscription, commentedDocModel);
        assertTrue(subscriptions.contains("CommentAdded"));
        assertTrue(subscriptions.contains("CommentUpdated"));

        // clear subscriptions
        for (String subscription : subscriptions) {
            notificationManager.removeSubscription(johnSubscription, subscription, commentedDocModel);
        }

        // create a document and check auto subscribe doesn't happen
        commentManager.createComment(johnSession, newComment(commentedDocModel.getId(), "Test message again"));
        transactionalFeature.nextTransaction();
        subscriptions = notificationManager.getSubscriptionsForUserOnDocument(johnSubscription, commentedDocModel);
        assertTrue(subscriptions.isEmpty());
    }

}
