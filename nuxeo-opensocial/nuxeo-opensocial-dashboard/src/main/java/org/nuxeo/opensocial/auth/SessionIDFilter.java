package org.nuxeo.opensocial.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

public class SessionIDFilter implements NuxeoAuthenticationPlugin {

    private static final String JSSESSIONID = "JSESSIONID";;

    public List<String> getUnAuthenticatedURLPrefix() {
        return new ArrayList<String>();
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return Boolean.FALSE; // we never use a prompt
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String[] params = httpRequest.getParameterValues(JSSESSIONID);
        if ((params != null) && (params[0] != null)) {
            String sessionID = params[0].trim();
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                UserIdentificationInfo result = (UserIdentificationInfo) session.getAttribute(NXAuthConstants.USERIDENT_KEY);
                return result;
            }
        }
        return null;
    }

    public void initPlugin(Map<String, String> parameters) {
        // nothing to do

    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return Boolean.FALSE;
    }

}
