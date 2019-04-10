/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *
 */

package org.nuxeo.wopi;

import static org.junit.Assert.assertTrue;
import static org.nuxeo.wopi.Constants.WOPI_DISCOVERY_KEY;
import static org.nuxeo.wopi.Constants.WOPI_DISCOVERY_REFRESH_EVENT;
import static org.nuxeo.wopi.Constants.WOPI_KEY_VALUE_STORE_NAME;

import java.io.File;

import javax.inject.Inject;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Feature filling the WOPI key value store with a test discovery and then re-loading the discovery.
 *
 * @since 10.3
 */
public class WOPIDiscoveryFeature implements RunnerFeature {

    @Inject
    protected KeyValueService keyValueService;

    @Inject
    protected WOPIService wopiService;

    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        KeyValueStore keyValueStore = keyValueService.getKeyValueStore(WOPI_KEY_VALUE_STORE_NAME);
        File testDiscoveryFile = FileUtils.getResourceFileFromContext("test-discovery.xml");
        keyValueStore.put(WOPI_DISCOVERY_KEY, org.apache.commons.io.FileUtils.readFileToByteArray(testDiscoveryFile));
        try (CapturingEventListener capturingEventListener = new CapturingEventListener(WOPI_DISCOVERY_REFRESH_EVENT)) {
            // force re-loading of discovery after filling the KeyValue
            ((WOPIServiceImpl) wopiService).loadDiscovery();
            // check that the refreshWOPIDiscovery event has been fired
            assertTrue(capturingEventListener.hasBeenFired(WOPI_DISCOVERY_REFRESH_EVENT));
        }

    }
}
