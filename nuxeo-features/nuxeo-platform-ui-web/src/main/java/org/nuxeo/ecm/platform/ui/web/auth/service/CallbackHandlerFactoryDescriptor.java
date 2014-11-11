package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "callbackHandlerFactory")
public class CallbackHandlerFactoryDescriptor implements Serializable {

    private static final long serialVersionUID = 237654398643289764L;

    @XNode("@name")
    private String name;

    @XNode("@class")
    Class className;

    public Class getClassName() {
        return className;
    }

    public void setClassName(Class className) {
        this.className = className;
    }

}
