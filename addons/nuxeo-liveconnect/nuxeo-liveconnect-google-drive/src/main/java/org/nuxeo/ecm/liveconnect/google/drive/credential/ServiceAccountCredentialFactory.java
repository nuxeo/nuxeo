/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *
 *      Nelson Silva
 */

package org.nuxeo.ecm.liveconnect.google.drive.credential;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

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
