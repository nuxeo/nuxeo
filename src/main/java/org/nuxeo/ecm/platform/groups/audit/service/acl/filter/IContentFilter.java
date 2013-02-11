package org.nuxeo.ecm.platform.groups.audit.service.acl.filter;

public interface IContentFilter {
	public boolean acceptsUserOrGroup(String userOrGroup);
}
