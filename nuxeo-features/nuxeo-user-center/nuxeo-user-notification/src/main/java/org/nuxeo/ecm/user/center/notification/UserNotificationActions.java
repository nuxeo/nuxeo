/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Martins
 */
package org.nuxeo.ecm.user.center.notification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ec.notification.SubscriptionAdapter;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.runtime.api.Framework;

@Name("userNotificationActions")
@Scope(ScopeType.CONVERSATION)
public class UserNotificationActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected NuxeoPrincipal currentUser;

    @In(create = true)
    protected CoreSession documentManager;

    @In(create = true)
    protected transient NotificationManager notificationManager;

    private List<UserSubscription> subscriptions;

    @Factory(value = "userSubscriptions", scope = ScopeType.EVENT)
    public List<UserSubscription> getUserSubscriptions() {

        List<UserSubscription> result = new ArrayList<>();

        String prefixedUserName = NuxeoPrincipal.PREFIX + currentUser.getName();
        result.addAll(fetchSubscriptionsFor(prefixedUserName));

        for (String group : currentUser.getAllGroups()) {
            String prefixedgroupName = NuxeoGroup.PREFIX + group;
            result.addAll(fetchSubscriptionsFor(prefixedgroupName));
        }

        reorderSubscriptions(result);

        return subscriptions;
    }

    private List<UserSubscription> fetchSubscriptionsFor(String prefixedUserName) {
        List<UserSubscription> result = new ArrayList<>();
        NotificationManager nm = Framework.getService(NotificationManager.class);
        List<DocumentModel> subscribedDocs = nm.getSubscribedDocuments(prefixedUserName);
        for (DocumentModel doc : subscribedDocs) {
            // Avoid treating document the current user can't read
            if (documentManager.exists(doc.getRef())) {
                SubscriptionAdapter sa = doc.getAdapter(SubscriptionAdapter.class);
                List<String> notifications = sa.getUserSubscriptions(prefixedUserName);
                for (String notification : notifications) {
                    result.add(new UserSubscription(doc.getId(), notification, prefixedUserName));
                }
            }
        }
        return result;
    }

    private void reorderSubscriptions(List<UserSubscription> allSubscriptions) {
        Map<String, List<UserSubscription>> unsortedSubscriptions = new HashMap<String, List<UserSubscription>>();
        for (Object obj : allSubscriptions) {
            UserSubscription us = (UserSubscription) obj;
            DocumentModel doc = getDocument(us.getDocId());
            String path;
            if (doc == null) {
                path = us.getDocId();
            } else {
                path = getDocument(us.getDocId()).getPathAsString();
            }
            if (!unsortedSubscriptions.containsKey(path)) {
                unsortedSubscriptions.put(path, new ArrayList<UserSubscription>());
            }
            unsortedSubscriptions.get(path).add(us);
        }
        SortedSet<String> sortedset = new TreeSet<String>(unsortedSubscriptions.keySet());
        subscriptions = new ArrayList<UserSubscription>();
        Iterator<String> it = sortedset.iterator();
        while (it.hasNext()) {
            subscriptions.addAll(unsortedSubscriptions.get(it.next()));
        }
    }

    public DocumentModel getDocument(String docId) {
        // test if user has READ right
        DocumentRef ref = new IdRef(docId);
        if (documentManager.exists(ref)) {
            return documentManager.getDocument(ref);
        }
        return null;
    }

    public boolean getCanRemoveNotification(String userId) {
        // Do not allow removing for group subscription
        if (userId != null && userId.equals(NuxeoPrincipal.PREFIX + currentUser.getName())) {
            return true;
        }
        return false;
    }

}
