package org.nuxeo.ecm.platform.oauth2.request;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public abstract class Oauth2Request {

    public static final String CLIENT_ID = "client_id";

    public static final String REDIRECT_URI = "redirectUri";

    protected String clientId;

    protected String redirectUri;

    public Oauth2Request(HttpServletRequest request) {
        clientId = request.getParameter(CLIENT_ID);
        redirectUri = decodeParameter(request, REDIRECT_URI);
    }

    public static String decodeParameter(HttpServletRequest request,
            String parameterName) {
        String value = request.getParameter(parameterName);
        try {
            if (isNotBlank(value)) {
                return URLDecoder.decode(value, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            // Nothing to do.
        }
        return value;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getClientId() {
        return clientId;
    }
}
