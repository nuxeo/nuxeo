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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.apidoc.repository.SnapshotPersister;
import org.nuxeo.ecm.core.io.impl.DWord;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.drivers.FirefoxDriverProvider;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.proxy.ProxyManager;
import org.nuxeo.runtime.api.Framework;
import org.openqa.selenium.By;
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
        // clean up persisted distribs
        RestHelper.deleteDocument(SnapshotPersister.Root_PATH + SnapshotPersister.Root_NAME);
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

    protected String getDistribExportName(String distribId) {
        return String.format("nuxeo-distribution-%s.zip", distribId);
    }

    protected void checkDistrib(String distribId, boolean partial, String partialVirtualGroup, boolean legacy) {
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_BUNDLEGROUPS);
        checkBundleGroups(partial, partialVirtualGroup, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_BUNDLES);
        checkBundles(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_COMPONENTS);
        checkComponents(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_EXTENSIONPOINTS);
        checkExtensionPoints(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_SERVICES);
        checkServices(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_CONTRIBUTIONS);
        checkContributions(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_OPERATIONS);
        checkOperations(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_PACKAGES);
        checkPackages(partial, legacy);
    }

    @Test
    public void testLiveDistribExportAndImport() {
        String distribName = "my-server";
        open(DistribAdminPage.URL);
        String version = asPage(DistribAdminPage.class).saveCurrentLiveDistrib(distribName, false);
        String distribId = getDistribId(distribName, version);
        asPage(DistribAdminPage.class).checkPersistedDistrib(distribId);
        checkDistrib(distribId, false, null, false);

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
    public void testLivePartialDistribExportAndImport() {
        String distribName = "my-partial-server";
        open(DistribAdminPage.URL);
        String version = asPage(DistribAdminPage.class).saveCurrentLiveDistrib(distribName, true);
        String distribId = getDistribId(distribName, version);
        asPage(DistribAdminPage.class).checkPersistedDistrib(distribId);
        checkDistrib(distribId, true, distribName, false);

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

    protected void createSampleZip(String sourceDirPath, String zipFilePath, boolean addMarker) throws IOException {
        Path p = Files.createFile(Paths.get(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            if (addMarker) {
                ZipEntry zipEntry = new ZipEntry(".nuxeo-archive");
                zs.putNextEntry(zipEntry);
                zs.closeEntry();
            }
            // read paths from reference file as NuxeoArchiveReader requires a given order and extra info
            List<String> lines = Files.readAllLines(Paths.get(sourceDirPath, "entries.txt"));
            for (Iterator<String> lineIter = lines.iterator(); lineIter.hasNext();) {
                String path = lineIter.next();
                if (StringUtils.isEmpty(path)) {
                    continue;
                }
                ZipEntry entry = new ZipEntry(path);
                Path ppath = Paths.get(sourceDirPath, path);
                if (!Files.isDirectory(ppath)) {
                    zs.putNextEntry(entry);
                    Files.copy(ppath, zs);
                } else {
                    entry.setExtra(new DWord(Integer.valueOf(lineIter.next())).getBytes());
                    zs.putNextEntry(entry);
                }
                zs.closeEntry();
            }
        }
    }

    /**
     * @implNote non-regression test for NXP-14948: sample export contains code from quota plugin
     */
    @Test
    public void testSampleDistribImport() throws IOException {
        String sourceDirPath = getReferencePath("data/sample_export");
        File file = new File(downloadDir, "distrib-apidoc.zip");
        createSampleZip(sourceDirPath, file.getPath(), false);

        String newDistribName = "apidoc";
        String newVersion = "1.0.0";
        open(DistribAdminPage.URL);
        asPage(DistribAdminPage.class).importPersistedDistrib(file, newDistribName, newVersion,
                "Details: Not a valid Nuxeo Archive - no marker file found");

        // add the needed ".nuxeo-archive" file at the root of the zip and retry
        FileUtils.deleteQuietly(file);
        createSampleZip(sourceDirPath, file.getPath(), true);

        Locator.scrollAndForceClick(driver.findElement(By.linkText("RETRY")));
        asPage(DistribAdminPage.class).importPersistedDistrib(file, newDistribName, newVersion, null);

        open(ExplorerHomePage.URL);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
        checkDistrib(newDistribId, true, newDistribName, true);
    }

}
