/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.content.template.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Notification Descriptor. Immutable.
 */
@XObject(value = "notification")
public class NotificationDescriptor {

    @XNode("@event")
    private String event;

    @XNodeList(value = "user", type = ArrayList.class, componentType = String.class)
    private List<String> users;

    @XNodeList(value = "group", type = ArrayList.class, componentType = String.class)
    private List<String> groups;

    public NotificationDescriptor() {
        // default constructor
        this.users = new ArrayList<>();
        this.groups = new ArrayList<>();
    }

    public NotificationDescriptor(NotificationDescriptor toCopy) {
        this.event = toCopy.event;
        this.users = new ArrayList<>(toCopy.users);
        this.groups = new ArrayList<>(toCopy.groups);
    }

    public String getEvent() {
        return event;
    }

    public List<String> getGroups() {
        return groups;
    }

    public List<String> getUsers() {
        return users;
    }

}
