/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.oauth2.providers;

import static org.nuxeo.ecm.platform.oauth2.Constants.STATE_PARAM;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;

/**
 * @since 11.0
 */
public class FrameIOOAuth2ServiceProvider extends NuxeoOAuth2ServiceProvider {

    public static final String FRAMEIO_KEY_VALUE_STORE_NAME = "frameio";

    public static final String FRAMEIO_OAUTH2_KVS_KEY = "nuxeo.provider.frameio.oauth2.key";

    public static final String FRAMEIO_OAUTH2_KVS_STATE_KEY = "nuxeo.provider.frameio.oauth2.state";

    private static final RandomStringGenerator GENERATOR = new RandomStringGenerator.Builder().filteredBy(
            CharacterPredicates.LETTERS, CharacterPredicates.DIGITS).withinRange('0', 'z').build();

    private static final String state = GENERATOR.generate(40);

    @Override
    public String getAuthorizationUrl(HttpServletRequest request) {
        return getAuthorizationCodeFlow().newAuthorizationUrl()
                                         .setState(state)
                                         .setRedirectUri(getCallbackUrl(request))
                                         .build();
    }

    @Override
    public String getAuthorizationUrl(String serverURL) {
        return getAuthorizationCodeFlow().newAuthorizationUrl()
                                         .setState(state)
                                         .setRedirectUri(getCallbackUrl(serverURL))
                                         .build();
    }

    @Override
    public Credential handleAuthorizationCallback(HttpServletRequest request) {

        // Checking if there was an error such as the user denied access
        String error = getError(request);
        if (error != null) {
            throw new NuxeoException("There was an error: \"" + error + "\".");
        }

        // Checking conditions on the "code" URL parameter
        String code = getAuthorizationCode(request);
        if (code == null) {
            // FIXME: improve the error log: error_hint, error_description
            throw new NuxeoException("There is not code provided as QueryParam.");
        }

        if (!state.equals(request.getParameter(STATE_PARAM))) {
            throw new NuxeoException("Invalid state.");
        }

        try {
            AuthorizationCodeFlow flow = getAuthorizationCodeFlow();

            String redirectUri = getCallbackUrl(request);

            TokenResponse tokenResponse = flow.newTokenRequest(code)
                                              .setScopes(getScopes().isEmpty() ? null : getScopes())
                                              .setRedirectUri(redirectUri)
                                              .execute();

            // Create a unique userId to use with the credential store
            String userId = getOrCreateServiceUser(request, tokenResponse.getAccessToken());

            return flow.createAndStoreCredential(tokenResponse, userId);
        } catch (IOException e) {
            throw new NuxeoException("Failed to retrieve credential", e);
        }
    }

    @Override
    public AuthorizationCodeFlow getAuthorizationCodeFlow() {
        Credential.AccessMethod method = BearerToken.authorizationHeaderAccessMethod();
        GenericUrl tokenServerUrl = new GenericUrl(getTokenServerURL());
        String clientId = getClientId();
        HttpExecuteInterceptor basicAuthentication = new BasicAuthentication(clientId, getClientSecret());
        String authorizationServerUrl = getAuthorizationServerURL();

        return new AuthorizationCodeFlow.Builder(method, HTTP_TRANSPORT, JSON_FACTORY, tokenServerUrl,
                basicAuthentication, getClientId(), authorizationServerUrl)
                                                                           .setScopes(getScopes())
                                                                           .setCredentialDataStore(
                                                                                   getCredentialDataStore())
                                                                           .build();
    }

}
