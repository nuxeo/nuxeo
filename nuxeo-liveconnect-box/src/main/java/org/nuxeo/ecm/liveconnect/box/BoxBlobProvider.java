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
package org.nuxeo.ecm.liveconnect.box;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.AbstractBlobProvider;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.liveconnect.update.BatchUpdateBlobProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.runtime.api.Framework;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxSharedLink;
import com.box.sdk.BoxSharedLink.Access;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.common.base.Splitter;

/**
 * Provider for blobs getting information from Box.
 *
 * @since 8.1
 */
public class BoxBlobProvider extends AbstractBlobProvider implements BatchUpdateBlobProvider {

    private static final Log log = LogFactory.getLog(BoxBlobProvider.class);

    private static final String APPLICATION_NAME = "Nuxeo/0";

    private static final String FILE_CACHE_NAME = "box";

    private static final String BOX_DOCUMENT_TO_BE_UPDATED_PP = "box_document_to_be_updated";

    private static final String DOWNLOAD_CONTENT_URL = "https://api.box.com/2.0/files/%s/content";

    private static final char BLOB_KEY_SEPARATOR = ':';

    /** Resource cache */
    private Cache cache;

    @Override
    public void close() {
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        return new SimpleManagedBlob(blobInfo);
    }

    @Override
    public String writeBlob(Blob blob, Document doc) throws IOException {
        throw new UnsupportedOperationException("Writing a blob to Box is not supported");
    }

