/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.liveconnect.core;

import java.io.IOException;

import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;

import com.google.api.client.auth.oauth2.Credential;

/**
 * OAuth2 credential factory.
 *
 * @since 8.1
 */
public class OAuth2CredentialFactory implements CredentialFactory {

    private final OAuth2ServiceProvider provider;

    public OAuth2CredentialFactory(OAuth2ServiceProvider provider) {
        this.provider = provider;
    }

    @Override
    public Credential build(String user) throws IOException {
        return provider.loadCredential(user);
    }

}
