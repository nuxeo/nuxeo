package org.nuxeo.ecm.platform.groups.audit.service.acl.filter;

public class AcceptsAllContent implements IContentFilter {
    @Override
    public boolean acceptsUserOrGroup(String userOrGroup) {
        return true;
    }
}
