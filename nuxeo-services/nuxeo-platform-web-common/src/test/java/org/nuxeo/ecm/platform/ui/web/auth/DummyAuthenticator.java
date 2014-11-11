package org.nuxeo.ecm.platform.ui.web.auth;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

/**
 * @author <a href="mailto:throger@gmail.com">Thomas Roger</a>
 */
public class DummyAuthenticator implements NuxeoAuthenticationPlugin {

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return null;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return null;
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return null;
    }

    public void initPlugin(Map<String, String> parameters) {
    }

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

}
