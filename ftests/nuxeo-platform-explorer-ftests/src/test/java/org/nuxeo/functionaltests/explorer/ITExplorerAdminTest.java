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
package org.nuxeo.functionaltests.explorer;

import static org.junit.Assume.assumeTrue;
import static org.nuxeo.functionaltests.Constants.ADMINISTRATOR;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.functionaltests.drivers.FirefoxDriverProvider;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.proxy.ProxyManager;
import org.nuxeo.runtime.api.Framework;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;

/**
 * Test Explorer pages usually handled by admins.
 *
 * @since 11.1
 */
public class ITExplorerAdminTest extends AbstractExplorerTest {

    public static File downloadDir;

    /**
     * Updates the firefox profile to ease up testing of downloaded distribution.
     * <p>
     * See NXP-20646 for download solution.
     */
    @BeforeClass
    public static void initFirefoxDriver() throws Exception {
        assumeTrue(driver instanceof FirefoxDriver);

        // quit existing: will recreate one
        quitDriver();

        proxyManager = new ProxyManager();
        Proxy proxy = proxyManager.startProxy();
        if (proxy != null) {
            proxy.setNoProxy("");
        }
        DesiredCapabilities dc = DesiredCapabilities.firefox();
        dc.setCapability(CapabilityType.PROXY, proxy);
        FirefoxProfile profile = FirefoxDriverProvider.getProfile();
        JavaScriptError.addExtension(profile);

        // specific profile part
        downloadDir = Framework.createTempDirectory("webdriver-explorer-admin").toFile();
        profile.setPreference("browser.download.dir", downloadDir.toString());
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.download.useDownloadDir", true);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/zip");

        dc.setCapability(FirefoxDriver.PROFILE, profile);
        driver = new FirefoxDriver(dc);
    }

    @AfterClass
    public static void cleanupDownloadDir() throws Exception {
        FileUtils.deleteDirectory(downloadDir);
    }

    @Before
    public void before() {
        doLogin();
    }

    @After
    public void after() {
        doLogout();
    }

    @Override
    protected void doLogin() {
        getLoginPage().login(ADMINISTRATOR, ADMINISTRATOR);
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
    }

    @Test
    public void testHomePageLiveDistrib() {
        ExplorerHomePage home = goHome();
        home.check();
        // check upload form is here for admin
        asPage(UploadFragment.class);
    }

    protected String getDistribId(String name, String version) {
        return String.format("%s-%s", name, version);
    }

    @Test
    public void testLiveDistribExportAndImport() {
        String distribName = "my-server";
        open(DistribAdminPage.URL);
        String version = asPage(DistribAdminPage.class).saveCurrentLiveDistrib(distribName);
        String distribId = getDistribId(distribName, version);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(distribId);

        // check importing it back
        open(DistribAdminPage.URL);
        String filename = String.format("nuxeo-distribution-%s.zip", distribId);
        File file = asPage(DistribAdminPage.class).exportFirstPersistedDistrib(downloadDir, filename);

        open(DistribAdminPage.URL);
        String newDistribName = "imported-server";
        String newVersion = "1.0.0";
        asPage(DistribAdminPage.class).importPersistedDistrib(file, newDistribName, newVersion);
        open(ExplorerHomePage.URL);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
    }

}
