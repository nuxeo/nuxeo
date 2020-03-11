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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectBlobProvider;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.files.ThumbnailFormat;
import com.dropbox.core.v2.files.ThumbnailSize;

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

    private static final Log log = LogFactory.getLog(DropboxBlobProvider.class);

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
        DbxClientV2 client = getDropboxClient(getCredential(fileInfo));
        try {
            switch (usage) {
            case STREAM:
                url = client.files().getTemporaryLink(filePath).getLink();
                break;
            case DOWNLOAD:
                url = client.files().getTemporaryLink(filePath).getLink();
                break;
            case VIEW:
                url = client.files().getTemporaryLink(filePath).getLink();
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
            DbxDownloader<?> downloader = getDropboxClient(getCredential(fileInfo)).files()
                    .getThumbnailBuilder(filePath)
                    .withFormat(ThumbnailFormat.JPEG)
                    .withSize(ThumbnailSize.W64H64)
                    .start();
            if (downloader == null) {
                return null;
            }
            return downloader.getInputStream();
        } catch (DbxException e) {
            log.warn(String.format("Failed to get thumbnail for file %s", filePath), e);
            return null;
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

    protected DbxClientV2 getDropboxClient(Credential credential) throws IOException {
        return getDropboxClient(credential.getAccessToken());
    }

    protected DbxClientV2 getDropboxClient(String accessToken) throws IOException {
        DbxRequestConfig config = new DbxRequestConfig(APPLICATION_NAME, Locale.getDefault().toString());
        return new DbxClientV2(config, accessToken);
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
            FileMetadata fileMetadata = getDropboxClient(getCredential(fileInfo))
                    .files()
                    .download(fileInfo.getFileId())
                    .getResult();

            if (fileMetadata == null) {
                return null;
            }
            return new DropboxLiveConnectFile(fileInfo, fileMetadata);
        } catch (DbxException e) {
            throw new IOException("Failed to retrieve Dropbox file metadata", e);
        }
    }

}
