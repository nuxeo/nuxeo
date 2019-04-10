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
 *     Andre Justo
 */
package org.nuxeo.ecm.liveconnect.dropbox;

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

import org.apache.commons.lang3.StringUtils;
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
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Provider for blobs getting information from Dropbox.
 *
 * @since 7.3
 */
public class DropboxBlobProvider extends AbstractBlobProvider implements BatchUpdateBlobProvider {

    private static final Log log = LogFactory.getLog(DropboxBlobProvider.class);

    private static final String APPLICATION_NAME = "Nuxeo/0";

    private static final String FILE_CACHE_NAME = "dropbox";

    private static final String DROPBOX_DOCUMENT_TO_BE_UPDATED_PP = "dropbox_document_to_be_updated";

    /**
     * {@link DbxEntry.File} resource cache
     */
    private Cache fileCache;

    @Override
    public void close() {
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        return new SimpleManagedBlob(blobInfo);
    }

    @Override
    public boolean supportsUserUpdate() {
        return supportsUserUpdateDefaultTrue();
    }

    @Override
    public String writeBlob(Blob blob, Document doc) {
        throw new UnsupportedOperationException("Writing a blob to Dropbox is not supported");
    }

    @Override
    public URI getURI(ManagedBlob blob, UsageHint usage, HttpServletRequest servletRequest) throws IOException {
        String url = null;
        String fileInfo = getFileInfo(blob.getKey());
        String user = getUser(fileInfo);
        String filePath = getFilePath(fileInfo);
        DbxClient client = getDropboxClient(getCredential(user));
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
        return url != null ? asURI(url) : null;
    }

    protected InputStream getStream(URI uri) throws IOException {
        return doGet(uri);
    }

