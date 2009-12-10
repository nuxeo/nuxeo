package org.nuxeo.ecm.platform.wss.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("backendFactory")
public class BackendFactoryDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected Class className;


    public Class getFactoryClass() {
        return className;
    }

}
