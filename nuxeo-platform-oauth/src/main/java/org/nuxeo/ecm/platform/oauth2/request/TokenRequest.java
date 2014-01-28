package org.nuxeo.ecm.platform.oauth2.request;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class TokenRequest {
    protected String grantType;

    protected String code;

    protected String redirectUri;

    protected String clientId;

    protected String clientSecret;

    public TokenRequest(HttpServletRequest request)
            throws UnsupportedEncodingException {
        grantType = request.getParameter("grant_type");
        code = request.getParameter("code");
        clientId = request.getParameter("client_id");
        redirectUri = URLDecoder.decode(request.getParameter("redirect_uri"),
                "UTF-8");
        clientSecret = request.getParameter("client_secret");
    }

    public String getGrantType() {
        return grantType;
    }

    public String getCode() {
        return code;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientId() {
        return clientId;
    }
}
