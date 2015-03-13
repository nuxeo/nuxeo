package org.nuxeo.ecm.platform.ec.notification;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.runtime.api.Framework;

public class SubscriptionAdapter {



    private DocumentModel doc;

    public SubscriptionAdapter(DocumentModel doc) {
        this.doc = doc;
    }
    /**
     * Return the list of subscribers name for a given notification.
     * @param notification
     * @return
     * @since 7.3
     */
    public List<String> getNotificationSubscribers(String notification) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    /**
     * Return the list of of subscriptions for a given user
     * @return
     * @since 7.3
     */
    public List<String> getUserSubscriptions(String username) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    /**
     * Add a subscription to a notification for a given user.
     * @param username
     * @param notification
     * @since 7.3
     */
    public void addSubscription(String username, String notification) {
        // TODO Auto-generated method stub
        //
        throw new UnsupportedOperationException();
    }

    /**
     * Add a subscription to all notification for a given user
     * @param username
     * @since 7.3
     */
    public void addSubscriptionsToAll(String username) {



        Set<String> notificationNames = new HashSet<String>();

        List<Notification> notifications = Framework.getLocalService(NotificationService.class).getNotificationRegistry().getNotifications();
        for (Notification notification : notifications) {
            // Do not subscribe to auto-subscribed notification
            if (notification.getAutoSubscribed()) {
                continue;
            }
            if (!notificationNames.contains(notification.getName())) {
                // Check if notification is available for the current document
                String availableIn = notification.getAvailableIn();
                String[] types = availableIn.replace(",", " ").split(" ");
                if (availableIn == null || "all".equals(availableIn) || "*".equals(availableIn)
                        || Arrays.asList(types).contains(doc.getType())) {
                    notificationNames.add(notification.getName());
                    continue;
                }
                CoreSession session = doc.getCoreSession();
                if (session != null) {
                    for (DocumentModel parent : session.getParentDocuments(doc.getRef())) {
                        if (Arrays.asList(types).contains(parent.getType())) {
                            notificationNames.add(notification.getName());
                            continue;
                        }
                    }
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
     * @param username
     * @param notification
     * @since 7.3
     */
    public void removeUserNotificationSubscription(String username, String notification) {
        // TODO Auto-generated method stub
        //
        throw new UnsupportedOperationException();
    }

    /**
     * Copy the subscriptions of the current doc to the targetted document.
     * @param targetDoc
     * @since 7.3
     */
    public void copySubscriptionsTo(DocumentModel targetDoc) {
        // TODO Auto-generated method stub
        //
        throw new UnsupportedOperationException();
    }


}
