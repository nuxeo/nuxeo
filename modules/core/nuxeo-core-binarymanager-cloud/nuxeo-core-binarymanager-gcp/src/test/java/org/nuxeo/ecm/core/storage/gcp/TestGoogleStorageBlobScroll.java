/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.storage.gcp;

import static org.nuxeo.ecm.core.storage.gcp.GoogleStorageBlobProvider.STORE_SCROLL_NAME;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.blob.AbstractTestBlobScroll;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2023.5
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, GoogleStorageBlobProviderFeature.class })
public class TestGoogleStorageBlobScroll extends AbstractTestBlobScroll {

    @Override
    protected String getScrollName() {
        return STORE_SCROLL_NAME;
    }

}