    @Override
    public URI getURI(ManagedBlob blob, UsageHint usage, HttpServletRequest servletRequest) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        String url = null;
        switch (usage) {
        case STREAM:
        case DOWNLOAD:
            url = getDownloadUrl(fileInfo);
            break;
        case VIEW:
            url = retrieveSharedLink(fileInfo).getURL();
            break;
        case EMBED:
            // Soon supported, please see : NXP-
            break;
        }
        return Optional.ofNullable(url).flatMap(this::asURI).orElse(null);
    }

    @Override
    public List<DocumentModel> checkChangesAndUpdateBlob(List<DocumentModel> doc) {
        return null;
    }

    @Override
    public String getPageProviderNameForUpdate() {
        return BOX_DOCUMENT_TO_BE_UPDATED_PP;
    }

    @Override
    public String getBlobProviderId() {
        return blobProviderId;
    }

    protected ManagedBlob toBlob(LiveConnectFileInfo fileInfo) throws IOException {
        LiveConnectFile file = getFile(fileInfo);
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = buildBlobKey(fileInfo);
        blobInfo.mimeType = file.getMimeType();
        blobInfo.encoding = file.getEncoding();
        blobInfo.filename = file.getFilename().replace('/', '-');
        blobInfo.length = file.getFileSize();
        blobInfo.digest = file.getDigest();
        return new SimpleManagedBlob(blobInfo);
    }

    private String buildBlobKey(LiveConnectFileInfo fileInfo) {
        StringBuilder key = new StringBuilder(blobProviderId);
        key.append(BLOB_KEY_SEPARATOR);
        key.append(fileInfo.getUser());
        key.append(BLOB_KEY_SEPARATOR);
        key.append(fileInfo.getFileId());
        if (fileInfo.getRevisionId().isPresent()) {
            key.append(BLOB_KEY_SEPARATOR);
            key.append(fileInfo.getRevisionId().get());
        }
        return key.toString();
    }

    protected LiveConnectFileInfo toFileInfo(ManagedBlob blob) {
        String key = blob.getKey();
        List<String> keyParts = Splitter.on(BLOB_KEY_SEPARATOR).splitToList(key);
        // According to buildBlobKey we have :
        // 0 - blobProviderId
        // 1 - userId
        // 2 - fileId
        // 3 - revisionId (optional)
        if (keyParts.size() < 3 || keyParts.size() > 4) {
            throw new IllegalArgumentException("The key doesn't have a valid format=" + key);
        }
        return new LiveConnectFileInfo(keyParts.get(1), keyParts.get(2), keyParts.size() == 4 ? keyParts.get(3) : null);
    }

    protected Credential getCredential(String user) throws IOException {
        Credential credential = getCredentialFactory().build(user);
        Long expiresInSeconds = credential.getExpiresInSeconds();
        if (expiresInSeconds != null && expiresInSeconds <= 0) {
            credential.refreshToken();
        }
        return credential;
    }

    protected OAuthCredentialFactory getCredentialFactory() {
        return new OAuthCredentialFactory(getOAuth2Provider());
    }

    protected BoxAPIConnection getBoxClient(NuxeoOAuth2Token token) throws IOException {
        return getBoxClient(getCredential(token.getServiceLogin()));
    }

    protected BoxAPIConnection getBoxClient(Credential credential) throws IOException {
        return new BoxAPIConnection(credential.getAccessToken());
    }

    /**
     * Parse a {@link URI}.
     *
     * @return the {@link Optional}<{@link URI}> or {@link Optional#empty()} if it fails
     */
    protected Optional<URI> asURI(String link) {
        try {
            return Optional.of(new URI(link));
        } catch (URISyntaxException e) {
            log.error("Invalid URI: " + link, e);
            return Optional.empty();
        }
    }

    protected BoxOAuth2ServiceProvider getOAuth2Provider() {
        return (BoxOAuth2ServiceProvider) Framework.getLocalService(OAuth2ServiceProviderRegistry.class).getProvider(
                blobProviderId);
    }

    /**
     * Returns the {@link LiveConnectFile} from cache, if it doesn't exist retrieves it with API and cache it.
     *
     * @param fileInfo the file info
     * @return the {@link LiveConnectFile} from cache, if it doesn't exist retrieves it with API and cache it
     */
    protected LiveConnectFile getFile(LiveConnectFileInfo fileInfo) throws IOException {
        LiveConnectFile file = (LiveConnectFile) getFileCache().get(fileInfo.getFileId());
        if (file == null) {
            file = retrieveFile(fileInfo);
            getFileCache().put(fileInfo.getFileId(), file);
        }
        return file;
    }

    /**
     * Retrieves the file with API.
     *
     * @param fileInfo the file info
     * @return the file retrieved from API
     */
    protected LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException {
        try {
            return new BoxLiveConnectFile(fileInfo, prepareBoxFile(fileInfo).getInfo());
        } catch (BoxAPIException e) {
            throw new IOException("Failed to retrieve Box file metadata", e);
        }
    }

    private BoxSharedLink retrieveSharedLink(LiveConnectFileInfo fileInfo) throws IOException {
        try {
            return prepareBoxFile(fileInfo).createSharedLink(Access.OPEN, null, null);
        } catch (BoxAPIException e) {
            throw new IOException("Failed to retrieve Box shared link", e);
        }
    }

    private BoxFile prepareBoxFile(LiveConnectFileInfo fileInfo) throws IOException {
        BoxAPIConnection boxClient = getBoxClient(getCredential(fileInfo.getUser()));
        return new BoxFile(boxClient, fileInfo.getFileId());
    }

    /**
     * Returns the temporary download url for input file.
     *
     * @param fileInfo the file info
     * @return the temporary download url for input file
     */
    private String getDownloadUrl(LiveConnectFileInfo fileInfo) throws IOException {
        try {
            Credential credential = getCredential(fileInfo.getUser());

            GenericUrl url = new GenericUrl(String.format(DOWNLOAD_CONTENT_URL, fileInfo.getFileId()));
            HttpRequest request = getOAuth2Provider().getRequestFactory().buildGetRequest(url);
            request.setHeaders(new HttpHeaders().setAuthorization("Bearer " + credential.getAccessToken()));
            request.setFollowRedirects(false);
            request.setThrowExceptionOnExecuteError(false);

            HttpResponse response = request.execute();
            response.disconnect();
            if (!HttpStatusCodes.isRedirect(response.getStatusCode())) {
                throw new HttpResponseException(response);
            }

            return response.getHeaders().getLocation();
        } catch (BoxAPIException e) {
            throw new IOException("Failed to retrieve Box file download url.", e);
        }
    }

    private Cache getFileCache() {
        if (cache == null) {
            cache = Framework.getService(CacheService.class).getCache(FILE_CACHE_NAME);
        }
        return cache;
    }

}
