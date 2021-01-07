/*
 * (C) Copyright 2007-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Narcis Paslaru
 */
package org.nuxeo.ecm.platform.notification.api;

import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.registry.Registry;

/**
 * Notification descriptors registry API.
 */
public interface NotificationRegistry extends Registry {

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
