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
 *
 *      Nelson Silva
 */
package org.nuxeo.ecm.liveconnect.google.drive.credential;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.nuxeo.ecm.liveconnect.core.CredentialFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;

/**
 * Credential factory for Service Accounts.
 *
 * @since 7.3
 */
public class ServiceAccountCredentialFactory implements CredentialFactory {

    private final String accountId;

    private final File p12File;

    public ServiceAccountCredentialFactory(String accountId, File p12File) {
        this.accountId = accountId;
        this.p12File = p12File;
    }

    @Override
    public Credential build(String user) throws IOException {
        try {
            return new GoogleCredential.Builder() //
                .setTransport(getHttpTransport()) //
                .setJsonFactory(getJsonFactory()) //
                .setServiceAccountId(accountId) //
                .setServiceAccountPrivateKeyFromP12File(p12File) //
                .setServiceAccountScopes(Collections.singleton(DriveScopes.DRIVE)) //
                .setServiceAccountUser(user).build();
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    protected static JsonFactory getJsonFactory() {
        return JacksonFactory.getDefaultInstance();
    }

    protected static HttpTransport getHttpTransport() throws IOException {
        try {
            return GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }
}
