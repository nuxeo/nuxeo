/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Template item descriptor. Immutable.
 */
@XObject(value = "templateItem")
public class TemplateItemDescriptor  implements Serializable {

    private static final long serialVersionUID = 18765764747899L;

    @XNode("@typeName")
    private String typeName;

    @XNode("@id")
    private String id;

    @XNode("@title")
    private String title;

    @XNode("@path")
    private String path;

    @XNode("@description")
    private String description;

    // Declared as ArrayList to be serializable.
    @XNodeList(value = "acl/ace", type = ArrayList.class, componentType = ACEDescriptor.class)
    public List<ACEDescriptor> acl;

    @XNodeList(value = "properties/property", type = ArrayList.class, componentType = PropertyDescriptor.class)
    public List<PropertyDescriptor> properties;

    @XNodeList(value = "notifications/notification", type = ArrayList.class, componentType = NotificationDescriptor.class)
    public List<NotificationDescriptor> notifications;

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getTitle() {
        return title;
    }

    public List<ACEDescriptor> getAcl() {
        return acl;
    }

    public List<PropertyDescriptor> getProperties() {
        return properties;
    }

    public List<NotificationDescriptor> getNotifications() {
        return notifications;
    }

}
