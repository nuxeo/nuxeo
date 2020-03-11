/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.liveconnect.onedrive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectBlobProvider;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.onedrive.client.OneDriveAPI;
import org.nuxeo.onedrive.client.OneDriveFile;
import org.nuxeo.onedrive.client.OneDriveSharingLink.Type;
import org.nuxeo.onedrive.client.OneDriveThumbnailSize;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.auth.oauth2.Credential;

/**
 * Provider for blobs getting information from OneDrive.
 *
 * @since 8.2
 */
public class OneDriveBlobProvider extends AbstractLiveConnectBlobProvider<OneDriveOAuth2ServiceProvider> {

    private static final String CACHE_NAME = "onedrive";

    private static final String ONEDRIVE_DOCUMENT_TO_BE_UPDATED_PP = "onedrive_document_to_be_updated";

    @Override
    protected String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    protected String getPageProviderNameForUpdate() {
        return ONEDRIVE_DOCUMENT_TO_BE_UPDATED_PP;
    }

    @Override
    public URI getURI(ManagedBlob blob, UsageHint usage, HttpServletRequest servletRequest) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        String url = null;
        switch (usage) {
        case STREAM:
        case DOWNLOAD:
            url = prepareOneDriveFile(fileInfo).getMetadata().getDownloadUrl();
            break;
        case VIEW:
        case EDIT:
            url = prepareOneDriveFile(fileInfo).createSharedLink(Type.EDIT).getLink().getWebUrl();
            break;
        }
        return url == null ? null : asURI(url);
    }

    @Override
    public InputStream getStream(ManagedBlob blob) throws IOException {
        return prepareOneDriveFile(toFileInfo(blob)).download();
    }

    @Override
    public InputStream getThumbnail(ManagedBlob blob) throws IOException {
        return prepareOneDriveFile(toFileInfo(blob)).downloadThumbnail(OneDriveThumbnailSize.LARGE);
    }

    @Override
    public List<AppLink> getAppLinks(String username, ManagedBlob blob) throws IOException {
        // application links do not work with document which are not office document
        if (!blob.getMimeType().contains("officedocument")) {
            return Collections.emptyList();
        }
        // application links do not work with revisions
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        if (fileInfo.getRevisionId().isPresent()) {
            return Collections.emptyList();
        }
        String baseUrl = Framework.getProperty("nuxeo.url", VirtualHostHelper.getContextPathProperty());
        AppLink appLink = new AppLink();
        String appUrl = prepareOneDriveFile(fileInfo).createSharedLink(Type.EDIT).getLink().getWebUrl();
        appLink.setLink(appUrl);
        appLink.setAppName("Microsoft OneDrive");
        appLink.setIcon(baseUrl + "/icons/OneDrive.png");
        return Collections.singletonList(appLink);
    }

    protected OneDriveAPI getOneDriveAPI(LiveConnectFileInfo fileInfo) throws IOException {
        return getOneDriveAPI(getCredential(fileInfo));
    }

    protected OneDriveAPI getOneDriveAPI(Credential credential) {
        return getOAuth2Provider().getAPIInitializer().apply(credential.getAccessToken());
    }

    @Override
    protected LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException {
        return new OneDriveLiveConnectFile(fileInfo, retrieveOneDriveFileMetadata(fileInfo));
    }

    protected OneDriveFile.Metadata retrieveOneDriveFileMetadata(LiveConnectFileInfo fileInfo) throws IOException {
        return prepareOneDriveFile(fileInfo).getMetadata();
    }

    protected OneDriveFile prepareOneDriveFile(LiveConnectFileInfo fileInfo) throws IOException {
        OneDriveAPI api = getOneDriveAPI(fileInfo);
        return new OneDriveFile(api, fileInfo.getFileId());
    }

}
