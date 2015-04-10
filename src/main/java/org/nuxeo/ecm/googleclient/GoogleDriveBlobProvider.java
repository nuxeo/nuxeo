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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
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
public class GoogleDriveBlobProvider implements BlobProvider {

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

    @Override
    public URI getURI(ManagedBlob blob, ManagedBlob.UsageHint usage) throws IOException {
        String url = null;
        File file = getFile(blob);
        switch (usage) {
        case STREAM:
            url = file.getDownloadUrl();
            break;
        case DOWNLOAD:
            url = file.getWebContentLink();
            if (url == null) {
                url = file.getAlternateLink();
            }
            break;
        case VIEW:
        case EDIT:
            url = file.getAlternateLink();
            break;
        case EMBED:
            url = file.getEmbedLink();
            if (url == null) {
                // non-native file resources do not return an embedLink but it is available
                url = file.getAlternateLink();
                URI uri = asURI(url).resolve("./preview");
                url = uri.toString();
            }
            break;
        }
        return (url != null) ? asURI(url) : null;
    }
    
    @Override
    public InputStream getStream(String blobKey, URI uri) throws IOException {
        String info = getFileInfo(blobKey);
        String user = getUser(info);
        HttpResponse resp = doGet(user, uri);
        return resp.getContent();
    }

    @Override
    public Map<String, URI> getAvailableConversions(ManagedBlob blob, ManagedBlob.UsageHint hint) throws IOException {
        File file = getFile(blob);
        Map<String, String> exportLinks = file.getExportLinks();
        if (exportLinks == null) {
            return Collections.emptyMap();
        }
        Map<String, URI> conversions = new HashMap<>();
        for (String mimeType : exportLinks.keySet()) {
            conversions.put(mimeType, asURI(exportLinks.get(mimeType)));
        }
        return conversions;
    }

    @Override
    public URI getThumbnail(ManagedBlob blob, ManagedBlob.UsageHint hint) throws IOException {
        return asURI(getFile(blob).getThumbnailLink());
    }

    /**
     * Gets the blob for a Google Drive file.
     *
     * @param fileInfo the file info ({email}:{fileId})
     * @return the blob
     */
    public Blob getBlob(String fileInfo) throws IOException {
        String user = getUser(fileInfo);
        String fileId = getFileId(fileInfo);
        File file = getFile(user, fileId);
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
        // etag for native docs and md5 for everything else
        String digest = file.getMd5Checksum();
        if (digest == null) {
            digest = file.getEtag();
        }
        blobInfo.digest = digest;
        return new SimpleManagedBlob(blobInfo, this);
    }

    protected String getFileInfo(String key) {
        int colon = key.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException(key);
        }
        String fileInfo = key.substring(colon + 1);
        return fileInfo;
    }

    protected String getUser(String fileInfo) {
        return getFileInfoParts(fileInfo)[0];
    }

    protected String getFileId(String fileInfo) {
        return getFileInfoParts(fileInfo)[1];
    }

    protected String[] getFileInfoParts(String fileInfo) {
        String[] parts = fileInfo.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException(fileInfo);
        }
        return parts;
    }

    protected Credential getCredential(String user) throws IOException {
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

    protected File getFile(ManagedBlob blob) throws IOException {
        String fileInfo = getFileInfo(blob.getKey());
        String user = getUser(fileInfo);
        String fileId = getFileId(fileInfo);
        return getFile(user, fileId);
    }

    protected File getFile(String user, String fileId) throws IOException {
        return getService(user).files().get(fileId).execute();
    }

    /**
     * Executes a GET request with the user's credentials
     *
     * @return a {@link HttpResponse}
     */
    protected HttpResponse doGet(String user, URI url) throws IOException {
        return getService(user).getRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
    }

    /**
     * Parse a {@link URI}.
     *
     * @return the {@link URI} or null if it fails
     */
    private URI asURI(String link) {
        URI uri = null;
        try {
            uri = new URI(link);
        } catch (URISyntaxException e) {
            //
        }
        return uri;
    }
}
