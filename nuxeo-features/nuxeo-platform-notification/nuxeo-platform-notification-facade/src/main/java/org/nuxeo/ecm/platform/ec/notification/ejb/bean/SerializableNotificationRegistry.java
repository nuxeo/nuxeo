/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ec.notification.ejb.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.ec.notification.service.NotificationRegistryImpl;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;

public class SerializableNotificationRegistry implements Serializable,
        NotificationRegistry {

    private static final long serialVersionUID = 1L;

    private Map<String, List<Notification>> notificationRegistry = new HashMap<String, List<Notification>>();

    private List<Notification> notificationList = new ArrayList<Notification>();


    public SerializableNotificationRegistry(
            List<Notification> notificationList,
            Map<String, List<Notification>> notificationRegistry) {
        this.notificationList = notificationList;
        this.notificationRegistry = notificationRegistry;
    }

    public SerializableNotificationRegistry(NotificationRegistryImpl registry) {
        this.notificationList=registry.getNotifications();
        this.notificationRegistry=registry.getNotificationRegistry();
    }

    public void clear() {
        throw new IllegalStateException("This method is not availble via remote interface");
    }

    public List<Notification> getNotifications() {
        return notificationList;
    }

    public List<Notification> getNotificationsForEvent(String eventId) {
        if (notificationRegistry.get(eventId) == null) {
            notificationRegistry.put(eventId, new ArrayList<Notification>());
        }
        return notificationRegistry.get(eventId);
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

    public void registerNotification(Notification notif, List<String> events) {
        throw new IllegalStateException("This method is not availble via remote interface");
    }

    public void unregisterNotification(Notification notif, List<String> events) {
        throw new IllegalStateException("This method is not availble via remote interface");
    }

}
