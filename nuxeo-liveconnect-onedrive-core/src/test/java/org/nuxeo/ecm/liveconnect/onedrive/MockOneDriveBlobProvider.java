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
package org.nuxeo.ecm.liveconnect.onedrive;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.onedrive.client.OneDriveAPI;
import org.nuxeo.onedrive.client.OneDriveAPIException;
import org.nuxeo.onedrive.client.OneDriveExpand;
import org.nuxeo.onedrive.client.OneDriveFile;
import org.nuxeo.onedrive.client.OneDrivePermission;
import org.nuxeo.onedrive.client.OneDriveSharingLink;

import com.eclipsesource.json.JsonObject;

/**
 * @since 8.2
 */
public class MockOneDriveBlobProvider extends OneDriveBlobProvider {

    private static final String FILE_FILE_METADATA_FORMAT = "/file-metadata-%s.json";

    private static final String FILE_PERMISSION_METADATA_FORMAT = "/permission-metadata-%s.json";

    @Override
    protected OneDriveFile prepareOneDriveFile(LiveConnectFileInfo fileInfo) throws IOException {
        return new MockOneDriveFile(fileInfo);
    }

    private static class MockOneDriveFile extends OneDriveFile {

        public MockOneDriveFile(LiveConnectFileInfo fileInfo) {
            super(mock(OneDriveAPI.class), fileInfo.getFileId());
        }

        @Override
        public OneDriveFile.Metadata getMetadata(OneDriveExpand... expands) throws OneDriveAPIException {
            try {
                String name = String.format(FILE_FILE_METADATA_FORMAT, getId());
                String content = IOUtils.toString(getClass().getResource(name), StandardCharsets.UTF_8);

                return new Metadata(JsonObject.readFrom(content));
            } catch (IOException e) {
                throw new OneDriveAPIException("Unable to read the file for OneDriveFile.Metadata", e);
            }
        }

        @Override
        public OneDrivePermission.Metadata createSharedLink(OneDriveSharingLink.Type type) throws OneDriveAPIException {
            try {
                String name = String.format(FILE_PERMISSION_METADATA_FORMAT, getId());
                String content = IOUtils.toString(getClass().getResource(name), StandardCharsets.UTF_8);

                OneDrivePermission permission = new OneDrivePermission(getApi(), getId(), "PERMISSION_ID");
                return permission.new Metadata(JsonObject.readFrom(content));
            } catch (IOException e) {
                throw new OneDriveAPIException("Unable to read the file for OneDrivePermission.Metadata", e);
            }
        }

    }

}
