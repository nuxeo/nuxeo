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
import com.box.sdk.BoxFile.Info;
import com.google.api.client.auth.oauth2.Credential;

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
        String url = null;
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

    protected ManagedBlob createBlob(LiveConnectFileInfo fileInfo) throws IOException {
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
        key.append(':');
        key.append(fileInfo.getUser());
        key.append(':');
        key.append(fileInfo.getFileId());
        if (fileInfo.getRevisionId().isPresent()) {
            key.append(':');
            key.append(fileInfo.getRevisionId().get());
        }
        return key.toString();
    }

    protected Credential getCredential(String user) throws IOException {
        return getCredentialFactory().build(user);
    }

    protected OAuthCredentialFactory getCredentialFactory() {
        return new OAuthCredentialFactory(getOAuth2Provider());
    }

    protected BoxAPIConnection getBoxClient(NuxeoOAuth2Token token) throws IOException {
        return getBoxClient(getCredential(token.getServiceLogin()));
    }

    protected BoxAPIConnection getBoxClient(Credential credential) throws IOException {
        Long expiresInSeconds = credential.getExpiresInSeconds();
        if (expiresInSeconds != null && expiresInSeconds <= 0) {
            credential.refreshToken();
        }
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
     * @param fileInfo The file info.
     * @return The {@link LiveConnectFile} from cache, if it doesn't exist retrieves it with API and cache it.
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
     * @param fileInfo The file info.
     * @return The file retrieved from API.
     */
    private LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException {
        try {
            Info boxFile = new BoxFile(getBoxClient(getCredential(fileInfo.getUser())), fileInfo.getFileId()).getInfo();
            return new BoxLiveConnectFile(boxFile);
        } catch (BoxAPIException e) {
            throw new IOException("Failed to retrieve Box file metadata", e);
        }
    }

    private Cache getFileCache() {
        if (cache == null) {
            cache = Framework.getService(CacheService.class).getCache(FILE_CACHE_NAME);
        }
        return cache;
    }

}
