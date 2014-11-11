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

public class OAuthSessionFilter implements NuxeoAuthenticationPlugin {
    private static final Log log = LogFactory.getLog(OAuthSessionFilter.class);

    public List<String> getUnAuthenticatedURLPrefix() {
        return new ArrayList<String>();
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return Boolean.FALSE; // we never use a prompt
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String header = httpRequest.getHeader("X-Shindig-AuthType");

        String[] params = httpRequest.getParameterValues("oauth_signature");
        if ((params != null) && (params[0] != null)) {
            String signature = params[0].trim();
            params = httpRequest.getParameterValues("opensocial_viewer_id");
            if ((params != null) && (params[0] != null)) {
                String viewer = params[0].trim();
                if (checkSignature(httpRequest, viewer, signature)) {
                    // for now we don't check the info given to us
                    UserIdentificationInfo info = new UserIdentificationInfo(
                            viewer, "");
                    info.setLoginPluginName("OAUTH");
                    return info;
                }
            }
        }
        return null;
    }

    private boolean checkSignature(HttpServletRequest httpRequest,
            String viewer, String signature) {
        return true;
    }

    public void initPlugin(Map<String, String> parameters) {
        // nothing to do

    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return Boolean.FALSE;
    }

}
