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
package org.nuxeo.ecm.liveconnect.google.drive;

import static java.lang.Boolean.TRUE;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.liveconnect.google.drive.credential.CredentialFactory;
import org.nuxeo.ecm.liveconnect.google.drive.credential.OAuth2CredentialFactory;
import org.nuxeo.ecm.liveconnect.google.drive.credential.ServiceAccountCredentialFactory;
import org.nuxeo.ecm.liveconnect.update.BatchUpdateBlobProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ObjectParser;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.model.App;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.RevisionList;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Provider for blobs getting information from Google Drive.
 *
 * @since 7.3
 */
public class GoogleDriveBlobProvider implements BlobProvider, BatchUpdateBlobProvider {

    private static final String GOOGLEDRIVE_DOCUMENT_TO_BE_UPDATED_PP = "googledrive_document_to_be_updated";

    public static final int PREFERRED_ICON_SIZE = 16;

    private static final Log log = LogFactory.getLog(GoogleDriveBlobProvider.class);

    /**
     * Information about a file stored in Google Drive.
     */
    public static class FileInfo {
        public final String user;

        public final String fileId;

        public final String revisionId;

        public FileInfo(String user, String fileId, String revisionId) {
            this.user = user;
            this.fileId = fileId;
            this.revisionId = revisionId;
        }
    }

    public static final String PREFIX = "googledrive";

    private static final String APPLICATION_NAME = "Nuxeo/0";

    private static final String FILE_CACHE_NAME = "googleDrive";

    // Service account details
    public static final String SERVICE_ACCOUNT_ID_PROP = "serviceAccountId";

    public static final String SERVICE_ACCOUNT_P12_PATH_PROP = "serviceAccountP12Path";

    // ClientId for the file picker auth
    public static final String CLIENT_ID_PROP = "clientId";

    public static final String DEFAULT_EXPORT_MIMETYPE = "application/pdf";

    // Blob conversion constants
    protected static final String BLOB_CONVERSIONS_FACET = "BlobConversions";

    protected static final String BLOB_CONVERSIONS_PROPERTY = "blobconversions:conversions";

    protected static final String BLOB_CONVERSION_KEY = "key";

    protected static final String BLOB_CONVERSION_BLOB = "blob";

    protected static final ObjectParser JSON_PARSER = new JsonObjectParser(JacksonFactory.getDefaultInstance());

    private String serviceAccountId;

    private java.io.File serviceAccountP12File;

    private String clientId;

    /** resource cache */
    private Cache cache;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        if (!PREFIX.equals(blobProviderId)) {
            // TODO avoid this by passing a parameter to the GoogleDriveBlobUploader when constructed
            throw new IllegalArgumentException("Must be registered for name: " + PREFIX + ", not: " + blobProviderId);
        }
        // Validate service account configuration
        serviceAccountId = properties.get(SERVICE_ACCOUNT_ID_PROP);
        if (StringUtils.isBlank(serviceAccountId)) {
            return;
        }
        String p12 = properties.get(SERVICE_ACCOUNT_P12_PATH_PROP);
        if (StringUtils.isBlank(p12)) {
            throw new NuxeoException("Missing value for property: " + SERVICE_ACCOUNT_P12_PATH_PROP);
        }
        serviceAccountP12File = new java.io.File(p12);
        if (!serviceAccountP12File.exists()) {
            throw new NuxeoException("No such file: " + p12 + " for property: " + SERVICE_ACCOUNT_P12_PATH_PROP);
        }

        clientId = properties.get(CLIENT_ID_PROP);
        if (StringUtils.isBlank(clientId)) {
            throw new NuxeoException("Missing value for property: " + CLIENT_ID_PROP);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) {
        return new SimpleManagedBlob(blobInfo);
    }

    @Override
    public boolean supportsWrite() {
        return false;
    }

    @Override
    public String writeBlob(Blob blob, Document doc) {
        throw new UnsupportedOperationException("Writing a blob to Google Drive is not supported");
    }

    @Override
    public URI getURI(ManagedBlob blob, UsageHint usage, HttpServletRequest servletRequest) throws IOException {
        String url = null;
        switch (usage) {
        case STREAM:
            url = getStreamUrl(blob);
            break;
        case DOWNLOAD:
            url = getDownloadUrl(blob);
            break;
        case VIEW:
        case EDIT:
            url = getAlternateUrl(blob);
            break;
        case EMBED:
            url = getEmbedUrl(blob);
            break;
        }
        return url == null ? null : asURI(url);
    }

