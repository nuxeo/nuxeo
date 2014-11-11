package org.nuxeo.ecm.platform.oauth.tests;

import java.util.Date;
import java.util.Map;

import org.nuxeo.ecm.platform.oauth2.request.AuthorizationRequest;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class TestAuthorizationRequest extends AuthorizationRequest {

    public static Map<String, AuthorizationRequest> getRequests() {
        return AuthorizationRequest.requests;
    }

    public TestAuthorizationRequest(String clientId, String responseType,
            String state, String redirectUri, Date creationDate) {
        this.clientId = clientId;
        this.responseType = responseType;
        this.state = state;
        this.creationDate = creationDate;
        this.redirectUri = redirectUri;
    }
}
