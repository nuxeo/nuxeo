package org.nuxeo.ecm.platform.content.template.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("factoryBindingMultiTemplates")
public class FactoryBindingMultiTemplatesDescriptor extends FactoryBindingDescriptor {
    
    @XNode("name")
    public void setName(String value) {
        this.name = value;
    }
    
    @XNode("@factoryName")
    public void setFactoyName(String value) {
        this.factoryName = value;
    }

    @XNode("@targetType")
    public void setTargetType(String value) {
        this.targetType = value;
    }
    
    @XNode("@targetFacet")
    public void setTargetFacet(String value) {
        this.targetFacet = value;
    }

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public void setOptions(Map<String,String> value) {
        this.options = value;
    }
    
    @XNodeMap(value = "template", key="@name", type=HashMap.class, componentType = TemplateDescriptor.class)
    protected Map<String,TemplateDescriptor> templates;
    
    public List<TemplateItemDescriptor> getTemplate() {
        return templates.get("default").items;
    }
    
    public List<TemplateItemDescriptor> getTemplate(String name) {
        return templates.get(name).items;
    }
    
    @XNodeList(value = "acl/ace", type = ArrayList.class, componentType = ACEDescriptor.class)
    public void setRootAcl(List<ACEDescriptor> value) {
        this.rootAcl = value;
    }
}
