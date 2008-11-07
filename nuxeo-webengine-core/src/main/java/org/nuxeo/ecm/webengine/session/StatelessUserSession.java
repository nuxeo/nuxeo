package org.nuxeo.ecm.webengine.session;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

public class StatelessUserSession extends UserSession {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public StatelessUserSession(Principal principal) {
        super(principal);
    }

    public StatelessUserSession(Principal principal, String password) {
        super(principal, password);
    }

    public StatelessUserSession(Principal principal, Object credentials) {
        super(principal, credentials);
    }


    @Override
    public void terminateRequest(HttpServletRequest resuest) {
        uninstall();
    }

}
