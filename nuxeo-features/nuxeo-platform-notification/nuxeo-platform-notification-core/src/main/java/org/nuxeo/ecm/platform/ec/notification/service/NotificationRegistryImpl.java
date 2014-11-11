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
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.platform.ec.notification.NotificationImpl;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
public class NotificationRegistryImpl implements NotificationRegistry {

    private static final long serialVersionUID = 1L;

    // maps Notifications to Strings that are names of the objects
    private final Map<String, List<Notification>> notificationRegistry = new HashMap<String, List<Notification>>();

    private final List<Notification> notificationList = new ArrayList<Notification>();

    public void clear() {
        notificationRegistry.clear();
    }

    public void registerNotification(Notification notif, List<String> events) {
        Notification notification = new NotificationImpl(notif.getName(),
                notif.getTemplate(), notif.getChannel(), notif.getSubjectTemplate(), notif.getAutoSubscribed(),
                 notif.getSubject(), notif.getAvailableIn(), notif.getLabel());
        if (notif.getEnabled()) {
            notificationList.add(notification);
            if (events != null && !events.isEmpty()) {
                for (String event : events) {
                    getNotificationsForEvent(event).add(notification);
                }
            }
        } else {
            unregisterNotification(notif, events);
        }
    }

    public void unregisterNotification(Notification notif, List<String> events) {
        NotificationImpl notification = new NotificationImpl(notif.getName(),
                notif.getTemplate(), notif.getChannel(), notif.getSubjectTemplate(),
                notif.getAutoSubscribed(), notif.getSubject(), notif.getAvailableIn(), notif.getLabel());
        notificationList.remove(notification);
        if (events != null && !events.isEmpty()) {
            for (String event : events) {
                getNotificationsForEvent(event).remove(notification);
            }
        }
    }

    @Override
    public Set<String> getNotificationEventNames() {
        Set<String> ret = new HashSet<String>();
        for (String name : notificationRegistry.keySet()) {
            if (! notificationRegistry.get(name).isEmpty()) {
                ret.add(name);
            }
        }
        return ret;
    }

    /**
     * Gets the list of possible notifications for an event.
     */
    public List<Notification> getNotificationsForEvent(String eventId) {
        if (notificationRegistry.get(eventId) == null) {
            notificationRegistry.put(eventId, new ArrayList<Notification>());
        }

        return notificationRegistry.get(eventId);
    }

    public List<Notification> getNotifications() {
        return notificationList;
    }

    public Map<String, List<Notification>> getNotificationRegistry() {
        return notificationRegistry;
    }

    public List<Notification> getNotificationsForSubscriptions(String parentType) {
        List<Notification> result = new ArrayList<Notification>();
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
