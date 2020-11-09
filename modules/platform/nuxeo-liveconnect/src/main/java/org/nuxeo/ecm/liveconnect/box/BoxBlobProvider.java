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
import java.io.InputStream;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpStatus;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectBlobProvider;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxSharedLink;
import com.box.sdk.BoxSharedLink.Access;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.ArrayMap;

/**
 * Provider for blobs getting information from Box.
 *
 * @since 8.1
 */
public class BoxBlobProvider extends AbstractLiveConnectBlobProvider<BoxOAuth2ServiceProvider> {

    private static final String CACHE_NAME = "box";

    private static final String BOX_DOCUMENT_TO_BE_UPDATED_PP = "box_document_to_be_updated";

    private static final String BOX_URL = "https://api.box.com/2.0/";

    private static final String DOWNLOAD_CONTENT_URL = BOX_URL + "files/%s/content";

    private static final String THUMBNAIL_CONTENT_URL = BOX_URL + "files/%s/thumbnail.jpg?min_height=320&min_width=320";

    private static final String EMBED_URL = BOX_URL + "files/%s?fields=expiring_embed_link";

    @Override
    protected String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public String getPageProviderNameForUpdate() {
        return BOX_DOCUMENT_TO_BE_UPDATED_PP;
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
            url = getEmbedUrl(fileInfo);
            break;
        }
        return url == null ? null : asURI(url);
    }

    @Override
    public InputStream getStream(ManagedBlob blob) throws IOException {
        URI uri = getURI(blob, UsageHint.STREAM, null);
        return uri == null ? null : doGet(uri.toString()).getContent();
    }

    @Override
    public InputStream getThumbnail(ManagedBlob blob) throws IOException {
        // TODO update method when https://github.com/box/box-java-sdk/issues/54 will be done
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        GenericUrl url = new GenericUrl(String.format(THUMBNAIL_CONTENT_URL, fileInfo.getFileId()));

        HttpResponse response = executeAuthenticate(fileInfo, url, false);
        int statusCode = response.getStatusCode();
        if (statusCode == HttpStatus.SC_OK) {
            return response.getContent();
        } else if (statusCode == HttpStatus.SC_MOVED_TEMPORARILY || statusCode == HttpStatus.SC_ACCEPTED) {
            response.disconnect();
            return doGet(response.getHeaders().getLocation()).getContent();
        }
        throw new HttpResponseException(response);
    }

    @Override
    public ManagedBlob freezeVersion(ManagedBlob blob, Document doc) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        if (fileInfo.getRevisionId().isPresent()) {
            // already frozen
            return null;
        }
        BoxFile.Info boxFileInfo = retrieveBoxFileInfo(fileInfo);
        // put the latest file version in cache
        putFileInCache(new BoxLiveConnectFile(fileInfo, boxFileInfo));
        String revisionId = boxFileInfo.getVersion().getVersionID();

        fileInfo = new LiveConnectFileInfo(fileInfo.getUser(), fileInfo.getFileId(), revisionId);
        BoxLiveConnectFile file = new BoxLiveConnectFile(fileInfo, boxFileInfo);
        return toBlob(file);
    }

    protected BoxAPIConnection getBoxClient(NuxeoOAuth2Token token) throws IOException {
        return getBoxClient(getCredential(token));
    }

    protected BoxAPIConnection getBoxClient(Credential credential) throws IOException {
        return new BoxAPIConnection(credential.getAccessToken());
    }

    @Override
    protected LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException {
        return new BoxLiveConnectFile(fileInfo, retrieveBoxFileInfo(fileInfo));
    }

    protected BoxFile.Info retrieveBoxFileInfo(LiveConnectFileInfo fileInfo) throws IOException {
        try {
            return prepareBoxFile(fileInfo).getInfo();
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
        BoxAPIConnection boxClient = getBoxClient(getCredential(fileInfo));
        return new BoxFile(boxClient, fileInfo.getFileId());
    }

    /**
     * Returns the temporary download url for input file.
     *
     * @param fileInfo the file info
     * @return the temporary download url for input file
     */
    private String getDownloadUrl(LiveConnectFileInfo fileInfo) throws IOException {
        // TODO update method when https://github.com/box/box-java-sdk/issues/182 will be done
        GenericUrl url = new GenericUrl(String.format(DOWNLOAD_CONTENT_URL, fileInfo.getFileId()));
        fileInfo.getRevisionId().ifPresent(revId -> url.put("version", revId));

        HttpResponse response = executeAuthenticate(fileInfo, url, false);
        response.disconnect();
        if (response.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            return response.getHeaders().getLocation();
        }
        throw new HttpResponseException(response);
    }

    /**
     * Returns the temporary embed url for input file.
     *
     * @param fileInfo the file info
     * @return the temporary embed url for input file
     */
    @SuppressWarnings("unchecked")
    private String getEmbedUrl(LiveConnectFileInfo fileInfo) throws IOException {
        // TODO change below by box client call when it'll be available
        GenericUrl url = new GenericUrl(String.format(EMBED_URL, fileInfo.getFileId()));

        try {
            HttpResponse response = executeAuthenticate(fileInfo, url, true);
            GenericJson boxInfo = response.parseAs(GenericJson.class);
            return ((ArrayMap<String, Object>) boxInfo.get("expiring_embed_link")).get("url").toString();
        } catch (HttpResponseException e) {
            // Extension not supported
            if (e.getStatusCode() == HttpStatus.SC_REQUEST_URI_TOO_LONG) {
                return null;
            }
            throw e;
        }
    }

    private HttpResponse executeAuthenticate(LiveConnectFileInfo fileInfo, GenericUrl url, boolean followRedirect)
            throws IOException {
        Credential credential = getCredential(fileInfo);

        HttpRequest request = getOAuth2Provider().getRequestFactory().buildGetRequest(url);
        request.setHeaders(new HttpHeaders().setAuthorization("Bearer " + credential.getAccessToken()));
        request.setFollowRedirects(followRedirect);
        request.setThrowExceptionOnExecuteError(followRedirect);

        return request.execute();
    }

    /**
     * Executes a GET request
     */
    private HttpResponse doGet(String url) throws IOException {
        HttpRequestFactory requestFactory = getOAuth2Provider().getRequestFactory();
        return requestFactory.buildGetRequest(new GenericUrl(url)).execute();
    }

}
