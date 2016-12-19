/*
 * (C) Copyright 2007-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.notification.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
public interface NotificationManager {

    /**
     * Gets the users that subscribed to a notification on a certain document.
     *
     * @deprecated since 7.3 use {@link #getSubscribers(String, DocumentModel)}
     */
    @Deprecated
    List<String> getSubscribers(String notification, String docId);

    /**
     * Gets the users that subscribed to a notification on a certain document.
     */
    List<String> getSubscribers(String notification, DocumentModel doc);

    /**
     * Gets the notifications for which a user subscribed for a certain document.
     *
     * @deprecated since 7.3 use {@link #getSubscriptionsForUserOnDocument(String, DocumentModel)}
     */
    @Deprecated
    List<String> getSubscriptionsForUserOnDocument(String username, String docId);

    /**
     * Gets the notifications for which a user subscribed for a certain document.
     */
    List<String> getSubscriptionsForUserOnDocument(String username, DocumentModel doc);

    /**
     * @deprecated since 7.3 use {@link #getUsersSubscribedToNotificationOnDocument(String, DocumentModel)}
     */
    @Deprecated
    List<String> getUsersSubscribedToNotificationOnDocument(String notification, String docId);

    /**
     * Gets all users and groups that subscribed to a notification on a document This is used in management of
     * subscritptions.
     */
    List<String> getUsersSubscribedToNotificationOnDocument(String notification, DocumentModel doc);

    /**
     * Called when a user subscribes to a notification.
     */
    void addSubscription(String username, String notification, DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal, String notificationName);

    /**
     * @since 5.6 Called when a user subscribes to all notifications.
     */
    void addSubscriptions(String username, DocumentModel doc, Boolean sendConfirmationEmail, NuxeoPrincipal principal);

    /**
     * @since 5.6 Called when a user unsubscribes to all notifications.
     * @deprecated since 7.3 use {@link #removeSubscriptions(String, List, DocumentModel)}
     */
    void removeSubscriptions(String username, List<String> notifications, String docId);

    /**
     * @since 5.6 Called when a user unsubscribes to all notifications.
     */
    void removeSubscriptions(String username, List<String> notifications, DocumentModel doc);

    /**
     * Called when a user cancels his notification.
     *
     * @deprecated since 7.3 use {@link #removeSubscription(String, String, DocumentModel)}
     */
    void removeSubscription(String username, String notification, String docId);

    /**
     * Called when a user cancels his notification.
     */
    void removeSubscription(String username, String notification, DocumentModel doc);

    /**
     * Returns a notification with all data loaded (label, etc).
     */
    Notification getNotificationByName(String selectedNotification);

    /**
     * Directly sends a notification to the principal, using the data provided in the map
     * <p>
     * The map should contain at least the userName of the user calling the method stored under the key "author".
     * <p>
     * infoMap should also contain all the variables that should be used to make-up the body of the notifications
     * message.
     *
     * @param notificationName name of notification
     * @param infoMap data used to compose the notification body
     * @param userPrincipal recipient used to get the adress(es) to send emails
     */
    void sendNotification(String notificationName, Map<String, Object> infoMap, String userPrincipal);

    /**
     * Sends an e-mail directly.
     */
    void sendDocumentByMail(DocumentModel doc, String freemarkerTemplateName, String subject, String comment,
            NuxeoPrincipal sender, List<String> sendTo);

    List<Notification> getNotificationsForSubscriptions(String parentType);

    List<Notification> getNotificationsForEvents(String eventId);

    /**
     * Gets the list of event names used by notifications.
     *
     * @since 5.4.2
     */
    Set<String> getNotificationEventNames();

    /**
     * Returns the list of live docs the user is subscribed to.
     *
     * @since 7.3
     */
    List<DocumentModel> getSubscribedDocuments(String prefixedPrincipalName);

}