    // TODO remove unused hint from signature
    @Override
    public Map<String, URI> getAvailableConversions(ManagedBlob blob, UsageHint hint) throws IOException {
        Map<String, String> exportLinks = getExportLinks(blob);
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
    public InputStream getThumbnail(ManagedBlob blob) throws IOException {
        String url = getThumbnailUrl(blob);
        return getStream(blob, asURI(url));
    }

    /**
     * Gets the URL from which we can stream the content of the file.
     * <p>
     * Will return {@code null} if this is a native Google document.
     */
    protected String getStreamUrl(ManagedBlob blob) throws IOException {
        FileInfo fileInfo = getFileInfo(blob);
        if (fileInfo.revisionId == null) {
            File file = getFile(fileInfo);
            return file.getDownloadUrl();
        } else {
            Revision revision = getRevision(fileInfo);
            return revision != null ? revision.getDownloadUrl() : null;
        }
    }

    /**
     * Gets the URL to which we can redirect to let the user download the file.
     */
    protected String getDownloadUrl(ManagedBlob blob) throws IOException {
        FileInfo fileInfo = getFileInfo(blob);
        String url = null;
        if (fileInfo.revisionId == null) {
            File file = getFile(fileInfo);
            url = file.getWebContentLink();
            if (url == null) {
                // native Google document
                url = file.getAlternateLink();
            }
        } else {
            Revision revision = getRevision(fileInfo);
            if (revision != null) {
                url = revision.getDownloadUrl();
                if (StringUtils.isBlank(url)) {
                    url = revision.getExportLinks().get(DEFAULT_EXPORT_MIMETYPE);
                }
                // hack, without this we get a 401 on the returned URL...
                if (url.endsWith("&gd=true")) {
                    url = url.substring(0, url.length() - "&gd=true".length());
                }
            }
        }
        return url;
    }

    // TODO remove
    protected String getAlternateUrl(ManagedBlob blob) throws IOException {
        FileInfo fileInfo = getFileInfo(blob);
        // ignore revisionId
        File file = getFile(fileInfo);
        return file.getAlternateLink();
    }

    /**
     * Gets the URL to which we can redirect to let the user see a preview of the file.
     */
    protected String getEmbedUrl(ManagedBlob blob) throws IOException {
        FileInfo fileInfo = getFileInfo(blob);
        // ignore revisionId
        File file = getFile(fileInfo);
        String url = file.getEmbedLink();
        if (url == null) {
            // uploaded file, switch to preview
            url = file.getAlternateLink();
            url = asURI(url).resolve("./preview").toString();
        }
        return url;
    }

    /**
     * Gets the URL from which we can stream a thumbnail.
     */
    protected String getThumbnailUrl(ManagedBlob blob) throws IOException {
        FileInfo fileInfo = getFileInfo(blob);
        // ignore revisionId
        File file = getFile(fileInfo);
        return file.getThumbnailLink();
    }

    /**
     * Gets the export link.
     */
    protected Map<String, String> getExportLinks(ManagedBlob blob) throws IOException {
        FileInfo fileInfo = getFileInfo(blob);
        if (fileInfo.revisionId == null) {
            File file = getFile(fileInfo);
            return file.getExportLinks();
        } else {
            Revision revision = getRevision(fileInfo);
            return revision != null && TRUE.equals(revision.getPinned()) ?
                revision.getExportLinks() : Collections.emptyMap();
        }
    }

    @Override
    public InputStream getStream(ManagedBlob blob) throws IOException {
        URI uri = getURI(blob, UsageHint.STREAM, null);
        return uri == null ? null : getStream(blob, uri);
    }

    @Override
    public InputStream getConvertedStream(ManagedBlob blob, String mimeType, DocumentModel doc) throws IOException {
        Blob conversion = retrieveBlobConversion(blob, mimeType, doc);
        if (conversion != null) {
            return conversion.getStream();
        }

        Map<String, URI> conversions = getAvailableConversions(blob, UsageHint.STREAM);
        URI uri = conversions.get(mimeType);
        if (uri == null) {
            return null;
        }
        return getStream(blob, uri);
    }

    protected InputStream getStream(ManagedBlob blob, URI uri) throws IOException {
        FileInfo fileInfo = getFileInfo(blob);
        return doGet(fileInfo.user, uri);
    }

    @Override
    public List<AppLink> getAppLinks(String username, ManagedBlob blob) throws IOException {
        List<AppLink> appLinks = new ArrayList<>();

        FileInfo fileInfo = getFileInfo(blob);

        // application links do not work with revisions
        if (fileInfo.revisionId != null) {
            return appLinks;
        }

        // retrieve the service's user (email in this case) for this username
        String user = getServiceUser(username);

        // fetch a partial file response
        File file = getPartialFile(user, fileInfo.fileId, "openWithLinks", "defaultOpenWithLink");
        if (file.isEmpty()) {
            return appLinks;
        }

        // build the list of AppLinks
        String defaultLink = file.getDefaultOpenWithLink();
        for (Map.Entry<String, String> entry : file.getOpenWithLinks().entrySet()) {
            // build the AppLink
            App app = getApp(user, entry.getKey());
            AppLink appLink = new AppLink();
            appLink.setAppName(app.getName());
            appLink.setLink(entry.getValue());

            // pick an application icon
            for (com.google.api.services.drive.model.App.Icons icon : app.getIcons()) {
                if ("application".equals(icon.getCategory())) {
                    appLink.setIcon(icon.getIconUrl());
                    // break if we've got one with our preferred size
                    if (icon.getSize() == PREFERRED_ICON_SIZE) {
                        break;
                    }
                }
            }

            // add the default link first
            if (defaultLink != null && defaultLink.equals(entry.getValue())) {
                appLinks.add(0, appLink);
            } else {
                appLinks.add(appLink);
            }
        }
        return appLinks;
    }

    protected String getServiceUser(String username) {
        CredentialFactory credentialFactory = getCredentialFactory();
        if (credentialFactory instanceof OAuth2CredentialFactory) {
            OAuth2ServiceProvider provider = ((OAuth2CredentialFactory) credentialFactory).getProvider();
            return ((GoogleOAuth2ServiceProvider) provider).getServiceUser(username);
        } else {
            UserManager userManager = Framework.getLocalService(UserManager.class);
            DocumentModel user = userManager.getUserModel(username);
            if (user == null) {
                return null;
            }
            return (String) user.getPropertyValue(userManager.getUserEmailField());
        }
    }

    protected App getApp(String user, String appId) throws IOException {
        String cacheKey = "app_" + appId;
        return executeAndCache(cacheKey, getService(user).apps().get(appId), App.class);
    }

    @Override
    public ManagedBlob freezeVersion(ManagedBlob blob, Document doc) throws IOException {
        FileInfo fileInfo = getFileInfo(blob);
        if (fileInfo.revisionId != null) {
            // already frozen
            return null;
        }
        String user = fileInfo.user;
        String fileId = fileInfo.fileId;
        // find current revision for that doc
        File file = getFile(fileInfo);
        String revisionId = file.getHeadRevisionId();
        if (revisionId != null) {
            // uploaded file, there is a head revision
            fileInfo = new FileInfo(user, fileId, revisionId);
            Revision revision = getRevision(fileInfo);
            if (!TRUE.equals(revision.getPinned())) {
                // pin the revision
                Revision pinRevision = new Revision();
                pinRevision.setPinned(TRUE);
                getService(user).revisions().patch(fileId, revisionId, pinRevision).executeUnparsed().ignore();
            }
        } else {
            // native Google document
            // find last revision
            List<Revision> list = getRevisionList(fileInfo).getItems();
            if (list.isEmpty()) {
                return null;
            }
            Revision revision = list.get(list.size() - 1);

            // native Google document revision cannot be pinned so we store a conversion of the blob
            URI uri = asURI(revision.getExportLinks().get(DEFAULT_EXPORT_MIMETYPE));

            InputStream is = doGet(user, uri);
            Blob conversion = Blobs.createBlob(is);
            conversion.setFilename(blob.getFilename());
            conversion.setMimeType(DEFAULT_EXPORT_MIMETYPE);

            fileInfo = new FileInfo(user, fileId, revision.getId());

            // store a conversion of this revision
            storeBlobConversion(doc, getKey(fileInfo), conversion);
        }
        return getBlob(fileInfo);
    }

    /**
     * Store a conversion of the given blob
     */
    @SuppressWarnings("unchecked")
    protected void storeBlobConversion(Document doc, String blobKey, Blob blob) {
        if (!doc.hasFacet(BLOB_CONVERSIONS_FACET)) {
            doc.addFacet(BLOB_CONVERSIONS_FACET);
        }

        List<Map<String, Object>> conversions = (List<Map<String, Object>>) doc.getValue(BLOB_CONVERSIONS_PROPERTY);
        Map<String, Object> conversion = new HashMap<>();
        conversion.put(BLOB_CONVERSION_KEY, blobKey);
        conversion.put(BLOB_CONVERSION_BLOB, blob);
        conversions.add(conversion);
        doc.setValue(BLOB_CONVERSIONS_PROPERTY, conversions);
    }

    /**
     * Retrieve a stored conversion of the given blob
     */
    protected Blob retrieveBlobConversion(ManagedBlob blob, String mimeType, DocumentModel doc) {
        if (doc == null || !doc.hasFacet(BLOB_CONVERSIONS_FACET)) {
            return null;
        }

        boolean txWasActive = TransactionHelper.isTransactionActiveOrMarkedRollback();
        try {
            if (!txWasActive) {
                TransactionHelper.startTransaction();
            }
            ListProperty conversions = (ListProperty) doc.getProperty(BLOB_CONVERSIONS_PROPERTY);
            for (int i = 0; i < conversions.size(); i++) {
                if (blob.getKey().equals(conversions.get(i).getValue(BLOB_CONVERSION_KEY))) {
                    String conversionXPath = String.format("%s/%d/%s", BLOB_CONVERSIONS_PROPERTY, i, BLOB_CONVERSION_BLOB);
                    Blob conversion = (Blob) doc.getPropertyValue(conversionXPath);
                    if (conversion.getMimeType().equals(mimeType)) {
                        return conversion;
                    }
                }
            }
        } finally {
            if (!txWasActive) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
        return null;
    }

    /**
     * Gets the blob for a Google Drive file.
     *
     * @param fileInfo the file info
     * @return the blob
     */
    protected ManagedBlob getBlob(FileInfo fileInfo) throws IOException {
        String key = getKey(fileInfo);
        File file = getFile(fileInfo);
        String filename = file.getTitle().replace("/", "-");
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = key;
        blobInfo.mimeType = file.getMimeType();
        blobInfo.encoding = null; // TODO extract from mimeType
        blobInfo.filename = filename;
        blobInfo.length = file.getFileSize();
        // etag for native Google documents and md5 for everything else
        String digest = getDigest(file);
        blobInfo.digest = digest;
        return new SimpleManagedBlob(blobInfo);
    }

    protected String getDigest(File file) {
        String digest = file.getMd5Checksum();
        if (digest == null) {
            digest = file.getEtag();
        }
        return digest;
    }

    protected boolean isDigestChanged(Blob blob, File file) {
        final String digest = blob.getDigest();
        String md5CheckSum = file.getMd5Checksum();
        String eTag = file.getEtag();
        if (md5CheckSum != null) {
            return !md5CheckSum.equals(digest);
        } else {
            return eTag != null && !eTag.equals(digest);
        }
    }

    protected boolean isFilenameChanged(Blob blob, File file) {
        return !file.getTitle().replace("/", "-").equals(blob.getFilename());
    }

    protected boolean isChanged(Blob blob, File file) {
        return isFilenameChanged(blob, file) || isDigestChanged(blob, file);
    }

    /** Adds the prefix to the key. */
    protected String getKey(FileInfo fileInfo) {
        return PREFIX + ':' + fileInfo.user + ':' + fileInfo.fileId
                + (fileInfo.revisionId == null ? "" : ':' + fileInfo.revisionId);
    }

    /** Removes the prefix from the key. */
    protected FileInfo getFileInfo(ManagedBlob blob) {
        String key = blob.getKey();
        int colon = key.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException(key);
        }
        String suffix = key.substring(colon + 1);
        String[] parts = suffix.split(":");
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException(key);
        }
        return new FileInfo(parts[0], parts[1], parts.length < 3 ? null : parts[2]);
    }

