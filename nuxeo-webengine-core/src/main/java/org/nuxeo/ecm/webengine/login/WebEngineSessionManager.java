package org.nuxeo.ecm.webengine.login;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.DefaultSessionManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;

public class WebEngineSessionManager extends DefaultSessionManager implements
        NuxeoAuthenticationSessionManager {

    private static final String RESOURCES_PATH="/site/files";
    private static Log log = LogFactory.getLog(WebEngineSessionManager.class);
    private static boolean useSharedAnonymousSession=false;

    @Override
    public boolean canBypassRequest(ServletRequest request) {
        // resources do not require Authentication
        HttpServletRequest req = (HttpServletRequest)request;
        if (req.getServletPath().startsWith(RESOURCES_PATH))
            return true;
        return false;
    }

    @Override
    public void onAuthenticatedSessionCreated(ServletRequest request,
            HttpSession httpSession, CachableUserIdentificationInfo cachebleUserInfo) {

        UserSession userSession = null;
        if (useSharedAnonymousSession && ((NuxeoPrincipal) cachebleUserInfo.getPrincipal()).isAnonymous())
        {
            try {
                UserManager um = Framework.getService(UserManager.class);
                userSession = UserSession.getAnonymousSession(um);
            }catch(Exception e)
            {
                log.error("Error during Anonymous session creation", e);
                log.warn("Std UserSession will be used instead");
                // fall back to default session
            }
        }
        if (userSession==null)
        {
            // create WE custom UserSession
            userSession = new UserSession(cachebleUserInfo.getPrincipal(), cachebleUserInfo.getUserInfo().getPassword());
        }
        UserSession.setCurrentSession(httpSession, userSession);
    }

}
