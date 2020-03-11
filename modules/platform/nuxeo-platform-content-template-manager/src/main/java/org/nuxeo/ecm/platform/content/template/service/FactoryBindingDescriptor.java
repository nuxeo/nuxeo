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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Factory binding descriptor. Immutable.
 */
@XObject(value = "factoryBinding")
public class FactoryBindingDescriptor {

    private static final Log log = LogFactory.getLog(FactoryBindingDescriptor.class);

    @XNode("@name")
    private String name;

    @XNode("@factoryName")
    private String factoryName;

    @XNode("@targetType")
    private String targetType;

    @XNode("@targetFacet")
    private String targetFacet;

    @XNode("@append")
    private Boolean append = Boolean.FALSE;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    private Map<String, String> options;

    @XNodeList(value = "template/templateItem", type = ArrayList.class, componentType = TemplateItemDescriptor.class)
    private List<TemplateItemDescriptor> template;

    // Declared as ArrayList to be serializable.
    @XNodeList(value = "acl/ace", type = ArrayList.class, componentType = ACEDescriptor.class)
    private List<ACEDescriptor> rootAcl;

    public FactoryBindingDescriptor() {
        // default constructor
        this.options = new HashMap<>();
        this.template = new ArrayList<>();
        this.rootAcl = new ArrayList<>();
    }

    public FactoryBindingDescriptor(FactoryBindingDescriptor toCopy) {
        this.name = toCopy.name;
        this.factoryName = toCopy.factoryName;
        this.targetType = toCopy.targetType;
        this.targetFacet = toCopy.targetFacet;
        this.options = new HashMap<>(toCopy.options);
        this.template = toCopy.template.stream().map(TemplateItemDescriptor::new).collect(Collectors.toList());
        this.rootAcl = toCopy.rootAcl.stream().map(ACEDescriptor::new).collect(Collectors.toList());
    }

    public String getFactoryName() {
        return factoryName;
    }

    protected void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public String getTargetType() {
        return targetType;
    }

    protected void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetFacet() {
        return targetFacet;
    }

    protected void setTargetFacet(String targetFacet) {
        this.targetFacet = targetFacet;
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

    public void merge(FactoryBindingDescriptor src) {
        if (Boolean.TRUE.equals(src.getAppend())) {
            if (log.isInfoEnabled()) {
                log.info("FactoryBinding " + name + " is merging with " + src.getName());
            }
        } else {
            // this needs to be overridden by src
            factoryName = src.getFactoryName();
            name = src.getName();
            targetType = src.getTargetType();
            targetFacet = src.getTargetFacet();
            append = Boolean.FALSE;
            options.clear();
            rootAcl.clear();
            template.clear();
        }
        options.putAll(src.getOptions());
        rootAcl.addAll(src.getRootAcl());
        template.addAll(src.getTemplate());
    }

}
