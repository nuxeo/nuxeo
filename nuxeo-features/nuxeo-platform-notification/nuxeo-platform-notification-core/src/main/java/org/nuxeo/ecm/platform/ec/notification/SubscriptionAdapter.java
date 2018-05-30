/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.platform.ec.notification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Encapsulates all notification storage logic in the Notifiable facet.
 *
 * @since 7.3
 */
public class SubscriptionAdapter {

    public static final String NOTIFIABLE_FACET = "Notifiable";

    private static final String NOTIF_PROPERTY = "notif:notifications";

    private static final String NOTIF_SUBSCRIBERSKEY = "subscribers";

    private static final String NOTIF_NAMEKEY = "name";

    private DocumentModel doc;

    public SubscriptionAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    /**
     * Take the document storage propery and put it in a map.
     * <dl>
     * <dt>key</dt>
     * <dd>notificationName</dd>
     * <dt>value</dt>
     * <dd>list of subscribers</dd>
     * </dl>
     * After having modified the map, update the doc with {@link #setNotificationMap(Map)}
     *
     * @return
     */
    private Map<String, Set<String>> getNotificationMap() {

        if (!doc.hasFacet(SubscriptionAdapter.NOTIFIABLE_FACET)) {
            return new HashMap<>();
        }

        Map<String, Set<String>> result = new HashMap<String, Set<String>>();

        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> props = (List<Map<String, Serializable>>) doc.getPropertyValue(NOTIF_PROPERTY);
        for (Map<String, Serializable> prop : props) {
            String notificationName = (String) prop.get(NOTIF_NAMEKEY);
            String[] subscribers = (String[]) prop.get(NOTIF_SUBSCRIBERSKEY);

            if (subscribers != null && subscribers.length > 0) {
                if (!result.containsKey(notificationName)) {
                    Set<String> subscribersSet = new HashSet<String>();
                    result.put(notificationName, subscribersSet);
                }
                result.get(notificationName).addAll(Arrays.asList(subscribers));
            }
        }
        return result;

    }

    /**
     * Take a map and store it in the document's notification property. To get the original map, use
     * {@link #getNotificationMap()}
     */
    private void setNotificationMap(Map<String, Set<String>> map) {
        List<Map<String, Serializable>> props = new ArrayList<Map<String, Serializable>>();
        for (Entry<String, Set<String>> entry : map.entrySet()) {
            Set<String> subscribers = entry.getValue();
            if (!subscribers.isEmpty()) {
                Map<String, Serializable> propMap = new HashMap<>();
                propMap.put(NOTIF_NAMEKEY, entry.getKey());
                propMap.put(NOTIF_SUBSCRIBERSKEY, new ArrayList<String>(subscribers));
                props.add(propMap);
            }
        }

        if (!props.isEmpty()) {
            if (!doc.hasFacet(SubscriptionAdapter.NOTIFIABLE_FACET)) {
                doc.addFacet(SubscriptionAdapter.NOTIFIABLE_FACET);
            }
        }

        doc.setPropertyValue(NOTIF_PROPERTY, (Serializable) props);
    }

    /**
     * Clears the notification in the document's property.
     *
     * @since 10.2
     */
    protected void clearNotification() {
        doc.setPropertyValue(NOTIF_PROPERTY, null);
    }

    /**
     * Return the list of subscribers name for a given notification.
     *
     * @param notification
     * @return
     */
    public List<String> getNotificationSubscribers(String notification) {
        Set<String> subscribers = getNotificationMap().get(notification);
        return subscribers != null ? new ArrayList<>(subscribers) : Collections.emptyList();
    }

    /**
     * Return the list of of subscriptions for a given user
     *
     * @return
     */
    public List<String> getUserSubscriptions(String username) {
        List<String> result = new ArrayList<String>();
        for (Entry<String, Set<String>> entry : getNotificationMap().entrySet()) {
            if (entry.getValue().contains(username)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Add a subscription to a notification for a given user.
     *
     * @param username
     * @param notification
     */
    public void addSubscription(String username, String notification) {
        Map<String, Set<String>> notificationMap = getNotificationMap();
        if (!notificationMap.containsKey(notification)) {
            notificationMap.put(notification, new HashSet<>());
        }
        notificationMap.get(notification).add(username);
        setNotificationMap(notificationMap);
    }

    /**
     * Add a subscription to all notification for a given user
     *
     * @param username
     */
    public void addSubscriptionsToAll(String username) {

        Set<String> notificationNames = new HashSet<String>();

        NotificationManager ns = Framework.getLocalService(NotificationManager.class);

        for (Notification notif : ns.getNotificationsForSubscriptions(doc.getType())) {
            notificationNames.add(notif.getName());
        }

        CoreSession session = doc.getCoreSession();

        if (session != null) {
            for (DocumentModel parent : session.getParentDocuments(doc.getRef())) {
                for (Notification notif : ns.getNotificationsForSubscriptions(parent.getType())) {
                    notificationNames.add(notif.getName());
                }
            }
        }

        // add subscriptions to every relevant notification
        for (String name : notificationNames) {
            addSubscription(username, name);
        }

    }

    /**
     * Remove a subscription to a notification for a given user.
     *
     * @param username
     * @param notification
     */
    public void removeUserNotificationSubscription(String username, String notification) {
        Map<String, Set<String>> map = getNotificationMap();
        if (map.containsKey(notification)) {
            map.get(notification).remove(username);
        }
        setNotificationMap(map);
    }

    /**
     * Copy the subscriptions of the current doc to the targetted document.
     *
     * @param targetDoc
     */
    public void copySubscriptionsTo(DocumentModel targetDoc) {
        if (!targetDoc.hasFacet(NOTIFIABLE_FACET)) {
            targetDoc.addFacet(NOTIFIABLE_FACET);
        }
        targetDoc.setPropertyValue(NOTIF_PROPERTY, doc.getPropertyValue(NOTIF_PROPERTY));
    }

}
