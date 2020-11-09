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
package org.nuxeo.ecm.liveconnect.core;

import static org.nuxeo.ecm.liveconnect.LiveConnectFeature.SERVICE_CORE_ID;
import static org.nuxeo.ecm.liveconnect.LiveConnectFeature.USER_ID;
import static org.nuxeo.ecm.liveconnect.core.TestLiveConnectBlobProvider.FILE_1_BYTES;
import static org.nuxeo.ecm.liveconnect.core.TestLiveConnectBlobProvider.FILE_1_DIGEST;
import static org.nuxeo.ecm.liveconnect.core.TestLiveConnectBlobProvider.FILE_1_ID;
import static org.nuxeo.ecm.liveconnect.core.TestLiveConnectBlobProvider.FILE_1_NAME;
import static org.nuxeo.ecm.liveconnect.core.TestLiveConnectBlobProvider.FILE_1_SIZE;
import static org.nuxeo.ecm.liveconnect.core.TestLiveConnectBlobProvider.INVALID_FILE_ID;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;

/**
 * @since 8.1
 */
public class MockLiveConnectBlobProvider extends AbstractLiveConnectBlobProvider<OAuth2ServiceProvider> {

    @Override
    protected String getCacheName() {
        return "core";
    }

    @Override
    protected String getPageProviderNameForUpdate() {
        return "core_document_to_be_updated";
    }

    @Override
    protected LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException {
        if (INVALID_FILE_ID.equals(fileInfo.getFileId())) {
            // test crashing case
            throw new IOException("Invalid file id: " + INVALID_FILE_ID);
        }
        return new MockLiveConnectFile(fileInfo, FILE_1_NAME, FILE_1_SIZE, FILE_1_DIGEST);
    }

    @Override
    public InputStream getStream(ManagedBlob blob) throws IOException {
        String key = blob.getKey();
        if ((SERVICE_CORE_ID + ':' + USER_ID + ':' + INVALID_FILE_ID).equals(key)) {
            // test crashing case
            throw new IOException("Invalid file id: " + INVALID_FILE_ID);
        }
        if ((SERVICE_CORE_ID + ':' + USER_ID + ':' + FILE_1_ID).equals(key)) {
            return new ByteArrayInputStream(FILE_1_BYTES);
        }
        throw new IOException(key);
    }

}
