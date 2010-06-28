package org.nuxeo.ecm.webapp.security;

import java.util.List;

import org.nuxeo.ecm.core.api.security.UserEntry;

public interface SecurityDataPolicy {

    List<UserEntry> compute(SecurityData context);

}
