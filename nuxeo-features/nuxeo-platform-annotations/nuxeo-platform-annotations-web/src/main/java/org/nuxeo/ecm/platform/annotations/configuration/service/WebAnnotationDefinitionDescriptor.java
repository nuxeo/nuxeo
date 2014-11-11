/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.configuration.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
@XObject("webAnnotation")
public class WebAnnotationDefinitionDescriptor {

    @XNode("@uri")
    private String uri;

    @XNode("@name")
    private String name;

    @XNode("@icon")
    private String icon;

    @XNode("@type")
    private String type;

    @XNode("@enabled")
    private Boolean enabled = true;

    @XNode("@listIcon")
    private String listIcon;

    @XNode("@createIcon")
    private String createIcon;

    @XNode("@inMenu")
    private Boolean inMenu = false;

    @XNodeList(value = "field", type = WebAnnotationFieldDescriptor[].class, componentType = WebAnnotationFieldDescriptor.class)
    private WebAnnotationFieldDescriptor[] fields = new WebAnnotationFieldDescriptor[0];

    public String getUri() {
        return uri;
    }

    /**
     * @return the label.
     */
    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getType() {
        return type;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public String getListIcon() {
        return listIcon;
    }

    public String getCreateIcon() {
        return createIcon;
    }

    public Boolean isInMenu() {
        return inMenu;
    }

    public WebAnnotationFieldDescriptor[] getFields() {
        return fields;
    }

}
