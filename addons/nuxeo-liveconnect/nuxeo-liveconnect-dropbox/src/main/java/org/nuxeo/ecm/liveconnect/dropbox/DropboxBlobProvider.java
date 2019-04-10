/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Andre Justo
 */
package org.nuxeo.ecm.liveconnect.dropbox;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectBlobProvider;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxThumbnailFormat;
import com.dropbox.core.DbxThumbnailSize;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;

/**
 * Provider for blobs getting information from Dropbox.
 *
 * @since 7.3
 */
public class DropboxBlobProvider extends AbstractLiveConnectBlobProvider<DropboxOAuth2ServiceProvider> {

    private static final String APPLICATION_NAME = "Nuxeo/0";

    private static final String FILE_CACHE_NAME = "dropbox";

    private static final String DROPBOX_DOCUMENT_TO_BE_UPDATED_PP = "dropbox_document_to_be_updated";

    @Override
    protected String getCacheName() {
        return FILE_CACHE_NAME;
    }

    @Override
    public String getPageProviderNameForUpdate() {
        return DROPBOX_DOCUMENT_TO_BE_UPDATED_PP;
    }

    @Override
    public URI getURI(ManagedBlob blob, UsageHint usage, HttpServletRequest servletRequest) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        String filePath = fileInfo.getFileId();
        String url = null;
        DbxClient client = getDropboxClient(getCredential(fileInfo));
        try {
            switch (usage) {
            case STREAM:
                url = client.createTemporaryDirectUrl(filePath).url;
                break;
            case DOWNLOAD:
                url = client.createShareableUrl(filePath);
                url = url.replace("dl=0", "dl=1"); // enable download flag in url
                break;
            case VIEW:
                url = client.createShareableUrl(filePath);
                break;
            }
        } catch (DbxException e) {
            throw new IOException("Failed to get Dropbox file URI " + e);
        }
        return url == null ? null : asURI(url);
    }

    @Override
    public Map<String, URI> getAvailableConversions(ManagedBlob blob, UsageHint hint) throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public InputStream getThumbnail(ManagedBlob blob) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        String filePath = fileInfo.getFileId();
        try {
            DbxClient.Downloader downloader = getDropboxClient(getCredential(fileInfo)).startGetThumbnail(
                    DbxThumbnailSize.w64h64, DbxThumbnailFormat.bestForFileName(filePath, DbxThumbnailFormat.JPEG),
                    filePath, null);

            if (downloader == null) {
                return null;
            }
            return downloader.body;
        } catch (DbxException e) {
            throw new IOException("Failed to get Dropbox file thumbnail " + e);
        }
    }

    @Override
    public InputStream getStream(ManagedBlob blob) throws IOException {
        URI uri = getURI(blob, UsageHint.STREAM, null);
        return uri == null ? null : doGet(uri);
    }

    @Override
    public InputStream getConvertedStream(ManagedBlob blob, String mimeType, DocumentModel doc) throws IOException {
        Map<String, URI> conversions = getAvailableConversions(blob, UsageHint.STREAM);
        URI uri = conversions.get(mimeType);
        if (uri == null) {
            return null;
        }
        return doGet(uri);
    }

    @Override
    public ManagedBlob freezeVersion(ManagedBlob blob, Document doc) throws IOException {
        return null;
    }

    protected DbxClient getDropboxClient(Credential credential) throws IOException {
        return getDropboxClient(credential.getAccessToken());
    }

    protected DbxClient getDropboxClient(String accessToken) throws IOException {
        DbxRequestConfig config = new DbxRequestConfig(APPLICATION_NAME, Locale.getDefault().toString());
        return new DbxClient(config, accessToken);
    }

    /**
     * Executes a GET request
     */
    protected InputStream doGet(URI url) throws IOException {
        HttpRequestFactory requestFactory = getOAuth2Provider().getRequestFactory();
        HttpResponse response = requestFactory.buildGetRequest(new GenericUrl(url)).execute();
        return response.getContent();
    }

    protected String getClientId() {
        OAuth2ServiceProvider provider = getOAuth2Provider();
        return provider != null ? provider.getClientId() : null;
    }

    @Override
    protected LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException {
        try {
            DbxEntry fileMetadata = getDropboxClient(getCredential(fileInfo)).getMetadata(fileInfo.getFileId());
            if (fileMetadata == null) {
                return null;
            }
            return new DropboxLiveConnectFile(fileInfo, fileMetadata.asFile());
        } catch (DbxException e) {
            throw new IOException("Failed to retrieve Dropbox file metadata", e);
        }
    }

}
