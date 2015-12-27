/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.ec.notification.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;

/**
 * @since 5.6
 * @author Thierry Martins <tm@nuxeo.com>
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

    public void setNotificationVeto(Class<? extends NotificationListenerVeto> notificationVeto) {
        this.notificationVeto = notificationVeto;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

}
