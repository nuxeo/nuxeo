package org.nuxeo.ecm.platform.oauth2.request;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public abstract class Oauth2Request {

    private static final Log log = LogFactory.getLog(Oauth2Request.class);

    public static final String CLIENT_ID = "client_id";

    public static final String REDIRECT_URI = "redirect_uri";

    public static final String REDIRECT_URL = "redirect_url";

    protected String clientId;

    protected String redirectUri;

    public Oauth2Request() { }

    public Oauth2Request(HttpServletRequest request) {
        clientId = request.getParameter(CLIENT_ID);
        redirectUri = decodeParameter(request, REDIRECT_URI);
        // Fallback for non-RFC compliant client
        if (isBlank(redirectUri)) {
            redirectUri = decodeParameter(request, REDIRECT_URL);
        }
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
