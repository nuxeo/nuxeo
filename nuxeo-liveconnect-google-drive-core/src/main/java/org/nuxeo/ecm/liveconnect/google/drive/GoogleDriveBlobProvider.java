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
 *     Florent Guillaume
 *     Nelson Silva
 */
package org.nuxeo.ecm.liveconnect.google.drive;

import static java.lang.Boolean.TRUE;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectBlobProvider;
import org.nuxeo.ecm.liveconnect.core.CredentialFactory;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.liveconnect.core.OAuth2CredentialFactory;
import org.nuxeo.ecm.liveconnect.google.drive.credential.ServiceAccountCredentialFactory;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
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

/**
 * Provider for blobs getting information from Google Drive.
 *
 * @since 7.3
 */
public class GoogleDriveBlobProvider extends AbstractLiveConnectBlobProvider<GoogleOAuth2ServiceProvider> {

    public static final int PREFERRED_ICON_SIZE = 16;

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

    private static final String GOOGLEDRIVE_DOCUMENT_TO_BE_UPDATED_PP = "googledrive_document_to_be_updated";

    private static final String APPLICATION_NAME = "Nuxeo/0";

    private static final String FILE_CACHE_NAME = "googleDrive";

    private String serviceAccountId;

    private java.io.File serviceAccountP12File;

    private String clientId;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
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
    protected String getCacheName() {
        return FILE_CACHE_NAME;
    }

    @Override
    public String getPageProviderNameForUpdate() {
        return GOOGLEDRIVE_DOCUMENT_TO_BE_UPDATED_PP;
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
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        if (fileInfo.getRevisionId().isPresent()) {
            Revision revision = getRevision(fileInfo);
            return revision != null ? revision.getDownloadUrl() : null;
        } else {
            File file = getDriveFile(fileInfo);
            return file.getDownloadUrl();
        }
    }

