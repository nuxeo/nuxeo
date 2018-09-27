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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.wopi;

import static org.nuxeo.wopi.WOPIServiceImpl.DISCOVERY_XML;
import static org.nuxeo.wopi.WOPIServiceImpl.WOPI_DIR;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since 10.3
 */
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.wopi")
public class WOPIFeature implements RunnerFeature {

    public static final String TEST_DISCOVERY_NAME = "test-discovery.xml";

    @Override
    public void start(FeaturesRunner runner) {
        Path discoveryPath = Paths.get(Environment.getDefault().getData().getAbsolutePath(), WOPI_DIR, DISCOVERY_XML);
        if (Files.exists(discoveryPath)) {
            return;
        }

        InputStream is = getTestDiscovery();
        if (is != null) {
            try {
                Files.createDirectories(discoveryPath.getParent());
                Files.copy(is, discoveryPath);
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }
    }

    private static InputStream getTestDiscovery() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_DISCOVERY_NAME);
    }
}
