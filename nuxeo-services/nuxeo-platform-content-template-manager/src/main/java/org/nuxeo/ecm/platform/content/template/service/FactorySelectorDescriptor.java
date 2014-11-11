package org.nuxeo.ecm.platform.content.template.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("factorySelector")
public class FactorySelectorDescriptor {
    
    @XNode("@name")
    String name;
    
    @XNode("@class")
    Class<? extends FactorySelector> selector;

}
