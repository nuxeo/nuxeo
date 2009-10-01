package org.nuxeo.opensocial.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

public class JSessionAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(JSessionAuthenticator.class);

    public List<String> getUnAuthenticatedURLPrefix() {
        log.info("getUnauthenticatedURLPrefix called"
                + " not used by JSessionAuthenticator");
        return new ArrayList<String>();
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        log.info("handleLoginPrompt: " + baseURL + " ignored");
        return false;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        log.info("handleRetreiveIdentity has been called, searching for params");
        String sessionId = httpRequest.getHeader("jsessionid");
        if (sessionId == null) {
            log.info("Unable to find a jsessionid parameter");
            return null;
        }
        Object raw = httpRequest.getSession(false).getAttribute(sessionId);
        if (raw == null) {
            log.info("unable to find key in session!");
            return null;
        }
        log.info("found something in the session:" + raw.getClass().getName());
        return null;
    }

    public void initPlugin(Map<String, String> parameters) {
        log.info("initPlugin ignored (" + parameters.size() + ")");

    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        log.info("needLoginPrompt ignored");
        return false;
    }

}
