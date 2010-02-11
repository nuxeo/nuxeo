package org.nuxeo.ecm.platform.content.template.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("template")
public class TemplateDescriptor {
    
    @XNode("@name")
    public String name = "default";
    
    @XNode("@filter")
    public String filter = "default";
    
    @XNode("@property")
    public String property = "dc:source";
    
    @XNodeList(value="templateItem", type=ArrayList.class, componentType=TemplateItemDescriptor.class)
    public List<TemplateItemDescriptor> items;
}
