package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
//import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

import org.jboss.seam.Seam;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.core.Manager;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;
import org.nuxeo.ecm.platform.ui.web.rest.FancyURLRequestWrapper;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

public class SeamJsfSessionManager extends DefaultSessionManager implements
        NuxeoAuthenticationSessionManager {

    @Override
    public boolean bypassRequest(ServletRequest request)
    {
        if (request instanceof FancyURLRequestWrapper)
            return true;
        return false;
    }

    @Override
    public void invalidateSession(ServletRequest request) {
        try {
            Seam.invalidateSession();
        }
        catch (Exception e) {
            super.invalidateSession(request);
        }
    }

    @Override
    public boolean isAvalaible(ServletRequest request) {
        if (FacesContext.getCurrentInstance()!=null)
            return true;
        return false;
    }

    @Override
    public HttpSession reinitSession(ServletRequest request) {
        // destroy session
        // because of Seam Phase Listener we can't use Seam.invalidateSession()
        // because the session would be invalidated at the end of the request !
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            // Make long-running conversation temporary
            Manager.instance().endConversation(true);
            Manager.instance().endRequest(externalContext.getSessionMap());
            ServletLifecycle.endRequest(httpRequest);
            session.invalidate();

            session = httpRequest.getSession(false);
            if (session != null) {
                session.invalidate();
            }

        }
        session = httpRequest.getSession(true);

        // reinit Seam so the afterResponseComplete does not crash
        ServletLifecycle.beginRequest(httpRequest);
        return session;
    }

}
