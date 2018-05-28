/*
 * (C) Copyright 2007-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * This class holds data about the notifications.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
public interface NotificationRegistry extends Serializable {

    void clear();

    void registerNotification(Notification notif, List<String> events);

    /**
     * Unregister notification contribution and remove reference in event registry
     *
     * @since 5.6
     */
    void unregisterNotification(Notification notif);

    /**
     * Gets the list of event names used by notifications.
     *
     * @since 5.4.2
     */
    Set<String> getNotificationEventNames();

    List<Notification> getNotificationsForEvent(String eventId);

    List<Notification> getNotifications();

    List<Notification> getNotificationsForSubscriptions(String parentType);

}
