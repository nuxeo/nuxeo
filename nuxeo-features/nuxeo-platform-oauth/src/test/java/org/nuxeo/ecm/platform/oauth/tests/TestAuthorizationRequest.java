/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Arnaud Kervern
 */
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

    public TestAuthorizationRequest(String clientId, String responseType, String state, String redirectUri,
            Date creationDate) {
        this.clientId = clientId;
        this.responseType = responseType;
        this.state = state;
        this.creationDate = creationDate;
        this.redirectUri = redirectUri;
    }
}