    @Override
    public Map<String, URI> getAvailableConversions(ManagedBlob blob, UsageHint hint) throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public InputStream getThumbnail(ManagedBlob blob) throws IOException {
        String fileInfo = getFileInfo(blob.getKey());
        String user = getUser(fileInfo);
        String filePath = getFilePath(fileInfo);
        try {
            DbxClient.Downloader downloader = getDropboxClient(getCredential(user)).startGetThumbnail(DbxThumbnailSize.w64h64,
                DbxThumbnailFormat.bestForFileName(filePath, DbxThumbnailFormat.JPEG), filePath, null);

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
        return uri == null ? null : getStream(uri);
    }

    @Override
    public InputStream getConvertedStream(ManagedBlob blob, String mimeType, DocumentModel doc) throws IOException {
        Map<String, URI> conversions = getAvailableConversions(blob, UsageHint.STREAM);
        URI uri = conversions.get(mimeType);
        if (uri == null) {
            return null;
        }
        return getStream(uri);
    }

    @Override
    public ManagedBlob freezeVersion(ManagedBlob blob, Document doc) throws IOException {
        return null;
    }

    /**
     * Gets the blob for a Dropbox file.
     *
     * @param fileInfo the file info ({email}:{filePath})
     * @return the blob
     */
    protected Blob getBlob(String fileInfo) throws IOException {
        String user = getUser(fileInfo);
        String filePath = getFilePath(fileInfo);
        DbxEntry.File file = getFile(user, filePath);
        String key = String.format("%s:%s:%s", blobProviderId, user, filePath);
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = key;
        blobInfo.filename = file.name;
        blobInfo.length = file.numBytes;
        blobInfo.mimeType = getMimetypeFromFilename(file.name);
        blobInfo.encoding = null;
        blobInfo.digest = file.rev;
        return new SimpleManagedBlob(blobInfo);
    }

    /**
     * Removes the prefix from the key.
     */
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

    protected String getFilePath(String fileInfo) {
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
        return getCredentialFactory().build(user);
    }

    protected OAuthCredentialFactory getCredentialFactory() {
        return new OAuthCredentialFactory(getOAuth2Provider());
    }

    protected DbxClient getDropboxClient(Credential credential) throws IOException {
        return getDropboxClient(credential.getAccessToken());
    }

    protected DbxClient getDropboxClient(String accessToken) throws IOException {
        DbxRequestConfig config = new DbxRequestConfig(APPLICATION_NAME, Locale.getDefault().toString());
        return new DbxClient(config, accessToken);
    }

    /**
     * Retrieves and caches a {@link DbxEntry.File} resource.
     *
     * @return a {@link DbxEntry.File} resource.
     */
    protected DbxEntry.File getFile(String user, String filePath) throws IOException {
        DbxEntry.File fileResource = (DbxEntry.File) getFileCache().get(filePath);
        if (fileResource == null) {
            try {
                DbxEntry fileMetadata = getDropboxClient(getCredential(user)).getMetadata(filePath);
                if (fileMetadata == null) {
                    return null;
                }
                fileResource = fileMetadata.asFile();
                getFileCache().put(filePath, fileResource);
            } catch (DbxException e) {
                throw new IOException("Failed to get Dropbox file metadata " + e);
            }
        }
        return fileResource;
    }

    /**
     * Executes a GET request
     */
    protected InputStream doGet(URI url) throws IOException {
        HttpRequestFactory requestFactory = getOAuth2Provider().getRequestFactory();
        HttpResponse response = requestFactory.buildGetRequest(new GenericUrl(url)).execute();
        return response.getContent();
    }

    /**
     * Parse a {@link URI}.
     *
     * @return the {@link URI} or null if it fails
     */
    protected static URI asURI(String link) {
        try {
            return new URI(link);
        } catch (URISyntaxException e) {
            log.error("Invalid URI: " + link, e);
            return null;
        }
    }

    protected String getClientId() {
        OAuth2ServiceProvider provider = getOAuth2Provider();
        return provider != null ? provider.getClientId() : null;
    }

    private Cache getFileCache() {
        if (fileCache == null) {
            fileCache = Framework.getService(CacheService.class).getCache(FILE_CACHE_NAME);
        }
        return fileCache;
    }

    protected DropboxOAuth2ServiceProvider getOAuth2Provider() {
        return (DropboxOAuth2ServiceProvider) Framework.getLocalService(
            OAuth2ServiceProviderRegistry.class).getProvider(blobProviderId);
    }

    private String getMimetypeFromFilename(String filename) {
        MimetypeRegistryService mimetypeRegistryService = (MimetypeRegistryService) Framework.getLocalService(
            MimetypeRegistry.class);
        return mimetypeRegistryService.getMimetypeFromFilename(filename);
    }

    @Override
    public List<DocumentModel> checkChangesAndUpdateBlob(List<DocumentModel> docs) {
        List<DocumentModel> changedDocuments = new ArrayList<>();
        for (DocumentModel doc : docs) {
            final SimpleManagedBlob blob = (SimpleManagedBlob) doc.getProperty("content").getValue();
            if (blob == null) {
                continue;
            }
            if (isVersion(blob)) {
                continue;
            }
            String fileInfo = getFileInfo(blob.key);
            String user = getUser(fileInfo);
            String filePath = getFilePath(fileInfo);
            try {
                DbxEntry.File file = getFileNoCache(user, filePath);
                if (StringUtils.isBlank(blob.getDigest()) || !blob.getDigest().equals(file.rev)) {
                    log.trace("Updating " + blob.key);
                    getFileCache().invalidate(filePath);
                    doc.setPropertyValue("content", (SimpleManagedBlob) getBlob(fileInfo));
                    changedDocuments.add(doc);
                }

            } catch (DbxException | IOException e) {
                log.error("Could not update dropbox document " + filePath, e);
            }
        }
        return changedDocuments;
    }

    protected DbxEntry.File getFileNoCache(String user, String filePath) throws DbxException, IOException {
        DbxEntry fileMetadata = getDropboxClient(getCredential(user)).getMetadata(filePath);
        if (fileMetadata == null) {
            return null;
        }
        DbxEntry.File file = fileMetadata.asFile();
        return file;
    }

    @Override
    public String getPageProviderNameForUpdate() {
        return DROPBOX_DOCUMENT_TO_BE_UPDATED_PP;
    }

    @Override
    public String getBlobProviderId() {
        return blobProviderId;
    }

}
