package org.nuxeo.ecm.platform.userworkspace.core.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;

@XObject("userWorkspace")
public class UserWorkspaceDescriptor {


	@XNode("@targetDomainName")
	private String targetDomainName = "default-domain";

    @XNode("@class")
    private Class<? extends UserWorkspaceService> userWorkspaceClass;

    public Class<? extends UserWorkspaceService> getUserWorkspaceClass() {
        return userWorkspaceClass;
    }


    public String getTargetDomainName() {
    	return targetDomainName;
    }
}
