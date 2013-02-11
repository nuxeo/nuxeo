package org.nuxeo.ecm.platform.groups.audit.service.acl.filter;

public class AcceptsConnectClientOnly extends AbstractContentFilter implements
        IContentFilter {
    @Override
    public boolean acceptsUserOrGroup(String userOrGroup) {
        if (isEveryone(userOrGroup))
            return true;
        if (userOrGroup.startsWith("ConnectClient"))
            return true;
        return false;
    }
}