    protected Credential getCredential(String user) throws IOException {
        return getCredentialFactory().build(user);
    }

    protected CredentialFactory getCredentialFactory() {
        OAuth2ServiceProvider provider = Framework.getLocalService(OAuth2ServiceProviderRegistry.class).getProvider(
                PREFIX);
        if (provider != null && provider.isEnabled()) {
            // Web application configuration
            return new OAuth2CredentialFactory(provider);
        } else {
            // Service account configuration
            return new ServiceAccountCredentialFactory(serviceAccountId, serviceAccountP12File);
        }
    }

    protected Drive getService(String user) throws IOException {
        Credential credential = getCredential(user);
        if (credential == null) {
            throw new IOException("No credentials found for user " + user);
        }
        HttpTransport httpTransport = credential.getTransport();
        JsonFactory jsonFactory = credential.getJsonFactory();
        return new Drive.Builder(httpTransport, jsonFactory, credential) //
        .setApplicationName(APPLICATION_NAME) // set application name to avoid a WARN
        .build();
    }

    /**
     * Retrieve a partial {@link File} resource.
     */
    protected File getPartialFile(String user, String fileId, String... fields) throws IOException {
        return getService(user).files().get(fileId).setFields(StringUtils.join(fields, ",")).execute();
    }

    /**
     * Retrieves a {@link File} resource and caches the unparsed response.
     *
     * @return a {@link File} resource
     */
    // subclassed for mock
    protected File getFile(FileInfo fileInfo) throws IOException {
        // ignore revisionId
        String cacheKey = "file_" + fileInfo.fileId;
        DriveRequest<File> request = getService(fileInfo.user).files().get(fileInfo.fileId);
        return executeAndCache(cacheKey, request, File.class);
    }

