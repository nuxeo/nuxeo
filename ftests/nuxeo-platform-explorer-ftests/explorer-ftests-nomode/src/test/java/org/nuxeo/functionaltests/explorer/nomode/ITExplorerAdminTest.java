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
package org.nuxeo.functionaltests.explorer.nomode;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.UploadFragment;
import org.nuxeo.functionaltests.explorer.testing.AbstractExplorerDownloadTest;
import org.openqa.selenium.By;

/**
 * Test Explorer pages usually handled by admins.
 *
 * @since 11.1
 */
public class ITExplorerAdminTest extends AbstractExplorerDownloadTest {

    @Before
    public void before() {
        doLogin();
    }

    @After
    public void after() {
        doLogout();
        cleanupPersistedDistributions();
    }

    @Override
    protected void doLogin() {
        loginAsAdmin();
    }

    /**
     * Simple login, logout test, checking the home page is displayed without errors after login.
     */
    @Test
    public void testLoginLogout() {
        goHome();
    }

    @Test
    public void testDistribAdminPage() {
        open(DistribAdminPage.URL);
        DistribAdminPage page = asPage(DistribAdminPage.class);
        page.check();
        page.checkCanSave();
    }

    @Test
    public void testHomePageLiveDistrib() {
        ExplorerHomePage home = goHome();
        home.check();
        home.checkCurrentDistrib();
        UploadFragment.checkCanSee();
        checkHomeLiveDistrib();
    }

    protected String checkLiveDistribExport(String distribName) {
        open(DistribAdminPage.URL);
        String version = asPage(DistribAdminPage.class).saveCurrentLiveDistrib(distribName, false);
        String distribId = getDistribId(distribName, version);
        asPage(DistribAdminPage.class).checkPersistedDistrib(distribId);
        checkDistrib(distribId, false, null, false);
        return distribId;
    }

    protected void checkLiveDistribImport(String distribId) {
        // check importing it back
        open(DistribAdminPage.URL);
        String filename = getDistribExportName(distribId);
        File file = asPage(DistribAdminPage.class).exportFirstPersistedDistrib(downloadDir, filename);

        open(DistribAdminPage.URL);
        String newDistribName = "imported-server";
        String newVersion = "1.0.0";
        asPage(DistribAdminPage.class).importPersistedDistrib(file, newDistribName, newVersion, null);
        // NXP-29154: check redirection to admin page
        asPage(DistribAdminPage.class);
        open(ExplorerHomePage.URL);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
        checkDistrib(distribId, false, null, false);
    }

    @Test
    public void testLiveDistribExportAndImport() {
        String distribName = "my-server";
        String distribId = checkLiveDistribExport(distribName);
        checkLiveDistribImport(distribId);
    }

    protected String checkLivePartialDistribExport(String distribName) {
        open(DistribAdminPage.URL);
        String version = asPage(DistribAdminPage.class).saveCurrentLiveDistrib(distribName, true);
        String distribId = getDistribId(distribName, version);
        asPage(DistribAdminPage.class).checkPersistedDistrib(distribId);
        checkDistrib(distribId, true, distribName, false);
        return distribId;
    }

    protected void checkLivePartialDistribImport(String distribName, String distribId) {
        // check importing it back
        open(DistribAdminPage.URL);
        String filename = getDistribExportName(distribId);
        File file = asPage(DistribAdminPage.class).exportFirstPersistedDistrib(downloadDir, filename);

        // import it from the home page this time
        open(ExplorerHomePage.URL);
        String newDistribName = "partial-imported-server";
        String newVersion = "1.0.0";
        asPage(ExplorerHomePage.class).importPersistedDistrib(file, newDistribName, newVersion, null);
        // NXP-29154: check redirection to home page
        asPage(ExplorerHomePage.class);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
        checkDistrib(distribId, true, distribName, false);
    }

    @Test
    public void testLivePartialDistribExportAndImport() {
        String distribName = "my-partial-server";
        String distribId = checkLivePartialDistribExport(distribName);
        checkLivePartialDistribImport(distribName, distribId);
    }

    /**
     * @implNote non-regression test for NXP-14948: sample export contains code from quota plugin
     */
    @Test
    public void testSampleDistribImport() throws IOException {
        File zip = createSampleZip(false);

        String newDistribName = "apidoc-imported";
        String newVersion = "1.0.0";
        open(DistribAdminPage.URL);
        asPage(DistribAdminPage.class).importPersistedDistrib(zip, newDistribName, newVersion,
                "Details: Not a valid Nuxeo Archive - no marker file found");

        // add the needed ".nuxeo-archive" file at the root of the zip and retry
        zip = createSampleZip(true);

        Locator.scrollAndForceClick(driver.findElement(By.linkText("RETRY")));
        asPage(DistribAdminPage.class).importPersistedDistrib(zip, newDistribName, newVersion, null);

        open(ExplorerHomePage.URL);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
        checkDistrib(newDistribId, true, SAMPLE_BUNDLE_GROUP, true);
    }

}
