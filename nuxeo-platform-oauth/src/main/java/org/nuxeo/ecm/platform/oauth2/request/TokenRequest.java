package org.nuxeo.ecm.platform.oauth2.request;

import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class TokenRequest extends Oauth2Request {

    protected String grantType;

    protected String code;

    protected String clientSecret;

    protected String refreshToken;

    public TokenRequest(HttpServletRequest request) {
        super(request);
        grantType = request.getParameter("grant_type");
        code = request.getParameter("code");
        clientSecret = request.getParameter("client_secret");
        refreshToken = request.getParameter("refresh_token");
    }

    public String getGrantType() {
        return grantType;
    }

    public String getCode() {
        return code;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
