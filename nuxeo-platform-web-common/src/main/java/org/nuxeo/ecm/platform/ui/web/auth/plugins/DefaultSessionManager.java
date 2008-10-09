package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;

public class DefaultSessionManager implements NuxeoAuthenticationSessionManager {


    public boolean bypassRequest(ServletRequest request)
    {
        return false;
    }

    public void invalidateSession(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }

    public boolean isAvalaible(ServletRequest request) {
        return true;
    }

    public HttpSession reinitSession(ServletRequest request) {

         HttpServletRequest httpRequest = (HttpServletRequest) request;
         HttpSession session = httpRequest.getSession(false);

         if (session != null) {
             session.invalidate();
         }

         // create new one
         session = httpRequest.getSession(true);

         return session;
    }


}
