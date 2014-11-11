/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.ec.notification.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;

/**
 * @since 5.6
 * @author Thierry Martins <tm@nuxeo.com>
 *
 */
@XObject("veto")
public class NotificationListenerVetoDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@class")
    private Class<? extends NotificationListenerVeto> notificationVeto;

    @XNode("@remove")
    private boolean remove = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<? extends NotificationListenerVeto> getNotificationVeto() {
        return notificationVeto;
    }

    public void setNotificationVeto(
            Class<? extends NotificationListenerVeto> notificationVeto) {
        this.notificationVeto = notificationVeto;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

}
