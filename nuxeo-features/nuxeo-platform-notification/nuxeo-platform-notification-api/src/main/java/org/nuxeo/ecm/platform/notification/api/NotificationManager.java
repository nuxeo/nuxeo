/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: EJBPlacefulService.java 13110 2007-03-01 17:25:47Z rspivak $
 */
package org.nuxeo.ecm.platform.notification.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
public interface NotificationManager {

    /**
     * Gets the users that subscribed to a notification on a certain document.
     */
    List<String> getSubscribers(String notification, String docId)
            throws ClientException;

    /**
     * Gets the notifications for which a user subscribed for a certain
     * document.
     */
    List<String> getSubscriptionsForUserOnDocument(String username,
            String docId) throws ClassNotFoundException, ClientException;

    /**
     * Gets all users and groups that subscribed to a notification on a document
     * This is used in management of subscritptions.
     */
    List<String> getUsersSubscribedToNotificationOnDocument(
            String notification, String docId) throws ClientException;

    /**
     * Called when a user subscribes to a notification.
     */
    void addSubscription(String username, String notification,
            DocumentModel doc, Boolean sendConfirmationEmail,
            NuxeoPrincipal principal, String notificationName)
            throws ClientException;

    /**
     * Called when a user cancels his notification.
     */
    void removeSubscription(String username, String notification,
            String docId) throws ClientException;

    /**
     * Returns the notification manager.
     *
     * @deprecated should never have to return the registry : use delegation
     */
    @Deprecated
    NotificationRegistry getNotificationRegistry();

    /**
     * Returns a notification with all data loaded (label, etc).
     */
    Notification getNotificationByName(String selectedNotification);

    /**
     * Directly sends a notification to the principal, using the data provided
     * in the map
     * <p>
     * The map should contain at least the userName of the user calling the
     * method stored under the key "author".
     * <p>
     * infoMap should also contain all the variables that should be used to
     * make-up the body of the notifications message.
     *
     * @param notificationName name of notification
     * @param infoMap data used to compose the notification body
     * @param userPrincipal recipient used to get the adress(es) to send emails
     */
    void sendNotification(String notificationName, Map<String, Object> infoMap,
            String userPrincipal) throws ClientException;

    /**
     * Sends an e-mail directly.
     */
    void sendDocumentByMail(DocumentModel doc,
            String freemarkerTemplateName, String subject, String comment,
            NuxeoPrincipal sender, List<String> sendTo);

    List<Notification> getNotificationsForSubscriptions(String parentType);

    List<Notification> getNotificationsForEvents(String eventId);

    /**
     * Gets the list of event names used by notifications.
     *
     * @since 5.4.2
     */
    Set<String> getNotificationEventNames();

}
