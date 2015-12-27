/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
public class TemplateItemDescriptor implements Serializable {

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
