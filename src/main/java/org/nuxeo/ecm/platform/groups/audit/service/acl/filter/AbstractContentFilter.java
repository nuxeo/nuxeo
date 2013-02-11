package org.nuxeo.ecm.platform.groups.audit.service.acl.filter;

import org.nuxeo.ecm.core.api.security.SecurityConstants;

public class AbstractContentFilter {

    public AbstractContentFilter() {
        super();
    }

    public boolean isEveryone(String userOrGroup) {
        return SecurityConstants.EVERYONE.equals(userOrGroup);
    }
}