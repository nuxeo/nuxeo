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
 *     Narcis Paslaru
 *     Thierry Martins
 */
package org.nuxeo.ecm.platform.ec.notification.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;

/**
 * Registry with extra API for notification configurations retrieval.
 */
public class NotificationRegistryImpl extends MapRegistry<Notification> implements NotificationRegistry {

    // maps EventId to a list of notifications
    protected final Map<String, List<Notification>> notificationsByEvent = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
        super.initialize();
        notificationsByEvent.clear();
        contributions.entrySet()
                     .stream()
                     .filter(x -> !disabled.contains(x.getKey()))
                     .map(Map.Entry::getValue)
                     .map(NotificationDescriptor.class::cast)
                     .forEach(
                             n -> n.getEvents()
                                   .forEach(event -> notificationsByEvent.computeIfAbsent(event, k -> new ArrayList<>())
                                                                         .add(n)));
    }

    @Override
    public Set<String> getNotificationEventNames() {
        return notificationsByEvent.entrySet()
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
        if (eventId == null) {
            return Collections.emptyList();
        }
        checkInitialized();
        return notificationsByEvent.computeIfAbsent(eventId, k -> new ArrayList<>());
    }

    @Override
    public List<Notification> getNotifications() {
        return getContributionValues();
    }

    @Override
    public List<Notification> getNotificationsForSubscriptions(String parentType) {
        List<Notification> result = new ArrayList<>();
        for (Notification notification : getNotifications()) {
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