    /**
     * Retrieves a {@link Revision} resource and caches the unparsed response.
     *
     * @return a {@link Revision} resource
     */
    // subclassed for mock
    protected Revision getRevision(FileInfo fileInfo) throws IOException {
        if (fileInfo.revisionId == null) {
            throw new NullPointerException("null revisionId for " + fileInfo.fileId);
        }
        String cacheKey = "rev_" + fileInfo.fileId + "_" + fileInfo.revisionId;
        DriveRequest<Revision> request = getService(fileInfo.user).revisions().get(fileInfo.fileId,
                fileInfo.revisionId);
        try {
            return executeAndCache(cacheKey, request, Revision.class);
        } catch (HttpResponseException e) {
            // return null if revision is not found
            if (e.getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Executes a {@link DriveRequest} and caches the unparsed response.
     */
    protected <T> T executeAndCache(String cacheKey, DriveRequest<T> request, Class<T> aClass) throws IOException {
        String resource = (String) getCache().get(cacheKey);

        if (resource == null) {
            HttpResponse response = request.executeUnparsed();
            if (!response.isSuccessStatusCode()) {
                return null;
            }
            resource = response.parseAsString();
            if (cacheKey != null) {
                getCache().put(cacheKey, resource);
            }
        }
        return JSON_PARSER.parseAndClose(new StringReader(resource), aClass);
    }

    /**
     * Retrieves the list of {@link Revision} resources for a file.
     *
     * @return a list of {@link Revision} resources
     */
    // subclassed for mock
    protected RevisionList getRevisionList(FileInfo fileInfo) throws IOException {
        return getService(fileInfo.user).revisions().list(fileInfo.fileId).execute();
    }

    /**
     * Executes a GET request with the user's credentials.
     */
    protected InputStream doGet(String user, URI url) throws IOException {
        HttpResponse response = getService(user).getRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
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

    protected Cache getCache() {
        if (cache == null) {
            cache = Framework.getService(CacheService.class).getCache(FILE_CACHE_NAME);
        }
        return cache;
    }

    public String getClientId() {
        OAuth2ServiceProvider provider = getOAuth2Provider();
        return (provider != null && provider.isEnabled()) ? provider.getClientId() : clientId;
    }

    protected OAuth2ServiceProvider getOAuth2Provider() {
        return Framework.getLocalService(OAuth2ServiceProviderRegistry.class).getProvider(PREFIX);
    }

    @Override
    public List<DocumentModel> checkChangesAndUpdateBlob(List<DocumentModel> docs) {
        List<DocumentModel> changedDocuments = new ArrayList<>();
        // TODO use google batch request here
        for (DocumentModel doc : docs) {
            final SimpleManagedBlob blob = (SimpleManagedBlob) doc.getProperty("content").getValue();
            FileInfo fileInfo = getFileInfo(blob);
            if (isVersion(blob)) {
                // assume that revisions never change
                continue;
            }
            try {
                File remote = getPartialFile(fileInfo.user, fileInfo.fileId, "id", "title", "etag", "md5Checksum");
                if (isChanged(blob, remote)) {
                    doc.setPropertyValue("content", (SimpleManagedBlob) getBlob(getFileInfo(blob)));
                    String cacheKey = "file_" + fileInfo.fileId;
                    getCache().invalidate(cacheKey);
                    changedDocuments.add(doc);
                }
            } catch (IOException e) {
                log.error("Could not update google drive document " + doc.getTitle(), e);
            }

        }
        return changedDocuments;
    }

    @Override
    public String getPageProviderNameForUpdate() {
        return GOOGLEDRIVE_DOCUMENT_TO_BE_UPDATED_PP;
    }

    @Override
    public String getBlobPrefix() {
        return PREFIX;
    }

    @Override
    public boolean isVersion(ManagedBlob blob) {
        FileInfo fileInfo = getFileInfo(blob);
        return fileInfo.revisionId != null;
    }

}