    /**
     * Gets the URL to which we can redirect to let the user download the file.
     */
    protected String getDownloadUrl(ManagedBlob blob) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        String url = null;
        if (fileInfo.getRevisionId().isPresent()) {
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
        } else {
            File file = getDriveFile(fileInfo);
            url = file.getWebContentLink();
            if (url == null) {
                // native Google document
                url = file.getAlternateLink();
            }
        }
        return url;
    }

    // TODO remove
    protected String getAlternateUrl(ManagedBlob blob) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        // ignore revisionId
        File file = getDriveFile(fileInfo);
        return file.getAlternateLink();
    }

    /**
     * Gets the URL to which we can redirect to let the user see a preview of the file.
     */
    protected String getEmbedUrl(ManagedBlob blob) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        // ignore revisionId
        File file = getDriveFile(fileInfo);
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
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        // ignore revisionId
        File file = getDriveFile(fileInfo);
        return file.getThumbnailLink();
    }

    /**
     * Gets the export link.
     */
    protected Map<String, String> getExportLinks(ManagedBlob blob) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        if (fileInfo.getRevisionId().isPresent()) {
            Revision revision = getRevision(fileInfo);
            return revision != null && TRUE.equals(revision.getPinned()) ?
                    revision.getExportLinks() : Collections.emptyMap();
        } else {
            File file = getDriveFile(fileInfo);
            return file.getExportLinks();
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
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        return doGet(fileInfo, uri);
    }

    @Override
    public List<AppLink> getAppLinks(String username, ManagedBlob blob) throws IOException {
        List<AppLink> appLinks = new ArrayList<>();

        LiveConnectFileInfo fileInfo = toFileInfo(blob);

        // application links do not work with revisions
        if (fileInfo.getRevisionId().isPresent()) {
            return appLinks;
        }

        // retrieve the service's user (email in this case) for this username
        String user = getServiceUser(username);

        // fetch a partial file response
        File file = getPartialFile(user, fileInfo.getFileId(), "openWithLinks", "defaultOpenWithLink");
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
            List<App.Icons> icons = app.getIcons();
            if (icons != null) {
                for (App.Icons icon : icons) {
                    if ("application".equals(icon.getCategory())) {
                        appLink.setIcon(icon.getIconUrl());
                        // break if we've got one with our preferred size
                        if (icon.getSize() == PREFERRED_ICON_SIZE) {
                            break;
                        }
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
            return getOAuth2Provider().getServiceUser(username);
        } else {
            UserManager userManager = Framework.getService(UserManager.class);
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
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        if (fileInfo.getRevisionId().isPresent()) {
            // already frozen
            return null;
        }
        String user = fileInfo.getUser();
        String fileId = fileInfo.getFileId();
        // force update of Drive and Live Connect cache
        putFileInCache(retrieveFile(fileInfo));
        // find current revision for that doc (from cache as previous line cached it)
        File driveFile = getDriveFile(fileInfo);
        String revisionId = driveFile.getHeadRevisionId();
        if (revisionId != null) {
            // uploaded file, there is a head revision
            fileInfo = new LiveConnectFileInfo(user, fileId, revisionId);
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

            InputStream is = doGet(fileInfo, uri);
            Blob conversion = Blobs.createBlob(is);
            conversion.setFilename(blob.getFilename());
            conversion.setMimeType(DEFAULT_EXPORT_MIMETYPE);

            fileInfo = new LiveConnectFileInfo(user, fileId, revision.getId());

            // store a conversion of this revision
            storeBlobConversion(doc, buildBlobKey(fileInfo), conversion);
        }
        return toBlob(new GoogleDriveLiveConnectFile(fileInfo, driveFile));
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

    @Override
    protected boolean hasChanged(SimpleManagedBlob blob, LiveConnectFile file) {
        return !blob.getFilename().equals(file.getFilename().replace('/', '-')) && super.hasChanged(blob, file);
    }

    @Override
    protected CredentialFactory getCredentialFactory() {
        GoogleOAuth2ServiceProvider provider = getOAuth2Provider();
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

    @Override
    protected LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException {
        // First, invalidate the Drive file cache in order to force call to API
        invalidateInCache(fileInfo);
        // Second, retrieve it and cache it
        return new GoogleDriveLiveConnectFile(fileInfo, getDriveFile(fileInfo));
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
    protected File getDriveFile(LiveConnectFileInfo fileInfo) throws IOException {
        // ignore revisionId
        String fileId = fileInfo.getFileId();
        String cacheKey = "file_" + fileId;
        DriveRequest<File> request = getService(fileInfo.getUser()).files().get(fileId);
        return executeAndCache(cacheKey, request, File.class);
    }

    /**
     * Retrieves a {@link Revision} resource and caches the unparsed response.
     *
     * @return a {@link Revision} resource
     */
    // subclassed for mock
    protected Revision getRevision(LiveConnectFileInfo fileInfo) throws IOException {
        if (!fileInfo.getRevisionId().isPresent()) {
            throw new NullPointerException("null revisionId for " + fileInfo.getFileId());
        }
        String fileId = fileInfo.getFileId();
        String revisionId = fileInfo.getRevisionId().get();
        String cacheKey = "rev_" + fileId + "_" + revisionId;
        DriveRequest<Revision> request = getService(fileInfo.getUser()).revisions().get(fileId, revisionId);
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
        String resource = getDriveFromCache(cacheKey);

        if (resource == null) {
            HttpResponse response = request.executeUnparsed();
            if (!response.isSuccessStatusCode()) {
                return null;
            }
            resource = response.parseAsString();
            if (cacheKey != null) {
                putDriveInCache(cacheKey, resource);
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
    protected RevisionList getRevisionList(LiveConnectFileInfo fileInfo) throws IOException {
        return getService(fileInfo.getUser()).revisions().list(fileInfo.getFileId()).execute();
    }

    /**
     * Executes a GET request with the user's credentials.
     */
    protected InputStream doGet(LiveConnectFileInfo fileInfo, URI url) throws IOException {
        HttpResponse response = getService(fileInfo.getUser()).getRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
        return response.getContent();
    }

    public String getClientId() {
        GoogleOAuth2ServiceProvider provider = getOAuth2Provider();
        return (provider != null && provider.isEnabled()) ? provider.getClientId() : clientId;
    }

    private String getDriveFromCache(String key) {
        return getFromCache(key);
    }

    private void putDriveInCache(String key, String resource) {
        putInCache(key, resource);
    }

}
