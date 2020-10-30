/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.platform.ec.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class UserSubscriptionAdapterTest {

    @Inject
    private CoreSession session;

    private DocumentModel doc;

    @Before
    public void doBefore() {
        // Given a document
        doc = session.createDocumentModel("/", "testDoc", "Note");
        doc = session.createDocument(doc);

    }

    @Test
    public void aDocumentMayHaveAUserSubscriptionAdapter() {

        // I can get a user subscription adapter on it
        SubscriptionAdapter us = doc.getAdapter(SubscriptionAdapter.class);
        assertNotNull("It should be able to get a UserSubscription adapter", us);

        // To set and get subscriptions
        us.addSubscription("Administrator", "timetoeat");
        us.addSubscription("toto", "timetosleep");

        assertThat(us.getNotificationSubscribers("timetoeat")).contains("Administrator");
        assertThat(us.getNotificationSubscribers("timetoslepp")).doesNotContain("Administrator");

    }

    @Test
    public void itCanRetrieveTheSubscriptionsOfAUserOnADocument() {

        SubscriptionAdapter us = doc.getAdapter(SubscriptionAdapter.class);
        // To set and get subscriptions
        us.addSubscription("Administrator", "timetoeat");
        // To set and get subscriptions
        us.addSubscription("toto", "timetosleep");

        assertThat(us.getUserSubscriptions("Administrator")).contains("timetoeat");
        assertThat(us.getUserSubscriptions("Administrator")).doesNotContain("timetosleep");

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itCanUnsubscribeFromANotification() {
        SubscriptionAdapter us = doc.getAdapter(SubscriptionAdapter.class);
        // To set and get subscriptions
        us.addSubscription("Administrator", "timetoeat");
        us.addSubscription("Administrator", "timetosleep");
        assertThat(us.getUserSubscriptions("Administrator")).contains("timetoeat", "timetosleep");

        us.removeUserNotificationSubscription("Administrator", "timetoeat");
        assertThat(us.getUserSubscriptions("Administrator")).contains("timetosleep");
        assertThat(us.getUserSubscriptions("Administrator")).doesNotContain("timetoeat");
        var notifications = (List<Map<String, Serializable>>) doc.getPropertyValue("notif:notifications");
        assertEquals(1, notifications.size());

        us.removeUserNotificationSubscription("Administrator", "timetosleep");
        assertThat(us.getUserSubscriptions("Administrator")).doesNotContain("timetosleep");
        notifications = (List<Map<String, Serializable>>) doc.getPropertyValue("notif:notifications");
        assertTrue(notifications.isEmpty());

    }

    @Test
    public void itCanCopySubscriptionsFromADocModelToAnother() {
        // Given a second target document
        DocumentModel targetDoc = session.createDocumentModel("/", "testDoc2", "Note");
        targetDoc = session.createDocument(targetDoc);

        SubscriptionAdapter us = doc.getAdapter(SubscriptionAdapter.class);
        SubscriptionAdapter targetUs = targetDoc.getAdapter(SubscriptionAdapter.class);

        us.addSubscription("Administrator", "timetoeat");
        assertThat(us.getUserSubscriptions("Administrator")).contains("timetoeat");
        assertThat(targetUs.getUserSubscriptions("Administrator")).doesNotContain("timetoeat");

        us.copySubscriptionsTo(targetDoc);

        assertThat(targetUs.getUserSubscriptions("Administrator")).contains("timetoeat");

    }

    @Test
    public void itCanSubscriptToAllNotifications() {
        SubscriptionAdapter us = doc.getAdapter(SubscriptionAdapter.class);
        us.addSubscriptionsToAll("Administrator");

        assertThat(us.getUserSubscriptions("Administration")).hasSize(0);

        session.createDocument(session.createDocumentModel("/", "workspace", "Workspace"));
        DocumentModel doc = session.createDocument(session.createDocumentModel("/workspace", "subscribablenote",
                "Workspace"));

        us = doc.getAdapter(SubscriptionAdapter.class);
        us.addSubscriptionsToAll("Administrator");
        assertThat(us.getUserSubscriptions("Administrator")).contains("Modification", "Creation");

    }

}
