/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
