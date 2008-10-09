package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;

@XObject(value = "sessionManager")
public class SessionManagerDescriptor implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;


    @XNode("@name")
    private String name;

    @XNode("@enabled")
    Boolean enabled = true;

    @XNode("@class")
    Class<NuxeoAuthenticationSessionManager> className;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Class<NuxeoAuthenticationSessionManager> getClassName() {
        return className;
    }

    public void setClassName(Class<NuxeoAuthenticationSessionManager> className) {
        this.className = className;
    }

}
