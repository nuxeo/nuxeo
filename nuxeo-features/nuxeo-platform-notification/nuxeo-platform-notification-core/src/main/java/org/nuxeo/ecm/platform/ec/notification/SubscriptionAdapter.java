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

public class SubscriptionAdapter {

    private static final String NOTIF_SUBSCRIBERSKEY = "subscribers";

    private static final String NOTIF_NAMEKEY = "name";

    private static final String NOTIF_PROPERTY = "notif:notifications";

    public static final String NOTIFIABLE_FACET = "Notifiable";

    private DocumentModel doc;

    public SubscriptionAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    private Map<String, Set<String>> getNotificationMap() {

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

    private void setNotificationMap(Map<String, Set<String>> map) {
        List<Map<String, Serializable>> props = new ArrayList<Map<String, Serializable>>();
        for (Entry<String, Set<String>> entry : map.entrySet()) {
            Map<String, Serializable> propMap = new HashMap<>();
            propMap.put(NOTIF_NAMEKEY, entry.getKey());
            propMap.put(NOTIF_SUBSCRIBERSKEY, new ArrayList<String>(entry.getValue()));
            props.add(propMap);
        }
        doc.setPropertyValue(NOTIF_PROPERTY, (Serializable) props);
    }

    /**
     * Return the list of subscribers name for a given notification.
     *
     * @param notification
     * @return
     * @since 7.3
     */
    @SuppressWarnings("unchecked")
    public List<String> getNotificationSubscribers(String notification) {
        Set<String> subscribers = getNotificationMap().get(notification);
        return subscribers != null ? new ArrayList<>(subscribers) : Collections.EMPTY_LIST;
    }

    /**
     * Return the list of of subscriptions for a given user
     *
     * @return
     * @since 7.3
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
     * @since 7.3
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
     * @since 7.3
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
     * @since 7.3
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
     * @since 7.3
     */
    public void copySubscriptionsTo(DocumentModel targetDoc) {
        targetDoc.setPropertyValue(NOTIF_PROPERTY, doc.getPropertyValue(NOTIF_PROPERTY));
    }

}
