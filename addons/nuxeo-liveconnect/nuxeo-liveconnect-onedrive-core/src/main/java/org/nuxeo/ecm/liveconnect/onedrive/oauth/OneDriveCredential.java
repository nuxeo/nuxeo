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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;

/**
 * @since 8.2
 */
class OneDriveCredential extends Credential {

    private static final String RESOURCE_PARAMETER = "resource";

    private final String businessResource;

    public OneDriveCredential(Credential credential, String businessResource) {
        super(new Credential.Builder(credential.getMethod()).setTransport(credential.getTransport())
                                                            .setJsonFactory(credential.getJsonFactory())
                                                            .setTokenServerEncodedUrl(
                                                                    credential.getTokenServerEncodedUrl())
                                                            .setClientAuthentication(
                                                                    credential.getClientAuthentication())
                                                            .setRequestInitializer(credential.getRequestInitializer())
                                                            .setRefreshListeners(credential.getRefreshListeners())
                                                            .setClock(credential.getClock()));
        setAccessToken(credential.getAccessToken());
        setRefreshToken(credential.getRefreshToken());
        setExpirationTimeMilliseconds(credential.getExpirationTimeMilliseconds());
        this.businessResource = Objects.requireNonNull(businessResource);
    }

    @Override
    protected TokenResponse executeRefreshToken() throws IOException {
        String refreshToken = getRefreshToken();
        if (refreshToken == null) {
            return null;
        }
        OneDriveRefreshTokenRequest refreshTokenRequest = new OneDriveRefreshTokenRequest(getTransport(),
                getJsonFactory(), new GenericUrl(getTokenServerEncodedUrl()), refreshToken);
        refreshTokenRequest.set(RESOURCE_PARAMETER, businessResource);
        return refreshTokenRequest.setClientAuthentication(getClientAuthentication())
                                  .setRequestInitializer(getRequestInitializer())
                                  .execute();
    }

}
