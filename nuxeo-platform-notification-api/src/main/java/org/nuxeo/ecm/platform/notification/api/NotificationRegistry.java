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

package org.nuxeo.ecm.platform.notification.api;

import java.util.List;

/**
 * This class holds data about the notifications.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
public interface NotificationRegistry {

    void clear();

    void registerNotification(Notification notif, List<String> events);

    void unregisterNotification(Notification notif, List<String> events);

    List<Notification> getNotificationsForEvent(String eventId);

    List<Notification> getNotifications();

    List<Notification> getNotificationsForSubscriptions(String parentType);
}
