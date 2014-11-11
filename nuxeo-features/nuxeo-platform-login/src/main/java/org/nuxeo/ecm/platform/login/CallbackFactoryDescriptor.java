package org.nuxeo.ecm.platform.login;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "CallbackFactory")
public class CallbackFactoryDescriptor {

    @XNode("@class")
    protected Class className;

    @XNode("enabled")
    protected Boolean enabled;

    @XNode("@name")
    protected String name;

    public Class getClassName() {
        return className;
    }


    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
