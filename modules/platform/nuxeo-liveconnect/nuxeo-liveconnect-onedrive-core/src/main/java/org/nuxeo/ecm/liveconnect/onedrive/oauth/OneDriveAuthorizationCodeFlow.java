/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.onedrive.oauth;

import java.io.IOException;
import java.util.Objects;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.Credential.AccessMethod;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

/**
 * We need to add some hook on {@link AuthorizationCodeFlow} as OneDrive for Business needs the resource parameter for
 * token and refresh token requests. Furthermore their response don't follow OAuth standard for expires_in field. See
 * https://github.com/google/google-oauth-java-client/issues/62
 *
 * @since 8.2
 */
public class OneDriveAuthorizationCodeFlow extends AuthorizationCodeFlow {

    private static final String RESOURCE_PARAMETER = "resource";

    private final String businessResource;

    protected OneDriveAuthorizationCodeFlow(Builder builder) {
        super(builder);
        businessResource = Objects.requireNonNull(builder.businessResource);
    }

    @Override
    public AuthorizationCodeTokenRequest newTokenRequest(String authorizationCode) {
        OneDriveAuthorizationCodeTokenRequest tokenRequest = new OneDriveAuthorizationCodeTokenRequest(getTransport(),
                getJsonFactory(), new GenericUrl(getTokenServerEncodedUrl()), authorizationCode);
        tokenRequest.set(RESOURCE_PARAMETER, businessResource);
        return tokenRequest.setClientAuthentication(getClientAuthentication())
                           .setRequestInitializer(getRequestInitializer())
                           .setScopes(getScopes());
    }

    @Override
    public Credential loadCredential(String userId) throws IOException {
        return new OneDriveCredential(super.loadCredential(userId), businessResource);
    }

    public static class Builder extends AuthorizationCodeFlow.Builder {

        String businessResource;

        public Builder(AccessMethod method, HttpTransport transport, JsonFactory jsonFactory,
                GenericUrl tokenServerUrl, HttpExecuteInterceptor clientAuthentication, String clientId,
                String authorizationServerEncodedUrl) {
            super(method, transport, jsonFactory, tokenServerUrl, clientAuthentication, clientId,
                    authorizationServerEncodedUrl);
        }

        @Override
        public OneDriveAuthorizationCodeFlow build() {
            return new OneDriveAuthorizationCodeFlow(this);
        }

        public Builder setBusinessResource(String businessResource) {
            this.businessResource = Objects.requireNonNull(businessResource);
            return this;
        }

    }

}
