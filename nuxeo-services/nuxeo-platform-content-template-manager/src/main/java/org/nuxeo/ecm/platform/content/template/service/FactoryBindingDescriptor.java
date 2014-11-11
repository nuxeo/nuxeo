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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Factory binding descriptor. Immutable.
 */
@XObject(value = "factoryBinding")
public class FactoryBindingDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@factoryName")
    private String factoryName;

    @XNode("@targetType")
    private String targetType;

    @XNode("@targetFacet")
    private String targetFacet;

    @XNode("@append")
    private Boolean append = false;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    private Map<String, String> options;

    @XNodeList(value = "template/templateItem", type = ArrayList.class, componentType = TemplateItemDescriptor.class)
    private List<TemplateItemDescriptor> template;

    // Declared as ArrayList to be serializable.
    @XNodeList(value = "acl/ace", type = ArrayList.class, componentType = ACEDescriptor.class)
    private List<ACEDescriptor> rootAcl;

    public String getFactoryName() {
        return factoryName;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetFacet() {
        return targetFacet;
    }

    public List<TemplateItemDescriptor> getTemplate() {
        return template;
    }

    public List<ACEDescriptor> getRootAcl() {
        return rootAcl;
    }

    public Boolean getAppend() {
        return append;
    }

    public void setAppend(Boolean append) {
        this.append = append;
    }
}
