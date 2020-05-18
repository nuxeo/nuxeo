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
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleGroupArtifactPage;
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

    protected void checkLiveDistrib(String distribId) {
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_BUNDLEGROUPS);
        checkBundleGroups();
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_BUNDLES);
        checkBundles(false);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_COMPONENTS);
        checkComponents(false);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_EXTENSIONPOINTS);
        checkExtensionPoints(false);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_SERVICES);
        checkServices(false);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_CONTRIBUTIONS);
        checkContributions(false);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_OPERATIONS);
        checkOperations(false);
    }

    @Test
    public void testLiveDistribExportAndImport() {
        String distribName = "my-server";
        open(DistribAdminPage.URL);
        String version = asPage(DistribAdminPage.class).saveCurrentLiveDistrib(distribName, false);
        String distribId = getDistribId(distribName, version);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(distribId);
        checkLiveDistrib(distribId);

        // check importing it back
        open(DistribAdminPage.URL);
        String filename = getDistribExportName(distribId);
        File file = asPage(DistribAdminPage.class).exportFirstPersistedDistrib(downloadDir, filename);

        open(DistribAdminPage.URL);
        String newDistribName = "imported-server";
        String newVersion = "1.0.0";
        asPage(DistribAdminPage.class).importPersistedDistrib(file, newDistribName, newVersion, null);
        open(ExplorerHomePage.URL);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
        checkLiveDistrib(newDistribId);
    }

    protected void checkPartialDistrib(String virtualBundleGroup, String distribId) {
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_BUNDLEGROUPS);
        checkPartialBundleGroup(virtualBundleGroup);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_BUNDLES);
        checkBundles(true);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_COMPONENTS);
        checkComponents(true);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_EXTENSIONPOINTS);
        checkExtensionPoints(true);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_SERVICES);
        checkServices(true);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_CONTRIBUTIONS);
        checkContributions(true);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_OPERATIONS);
        checkOperations(true);
    }

    // will be easier to maintain as a reference when a specific bundle group is used for explorer...
    protected void checkPartialBundleGroup(String distribName) {
        Locator.findElementWaitUntilEnabledAndClick(By.linkText(distribName));
        BundleGroupArtifactPage apage = asPage(BundleGroupArtifactPage.class);
        apage.checkDocumentationText(null);
        apage.checkSubGroup(null);
        apage.checkBundle("org.nuxeo.apidoc.core");
        apage.checkBundle("org.nuxeo.apidoc.repo");
        apage.checkBundle("org.nuxeo.apidoc.webengine");
    }

    @Test
    public void testLivePartialDistribExportAndImport() {
        String distribName = "my-partial-server";
        open(DistribAdminPage.URL);
        String version = asPage(DistribAdminPage.class).saveCurrentLiveDistrib(distribName, true);
        String distribId = getDistribId(distribName, version);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(distribId);
        checkPartialDistrib(distribName, distribId);

        // check importing it back
        open(DistribAdminPage.URL);
        String filename = getDistribExportName(distribId);
        File file = asPage(DistribAdminPage.class).exportFirstPersistedDistrib(downloadDir, filename);

        open(DistribAdminPage.URL);
        String newDistribName = "partial-imported-server";
        String newVersion = "1.0.0";
        asPage(DistribAdminPage.class).importPersistedDistrib(file, newDistribName, newVersion, null);
        open(ExplorerHomePage.URL);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
        checkPartialDistrib(distribName, newDistribId);
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
            for (int i = 0; i < lines.size(); i++) {
                String path = lines.get(i);
                if (StringUtils.isEmpty(path)) {
                    continue;
                }
                ZipEntry entry = new ZipEntry(path);
                Path ppath = Paths.get(sourceDirPath, path);
                if (!Files.isDirectory(ppath)) {
                    zs.putNextEntry(entry);
                    Files.copy(ppath, zs);
                } else {
                    entry.setExtra(new DWord(Integer.valueOf(lines.get(i + 1))).getBytes());
                    zs.putNextEntry(entry);
                    i++;
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
        open(DistribAdminPage.URL);
        asPage(DistribAdminPage.class).importPersistedDistrib(file, newDistribName, newVersion, null);

        open(ExplorerHomePage.URL);
        String newDistribId = getDistribId(newDistribName, newVersion);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(newDistribId);
        // XXX: will not be able to reuse the same check method when/if apidoc export diverges from sample
        checkPartialDistrib(newDistribName, newDistribId);
    }

}
