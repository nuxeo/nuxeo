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
package org.nuxeo.ecm.platform.ec.notification.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ec.notification.NotificationImpl;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
public class NotificationRegistryImpl implements NotificationRegistry {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(NotificationRegistryImpl.class);

    // maps EventId to a list of notifications
    private final Map<String, List<Notification>> notificationRegistry = new HashMap<>();

    private final List<Notification> notificationList = new ArrayList<>();

    @Override
    public void clear() {
        notificationRegistry.clear();
    }

    @Override
    public void registerNotification(Notification notif, List<String> events) {
        if (notif.getName() == null) {
            log.error("Notifications contributions must have a name");
        }

        if (notif.getEnabled()) {
            NotificationImpl notification = new NotificationImpl(notif.getName(), notif.getTemplate(),
                    notif.getChannel(), notif.getSubjectTemplate(), notif.getAutoSubscribed(), notif.getSubject(),
                    notif.getAvailableIn(), notif.getLabel());

            if (notif.getTemplateExpr() != null) {
                notification.setTemplateExpr(notif.getTemplateExpr());
            }

            if (notificationList.contains(notification)) {
                unregisterNotification(notification);
            }
            notificationList.add(notification);

            if (events != null && !events.isEmpty()) {
                for (String event : events) {
                    List<Notification> regNotifs = getNotificationsForEvent(event);
                    if (!regNotifs.contains(notification)) {
                        regNotifs.add(notification);
                    }
                }
            }
        } else {
            unregisterNotification(notif);
        }
    }

    @Override
    public void unregisterNotification(Notification notif) {
        if (notif == null) {
            log.warn("Try to unregister a null notification, do nothing");
            return;
        }

        NotificationImpl notification = new NotificationImpl(notif.getName(), notif.getTemplate(), notif.getChannel(),
                notif.getSubjectTemplate(), notif.getAutoSubscribed(), notif.getSubject(), notif.getAvailableIn(),
                notif.getLabel());

        notificationList.remove(notification);
        notificationRegistry.values().forEach(notifications -> notifications.remove(notification));
    }

    @Override
    public Set<String> getNotificationEventNames() {
        return notificationRegistry.entrySet()
                                   .stream()
                                   .filter(entry -> !entry.getValue().isEmpty())
                                   .map(Entry::getKey)
                                   .collect(Collectors.toSet());
    }

    /**
     * Gets the list of possible notifications for an event.
     */
    @Override
    public List<Notification> getNotificationsForEvent(String eventId) {
        return notificationRegistry.computeIfAbsent(eventId, k -> new ArrayList<>());
    }

    @Override
    public List<Notification> getNotifications() {
        return notificationList;
    }

    /**
     * @deprecated since 10.2, seems unused
     */
    @Deprecated
    public Map<String, List<Notification>> getNotificationRegistry() {
        return notificationRegistry;
    }

    @Override
    public List<Notification> getNotificationsForSubscriptions(String parentType) {
        List<Notification> result = new ArrayList<>();
        for (Notification notification : notificationList) {
            if (notification.getAutoSubscribed()) {
                continue;
            }
            String type = notification.getAvailableIn();
            if (type == null || "all".equals(type) || "*".equals(type)) {
                result.add(notification);
            } else {
                String[] types = type.replace(",", " ").split(" ");
                if (Arrays.asList(types).contains(parentType)) {
                    result.add(notification);
                }
            }
        }
        return result;
    }

}
