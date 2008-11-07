package org.nuxeo.ecm.webengine.login;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.DefaultSessionManager;
import org.nuxeo.ecm.webengine.session.StatefulUserSession;
import org.nuxeo.ecm.webengine.session.StatelessUserSession;
import org.nuxeo.ecm.webengine.session.UserSession;

public class WebEngineSessionManager extends DefaultSessionManager {

    // TODO work on skin request to avoid hardcoding paths
    private static final String RESOURCES_PATH = "/nuxeo/site/files/";
    private static final Log log = LogFactory
            .getLog(WebEngineSessionManager.class);

    @Override
    public boolean canBypassRequest(ServletRequest request) {
        // static resources don't require Authentication
        return ((HttpServletRequest) request).getRequestURI().startsWith(
                RESOURCES_PATH);
    }

    @Override
    public void onAuthenticatedSessionCreated(ServletRequest request,
            HttpSession httpSession,
            CachableUserIdentificationInfo cachebleUserInfo) {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        UserSession userSession = null;
        // check for a valid session
        if (httpSession == null) {
            httpSession = httpRequest.getSession(false);
        }

        if (httpSession == null) {
            // create WE custom UserSession
            userSession = new StatelessUserSession(cachebleUserInfo
                    .getPrincipal(), cachebleUserInfo.getUserInfo()
                    .getPassword());
            log.debug("Creating Stateless UserSession");
        } else {
            // create WE custom UserSession
            userSession = new StatefulUserSession(cachebleUserInfo
                    .getPrincipal(), cachebleUserInfo.getUserInfo()
                    .getPassword());
            log.debug("Creating Stateful UserSession");
        }

        UserSession.register(httpRequest, userSession);
    }

    @Override
    public boolean needResetLogin(ServletRequest req) {
        String p = ((HttpServletRequest) req).getPathInfo();
        return p != null && p.startsWith("/login");
    }

}
