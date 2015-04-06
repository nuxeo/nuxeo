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
 *     Nelson Silva
 */
package org.nuxeo.ecm.googleclient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.ManagedBlobProvider;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.googleclient.credential.CredentialFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

/**
 * Provider for blobs getting information from Google Drive.
 *
 * @since 7.3
 */
public class GoogleDriveBlobProvider implements ManagedBlobProvider {

    private static final String APPLICATION_NAME = "Nuxeo/0";

    private final CredentialFactory credentialFactory;

    public GoogleDriveBlobProvider(CredentialFactory credentialFactory) {
        this.credentialFactory = credentialFactory;
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

    protected String getUser(String fileInfo) {
        return getFileInfo(fileInfo)[0];
    }

    protected String getFileId(String fileInfo) {
        return getFileInfo(fileInfo)[1];
    }

    protected String[] getFileInfo(String fileInfo) {
        String[] parts = fileInfo.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException(fileInfo);
        }
        return parts;
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
     * @param fileInfo the file info ({email}:{fileId})
     * @return the blob
     */
    public Blob getBlob(String fileInfo) throws IOException {
        String user = getUser(fileInfo);
        String fileId = getFileId(fileInfo);
        File file = getService(user).files().get(fileId).execute();
        String key = String.format("%s:%s:%s", GoogleDriveComponent.GOOGLE_DRIVE_PREFIX, user, fileId);
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

    public Credential getCredential(String user) throws IOException {
        return credentialFactory.build(user);
    }

    protected Drive getService(String user) throws IOException {
        Credential credential = getCredential(user);
        HttpTransport httpTransport = credential.getTransport();
        JsonFactory jsonFactory = credential.getJsonFactory();
        return new Drive.Builder(httpTransport, jsonFactory, credential) //
        .setApplicationName(APPLICATION_NAME) // set application name to avoid a WARN
        .build();
    }
}
