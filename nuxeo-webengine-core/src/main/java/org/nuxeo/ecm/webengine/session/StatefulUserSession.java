package org.nuxeo.ecm.webengine.session;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.nuxeo.ecm.webengine.session.UserSession;

public class StatefulUserSession extends UserSession implements HttpSessionBindingListener {


    public StatefulUserSession(Principal principal) {
        super(principal);
    }

    public StatefulUserSession(Principal principal, String password) {
        super(principal, password);
    }

    public StatefulUserSession(Principal principal, Object credentials) {
        super(principal, credentials);
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public void valueBound(HttpSessionBindingEvent event) {
        // the user session was bound to the HTTP session
        install();
    }

    public void valueUnbound(HttpSessionBindingEvent event) {
        // the user session was removed from the HTTP session
        uninstall();
    }

    @Override
    public void terminateRequest(HttpServletRequest request) {
        // TODO Auto-generated method stub

    }

}
