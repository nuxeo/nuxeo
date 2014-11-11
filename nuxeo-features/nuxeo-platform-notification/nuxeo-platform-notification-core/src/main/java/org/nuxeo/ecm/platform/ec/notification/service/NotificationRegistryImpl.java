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

    // maps Notifications to Strings that are names of the objects
    private final Map<String, List<Notification>> notificationRegistry = new HashMap<String, List<Notification>>();

    private final List<Notification> notificationList = new ArrayList<Notification>();

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
            NotificationImpl notification = new NotificationImpl(
                    notif.getName(), notif.getTemplate(), notif.getChannel(),
                    notif.getSubjectTemplate(), notif.getAutoSubscribed(),
                    notif.getSubject(), notif.getAvailableIn(),
                    notif.getLabel());

            if (notif.getTemplateExpr() != null) {
                notification.setTemplateExpr(notif.getTemplateExpr());
            }

            if (notificationList.contains(notification)) {
                unregisterNotification(notification, events);
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
            unregisterNotification(notif, events);
        }
    }

    @Override
    @Deprecated
    /**
     * Please use unregisterNotification(Notification notif) instead.
     * Deprecated since 5.7.2
     */
    public void unregisterNotification(Notification notif, List<String> events) {
        unregisterNotification(notif);
    }

    @Override
    public void unregisterNotification(Notification notif) {
        if (notif == null) {
            log.warn("Try to unregister a null notification, do nothing");
            return;
        }

        NotificationImpl notification = new NotificationImpl(notif.getName(),
                notif.getTemplate(), notif.getChannel(),
                notif.getSubjectTemplate(), notif.getAutoSubscribed(),
                notif.getSubject(), notif.getAvailableIn(), notif.getLabel());

        if (notificationList.contains(notification)) {
            notificationList.remove(notification);
        }

        for (String event : notificationRegistry.keySet()) {
            for (int i = notificationRegistry.get(event).size() - 1; i >= 0; i--) {
                List<Notification> regNotifs = notificationRegistry.get(event);
                if (regNotifs.contains(notification)) {
                    regNotifs.remove(notification);
                }
            }
        }
    }

    @Override
    public Set<String> getNotificationEventNames() {
        Set<String> ret = new HashSet<String>();
        for (String name : notificationRegistry.keySet()) {
            if (!notificationRegistry.get(name).isEmpty()) {
                ret.add(name);
            }
        }
        return ret;
    }

    /**
     * Gets the list of possible notifications for an event.
     */
    @Override
    public List<Notification> getNotificationsForEvent(String eventId) {
        if (notificationRegistry.get(eventId) == null) {
            notificationRegistry.put(eventId, new ArrayList<Notification>());
        }

        return notificationRegistry.get(eventId);
    }

    @Override
    public List<Notification> getNotifications() {
        return notificationList;
    }

    public Map<String, List<Notification>> getNotificationRegistry() {
        return notificationRegistry;
    }

    @Override
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
