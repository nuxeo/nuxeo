/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
