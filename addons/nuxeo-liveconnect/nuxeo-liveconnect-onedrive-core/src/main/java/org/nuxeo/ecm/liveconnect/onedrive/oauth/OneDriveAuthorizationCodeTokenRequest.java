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

import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

/**
 * We need this class for oauth with OneDrive as they don't return {@code expiresInSeconds} in number format. See
 * https://github.com/google/google-oauth-java-client/issues/62
 *
 * @since 8.2
 */
class OneDriveAuthorizationCodeTokenRequest extends AuthorizationCodeTokenRequest {

    public OneDriveAuthorizationCodeTokenRequest(HttpTransport transport, JsonFactory jsonFactory,
            GenericUrl tokenServerUrl, String code) {
        super(transport, jsonFactory, tokenServerUrl, code);
    }

    @Override
    public TokenResponse execute() throws IOException {
        return executeUnparsed().parseAs(OneDriveTokenResponse.class).toTokenResponse();
    }

}
