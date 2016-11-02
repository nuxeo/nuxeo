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
package org.nuxeo.ecm.liveconnect.onedrive;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectOAuth2ServiceProvider;
import org.nuxeo.ecm.liveconnect.onedrive.oauth.OneDriveAuthorizationCodeFlow;
import org.nuxeo.onedrive.client.OneDriveAPI;
import org.nuxeo.onedrive.client.OneDriveBasicAPI;
import org.nuxeo.onedrive.client.OneDriveBusinessAPI;
import org.nuxeo.onedrive.client.OneDriveEmailAccount;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;

/**
 * @since 8.2
 */
public class OneDriveOAuth2ServiceProvider extends AbstractLiveConnectOAuth2ServiceProvider {

    @Override
    protected String getUserEmail(String accessToken) throws IOException {
        OneDriveAPI api = getAPIInitializer().apply(accessToken);
        return OneDriveEmailAccount.getCurrentUserEmailAccount(api);
    }

    protected Optional<String> getOneDriveForBusinessResource() {
        GenericUrl tokenServerUrl = new GenericUrl(getTokenServerURL());
        return Optional.ofNullable((String) tokenServerUrl.getFirst("resource")).map(
                resource -> resource.replaceAll("\\\\/", "/"));
    }

    /**
     * Returns the {@link OneDriveAPI} initializer which takes an access token.
     *
     * @return the {@link OneDriveAPI} initializer which takes an access token.
     */
    public Function<String, OneDriveAPI> getAPIInitializer() {
        Optional<String> businessResourceURL = getOneDriveForBusinessResource();
        if (businessResourceURL.isPresent()) {
            return accessToken -> new OneDriveBusinessAPI(businessResourceURL.get(), accessToken);
        }
        return OneDriveBasicAPI::new;
    }

    @Override
    public AuthorizationCodeFlow getAuthorizationCodeFlow() {
        Optional<String> businessResource = getOneDriveForBusinessResource();
        if (businessResource.isPresent()) {
            String clientId = getClientId();
            String clientSecret = getClientSecret();
            String authorizationServerURL = getAuthorizationServerURL();

            Credential.AccessMethod method = BearerToken.authorizationHeaderAccessMethod();
            GenericUrl tokenServerUrl = new GenericUrl(getTokenServerURL());
            HttpExecuteInterceptor clientAuthentication = new ClientParametersAuthentication(clientId, clientSecret);

            return new OneDriveAuthorizationCodeFlow.Builder(method, HTTP_TRANSPORT, JSON_FACTORY, tokenServerUrl,
                    clientAuthentication, clientId, authorizationServerURL).setBusinessResource(businessResource.get())
                                                                           .setScopes(getScopes())
                                                                           .setCredentialDataStore(
                                                                                   getCredentialDataStore())
                                                                           .build();
        }
        return super.getAuthorizationCodeFlow();
    }

}
