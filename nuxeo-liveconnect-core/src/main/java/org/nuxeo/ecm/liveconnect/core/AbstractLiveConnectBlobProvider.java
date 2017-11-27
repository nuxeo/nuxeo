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
package org.nuxeo.ecm.liveconnect.core;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.blob.AbstractBlobProvider;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.DocumentBlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.liveconnect.update.BatchUpdateBlobProvider;
import org.nuxeo.ecm.liveconnect.update.worker.BlobProviderDocumentsUpdateWork;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.base.Splitter;

/**
 * Basic implementation of {@link BlobProvider} for live connect.
 *
 * @param <O> The OAuth2 service provider type.
 * @since 8.1
 */
public abstract class AbstractLiveConnectBlobProvider<O extends OAuth2ServiceProvider> extends AbstractBlobProvider
        implements LiveConnectBlobProvider<O>, BatchUpdateBlobProvider, DocumentBlobProvider {

    private static final Log log = LogFactory.getLog(AbstractLiveConnectBlobProvider.class);

    private static final String FILE_CACHE_PREFIX = "liveconnect_file_";

    private static final char BLOB_KEY_SEPARATOR = ':';

    static {
        ComplexTypeJSONDecoder.registerBlobDecoder(new JSONLiveConnectBlobDecoder());
    }

    /** Resource cache */
    private Cache cache;

    /**
     * Should be overriden by subclasses needing something different.
     */
    @Override
    public void close() {
    }

    /**
     * Should be overriden by subclasses needing something different.
     */
    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        return toBlob(toFileInfo(blobInfo.key));
    }

    /**
     * Should be overriden by subclasses needing something different.
     */
    @Override
    public String writeBlob(Blob blob) throws IOException {
        throw new UnsupportedOperationException("Writing a blob to live connect service is not supported");
    }

    @Override
    public boolean performsExternalAccessControl(BlobInfo blobInfo) {
        return true;
    }

    /**
     * Should be overriden by subclasses needing something different.
     */
    @Override
    public boolean isVersion(ManagedBlob blob) {
        return toFileInfo(blob).getRevisionId().isPresent();
    }

    @Override
    public List<DocumentModel> checkChangesAndUpdateBlob(List<DocumentModel> docs) {
        List<DocumentModel> changedDocuments = new ArrayList<>();
        for (DocumentModel doc : docs) {
            final SimpleManagedBlob blob = (SimpleManagedBlob) doc.getProperty("content").getValue();
            if (blob == null || isVersion(blob)) {
                continue;
            }
            LiveConnectFileInfo fileInfo = toFileInfo(blob);
            try {
                LiveConnectFile file = retrieveFile(fileInfo);
                putFileInCache(file);
                if (hasChanged(blob, file)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Updating blob=" + blob.key);
                    }
                    doc.setPropertyValue("content", toBlob(file));
                    changedDocuments.add(doc);
                }
            } catch (IOException e) {
                log.error("Could not update document=" + fileInfo, e);
            }

        }
        return changedDocuments;
    }

    @Override
    public void processDocumentsUpdate() {
        final RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        final WorkManager workManager = Framework.getLocalService(WorkManager.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            try (CoreSession session = CoreInstance.openCoreSessionSystem(repositoryName)) {

                long offset = 0;
                List<DocumentModel> nextDocumentsToBeUpdated;
                PageProviderService ppService = Framework.getService(PageProviderService.class);
                Map<String, Serializable> props = new HashMap<>();
                props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
                @SuppressWarnings("unchecked")
                PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(
                        getPageProviderNameForUpdate(), null, null, null, props);
                final long maxResult = pp.getPageSize();
                do {
                    pp.setCurrentPageOffset(offset);
                    pp.refresh();
                    nextDocumentsToBeUpdated = pp.getCurrentPage();

                    if (nextDocumentsToBeUpdated.isEmpty()) {
                        break;
                    }
                    List<String> docIds = nextDocumentsToBeUpdated.stream().map(DocumentModel::getId).collect(
                            Collectors.toList());
                    BlobProviderDocumentsUpdateWork work = new BlobProviderDocumentsUpdateWork(
                            buildWorkId(repositoryName, offset), blobProviderId);
                    work.setDocuments(repositoryName, docIds);
                    workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);
                    offset += maxResult;
                } while (nextDocumentsToBeUpdated.size() == maxResult);

            }
        }
    }

    private String buildWorkId(String repositoryName, long offset) {
        return blobProviderId + ':' + repositoryName + ':' + offset;
    }

    /**
     * Should be overriden by subclasses wanting to rely on a different fields.
     */
    protected boolean hasChanged(SimpleManagedBlob blob, LiveConnectFile file) {
        return StringUtils.isBlank(blob.getDigest()) || !blob.getDigest().equals(file.getDigest());
    }

    @Override
    @SuppressWarnings("unchecked")
    public O getOAuth2Provider() {
        return (O) Framework.getLocalService(OAuth2ServiceProviderRegistry.class).getProvider(blobProviderId);
    }

    @Override
    public SimpleManagedBlob toBlob(LiveConnectFileInfo fileInfo) throws IOException {
        LiveConnectFile file;
        try {
            file = getFile(fileInfo);
        } catch (IOException e) {
            // we don't want to crash everything if the remote file cannot be accessed
            log.error("Failed to access file: " + fileInfo, e);
            file = new ErrorLiveConnectFile(fileInfo);
        }
        return toBlob(file);
    }

    protected SimpleManagedBlob toBlob(LiveConnectFile file) {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = buildBlobKey(file.getInfo());
        blobInfo.mimeType = file.getMimeType();
        blobInfo.encoding = file.getEncoding();
        blobInfo.filename = file.getFilename().replace('/', '-');
        blobInfo.length = Long.valueOf(file.getFileSize());
        blobInfo.digest = file.getDigest();
        return new SimpleManagedBlob(blobInfo);
    }

    protected String buildBlobKey(LiveConnectFileInfo fileInfo) {
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
        return toFileInfo(blob.getKey());
    }

    /**
     * @since 8.4
     */
    protected LiveConnectFileInfo toFileInfo(String key) {
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

    /**
     * Returns the {@link LiveConnectFile} from cache, if it doesn't exist retrieves it with API and cache it.
     *
     * @param fileInfo the file info
     * @return the {@link LiveConnectFile} from cache, if it doesn't exist retrieves it with API and cache it
     */
    protected LiveConnectFile getFile(LiveConnectFileInfo fileInfo) throws IOException {
        LiveConnectFile file = getFileFromCache(fileInfo);
        if (file == null) {
            file = retrieveFile(fileInfo);
            putFileInCache(file);
        }
        return file;
    }

    private Cache getCache() {
        if (cache == null) {
            cache = Framework.getService(CacheService.class).getCache(getCacheName());
        }
        return cache;
    }

    protected Credential getCredential(LiveConnectFileInfo fileInfo) throws IOException {
        return getCredential(fileInfo.getUser());
    }

    protected Credential getCredential(NuxeoOAuth2Token token) throws IOException {
        return getCredential(token.getServiceLogin());
    }

    public final synchronized Credential getCredential(String user) throws IOException {
        // suspend current transaction and start a new one responsible for access token update
        try {
            return TransactionHelper.runInNewTransaction(() -> retrieveAndRefreshCredential(user));
        } catch (UncheckedIOException uioe) {
            // re-throw IOException because methods implementing interface throwing IOException use this method
            throw uioe.getCause();
        }
    }

    /**
     * Declare this method as {@code private} because we don't want upper class to override it and as there's concurrent
     * access on it, don't let anyone calling it.
     */
    private Credential retrieveAndRefreshCredential(String user) {
        try {
            Credential credential = getCredentialFactory().build(user);
            if (credential == null) {
                throw new NuxeoException(
                        "No credentials found for user " + user + " and service " + blobProviderId);
            }
            Long expiresInSeconds = credential.getExpiresInSeconds();
            if (expiresInSeconds != null && expiresInSeconds.longValue() <= 0) {
                credential.refreshToken();
            }
            return credential;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Should be overriden by subclasses needing another credential factory.
     */
    protected CredentialFactory getCredentialFactory() {
        return new OAuth2CredentialFactory(getOAuth2Provider());
    }

    @SuppressWarnings("unchecked")
    protected final <T extends Serializable> T getFromCache(String key) {
        return (T) getCache().get(key);
    }

    protected final <T extends Serializable> void putInCache(String key, T object) {
        getCache().put(key, object);
    }

    protected final void invalidateInCache(LiveConnectFileInfo fileInfo) {
        getCache().invalidate(FILE_CACHE_PREFIX + buildBlobKey(fileInfo));
    }

    protected final LiveConnectFile getFileFromCache(LiveConnectFileInfo fileInfo) {
        return getFromCache(FILE_CACHE_PREFIX + buildBlobKey(fileInfo));
    }

    protected final void putFileInCache(LiveConnectFile file) {
        putInCache(FILE_CACHE_PREFIX + buildBlobKey(file.getInfo()), file);
    }

    /**
     * Parse a {@link URI}.
     *
     * @return the {@link URI} or null if it fails
     */
    protected URI asURI(String link) {
        try {
            return new URI(link);
        } catch (URISyntaxException e) {
            log.error("Invalid URI: " + link, e);
            return null;
        }
    }

    protected abstract String getCacheName();

    protected abstract String getPageProviderNameForUpdate();

    /**
     * Retrieves the file with API.
     *
     * @param fileInfo the file info
     * @return the file retrieved from API
     */
    protected abstract LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException;

}
