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

import java.io.IOException;

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
        return new MockLiveConnectFile(fileInfo, LiveConnectTestCase.FILE_1_NAME, LiveConnectTestCase.FILE_1_SIZE,
                LiveConnectTestCase.FILE_1_DIGEST);
    }

}
