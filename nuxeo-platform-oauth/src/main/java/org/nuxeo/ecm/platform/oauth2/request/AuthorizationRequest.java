package org.nuxeo.ecm.platform.oauth2.request;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.nuxeo.ecm.platform.ui.web.auth.oauth2.NuxeoOAuth2Filter.ERRORS.*;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class AuthorizationRequest extends Oauth2Request {
    private static final Log log = LogFactory.getLog(AuthorizationRequest.class);

    protected static Map<String, AuthorizationRequest> requests = new HashMap<>();

    protected String responseType;

    protected String scope;

    protected String state;

    protected String sessionId;

    protected Date creationDate;

    protected String authorizationCode;

    protected String authorizationKey;

    protected String username;

    public static final String RESPONSE_TYPE = "response_type";

    public static final String SCOPE = "scope";

    public static final String STATE = "state";

    public AuthorizationRequest() { }

    public AuthorizationRequest(HttpServletRequest request) {
        super(request);
        responseType = request.getParameter(RESPONSE_TYPE);

        scope = request.getParameter(SCOPE);
        state = request.getParameter(STATE);
        sessionId = request.getSession(true).getId();

        creationDate = new Date();
        authorizationKey = RandomStringUtils.random(6, true, false);
    }

    public String checkError() {
        // Check mandatory fields
        if (isBlank(responseType) || isBlank(clientId) || isBlank(redirectUri)) {
            return invalid_request.toString();
        }

        // Check if client exists
        try {
            ClientRegistry registry = Framework.getLocalService(ClientRegistry.class);
            if (!registry.hasClient(clientId)) {
                return unauthorized_client.toString();
            }
        } catch (ClientException e) {
            log.warn(e, e);
            return server_error.toString();
        }

        // Check request type
        if (!"code".equals(responseType)) {
            return unsupported_response_type.toString();
        }
        return null;
    }

    public boolean isExpired() {
        // RFC 4.1.2, Authorization code lifetime is 10
        return new Date().getTime() - creationDate.getTime() > 10 * 60 * 1000;
    }

    public boolean isValidState(HttpServletRequest request) {
        return isBlank(getState())
                || request.getParameter(STATE).equals(getState());
    }

    public String getUsername() {
        return username;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getScope() {
        return scope;
    }

    public String getState() {
        return state;
    }

    public String getAuthorizationCode() {
        if (isBlank(authorizationCode)) {
            authorizationCode = RandomStringUtils.random(10, true, true);
        }
        return authorizationCode;
    }

    public String getAuthorizationKey() {
        return authorizationKey;
    }

    private static void deleteExpiredRequests() {
        Iterator<AuthorizationRequest> iterator = requests.values().iterator();
        AuthorizationRequest req;
        while (iterator.hasNext() && (req = iterator.next()) != null) {
            if (req.isExpired()) {
                requests.remove(req.sessionId);
            }
        }
    }

    public static AuthorizationRequest from(HttpServletRequest request)
            throws UnsupportedEncodingException {
        deleteExpiredRequests();

        String sessionId = request.getSession(true).getId();
        if (requests.containsKey(sessionId)) {
            AuthorizationRequest authRequest = requests.get(sessionId);
            if (!authRequest.isExpired() && authRequest.isValidState(request)) {
                return authRequest;
            }
        }

        AuthorizationRequest authRequest = new AuthorizationRequest(request);
        requests.put(sessionId, authRequest);
        return authRequest;
    }

    public static AuthorizationRequest fromCode(String authorizationCode) {
        for (AuthorizationRequest auth : requests.values()) {
            if (auth.authorizationCode != null
                    && auth.authorizationCode.equals(authorizationCode)) {
                requests.remove(auth.sessionId);
                return auth.isExpired() ? null : auth;
            }
        }
        return null;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
