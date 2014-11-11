package org.nuxeo.ecm.webapp.security;

import java.security.Principal;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface UserSession {

    public Principal getCurrentUser() throws Exception;

    public NuxeoPrincipal getCurrentNuxeoPrincipal() throws Exception;

    public boolean isAdministrator() throws Exception;

    public void destroy();

}
