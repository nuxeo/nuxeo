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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.googleclient;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.ManagedBlobProvider;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

/**
 * Provider for blobs getting information from Google Drive.
 *
 * @since 7.2
 */
public class GoogleDriveBlobProvider implements ManagedBlobProvider {

    public static final String SERVICE_ACCOUNT_ID_PROP = "nuxeo.google.serviceAccountId";

    public static final String SERVICE_ACCOUNT_P12_PATH_PROP = "nuxeo.google.serviceAccountP12Path";

    private static final String USER = "fguillaume@nuxeo.com"; // XXX don't hardcode

    protected String serviceAccountId;

    protected String p12;

    public GoogleDriveBlobProvider() {
        serviceAccountId = Framework.getProperty(SERVICE_ACCOUNT_ID_PROP);
        if (StringUtils.isBlank(serviceAccountId)) {
            throw new NuxeoException("Missing value for property: " + SERVICE_ACCOUNT_ID_PROP);
        }
        p12 = Framework.getProperty(SERVICE_ACCOUNT_P12_PATH_PROP);
        if (StringUtils.isBlank(p12)) {
            throw new NuxeoException("Missing value for property: " + SERVICE_ACCOUNT_P12_PATH_PROP);
        }
        java.io.File p12File = new java.io.File(p12);
        if (!p12File.exists()) {
            throw new NuxeoException("No such file: " + p12 + " for property: " + SERVICE_ACCOUNT_P12_PATH_PROP);
        }
    }

    @Override
    public SimpleManagedBlob createManagedBlob(String repositoryName, BlobInfo blobInfo, Document doc)
            throws IOException {
        return new SimpleManagedBlob(blobInfo, this);
    }

    @Override
    public BlobInfo getBlobInfo(String repositoryName, Blob blob, Document doc) {
        throw new UnsupportedOperationException("Storing a standard blob is not supported");
    }

    protected String getFileId(ManagedBlob blob) {
        String fileInfo = getFileInfo(blob);
        return getFileId(fileInfo);
    }

    protected String getUser(ManagedBlob blob) {
        String fileInfo = getFileInfo(blob);
        return getUser(fileInfo);
    }

    protected String getFileInfo(ManagedBlob blob) {
        String key = blob.getKey();
        int colon = key.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException(key);
        }
        String fileInfo = key.substring(colon + 1);
        return fileInfo;
    }

    protected String getFileId(String fileInfo) {
        // TODO add user in fileInfo
        return fileInfo;
    }

    protected String getUser(String fileInfo) {
        // TODO add user in fileInfo
        return USER;
    }

    @Override
    public InputStream getStream(ManagedBlob blob) throws IOException {
        String user = getUser(blob);
        String fileId = getFileId(blob);
        Drive service = getService(user);
        File file = service.files().get(fileId).execute();
        String url = file.getDownloadUrl();
        if (url == null) {
            throw new NuxeoException("No download URL for file: " + fileId);
        }
        HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
        return resp.getContent();
    }

    @Override
    public InputStream getConvertedStream(ManagedBlob blob, String mimeType) throws IOException {
        String user = getUser(blob);
        String fileId = getFileId(blob);
        Drive service = getService(user);
        File file = service.files().get(fileId).execute();
        Map<String, String> exportLinks = file.getExportLinks();
        String url = null;
        if (exportLinks != null) {
            url = exportLinks.get(mimeType);
        }
        if (url == null) {
            return null;
        }
        HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
        return resp.getContent();
    }

    @Override
    public List<String> getAvailableConversions(ManagedBlob blob) throws IOException {
        String user = getUser(blob);
        String fileId = getFileId(blob);
        Drive service = getService(user);
        File file = service.files().get(fileId).execute();
        Map<String, String> exportLinks = file.getExportLinks();
        return exportLinks == null ? Collections.emptyList() : new ArrayList<>(exportLinks.keySet());
    }

    // other links available in the file metadata:
    // alternateLink = web link to go edit online
    // embedLink = web link for an online preview
    // iconLink = link to icon (static, no auth)
    // thumbnailLink = link to document thumbnail

    /**
     * Gets the blob for a Google Drive file.
     *
     * @param fileInfo the file info
     * @return the blob
     */
    public Blob getBlob(String fileInfo) throws IOException {
        String user = getUser(fileInfo);
        String fileId = getFileId(fileInfo);
        File file = getService(user).files().get(fileId).execute();
        String key = GoogleDriveComponent.GOOGLE_DRIVE_PREFIX + ":" + fileId;
        String filename = file.getOriginalFilename();
        if (filename == null) {
            filename = file.getTitle().replace("/", "-");
        }
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = key;
        blobInfo.mimeType = file.getMimeType();
        blobInfo.encoding = null; // TODO extract from mimeType
        blobInfo.filename = filename;
        blobInfo.length = file.getFileSize();
        blobInfo.digest = file.getMd5Checksum();
        return new SimpleManagedBlob(blobInfo, this);
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

    protected Drive getService(String user) throws IOException {
        GoogleCredential credential = getCredential(user);
        HttpTransport httpTransport = credential.getTransport();
        JsonFactory jsonFactory = credential.getJsonFactory();
        return new Drive.Builder(httpTransport, jsonFactory, credential) //
        .setApplicationName("Nuxeo/0") // set application name to avoid a WARN
        .build();
    }

    protected GoogleCredential getCredential(String user) throws IOException {
        return getCredentialFromServiceAccount(user, serviceAccountId, p12);
    }

    protected GoogleCredential getCredentialFromServiceAccount(String user, String serviceAccountId, String p12)
            throws IOException {
        try {
            return new GoogleCredential.Builder() //
            .setTransport(getHttpTransport()) //
            .setJsonFactory(getJsonFactory()) //
            .setServiceAccountId(serviceAccountId) //
            .setServiceAccountPrivateKeyFromP12File(new java.io.File(p12)) //
            .setServiceAccountScopes(Collections.singleton(DriveScopes.DRIVE)) //
            .setServiceAccountUser(user).build();
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

}
