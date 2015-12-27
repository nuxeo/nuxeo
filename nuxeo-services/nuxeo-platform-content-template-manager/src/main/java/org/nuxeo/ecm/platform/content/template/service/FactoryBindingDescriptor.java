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
