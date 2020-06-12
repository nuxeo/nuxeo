/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.sitemode;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.testing.AbstractExplorerDownloadTest;

/**
 * Inits the server with a sample persisted distrib (as live distrib cannot be used in site mode).
 *
 * @since 11.2
 */
public class AbstractExplorerSiteModeTest extends AbstractExplorerDownloadTest {

    protected static final String DISTRIB_NAME = "apidoc-sitemode";

    protected static final String DISTRIB_VERSION = "1.0.1";

    @BeforeClass
    public static void initPersistedDistrib() throws IOException {
        loginAsAdmin();
        open(DistribAdminPage.URL);
        File zip = createSampleZip(true);
        asPage(DistribAdminPage.class).importPersistedDistrib(zip, DISTRIB_NAME, DISTRIB_VERSION, null);
        doLogout();
    }

    @AfterClass
    public static void cleanupPersistedDistrib() {
        cleanupPersistedDistributions();
    }

}
