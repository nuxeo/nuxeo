/*
 * (C) Copyright 2012-2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 *
 */

package org.nuxeo.launcher.connect;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.nuxeo.launcher.connect.ConnectBroker.LAUNCHER_CHANGED_PROPERTY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.connector.http.ConnectUrlConfig;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.launcher.config.ConfigurationConstants;
import org.nuxeo.launcher.connect.fake.LocalConnectFakeConnector;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

/**
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features({ LogCaptureFeature.class, ServletContainerFeature.class })
@Deploy("org.nuxeo.connect.standalone.test:OSGI-INF/server-deploy-contrib.xml")
public class TestConnectBroker {

    public static final String TEST_STORE_PATH = "src/test/resources/packages/store";

    public static final String TEST_LOCAL_ONLY_PATH = TEST_STORE_PATH + "/local-only";

    public static final File testStore = new File(TEST_STORE_PATH);

    public static final File nuxeoHome = new File(FeaturesRunner.getBuildDirectory() + "/launcher");

    protected ConnectBroker connectBroker;

    private Environment environment;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    @Before
    public void setUpPort() {
        int port = servletContainerFeature.getPort();
        ConnectUrlConfig.setTestPort(port);
    }

    public static class PkgRequestLogFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LogEvent event) {
            return event.getLevel().isMoreSpecificThan(Level.INFO) && (event.getLoggerName().contains("ConnectBroker")
                    || event.getLoggerName().contains("PackagePersistence")
                    || event.getLoggerName().contains("PackageManagerImpl")
                    || event.getLoggerName().contains("MessageInfo")
                    || event.getLoggerName().contains("LocalDownloadingPackage"));
        }
    }

    public static class FakeConnectDownloadServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String target = request.getPathInfo();
            if (target.startsWith("/test")) {
                String pkgId = target.substring(target.lastIndexOf("/") + 1);
                File pkgZip = new File(testStore, pkgId + ".zip");
                if (pkgZip.exists()) {
                    response.setContentLength((int) pkgZip.length());
                    response.setStatus(HttpServletResponse.SC_OK);
                    try (ServletOutputStream os = response.getOutputStream();
                            FileInputStream is = new FileInputStream(pkgZip)) {
                        IOUtils.copy(is, os);
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        }
    }

    @Before
    public void beforeEach() throws Exception {
        // set fake Connect connector
        String addonJSON = FileUtils.readFileToString(new File(testStore, "addon_remote.json"), UTF_8);
        String hotfixJSON = FileUtils.readFileToString(new File(testStore, "hotfix_remote.json"), UTF_8);
        String studioJSON = FileUtils.readFileToString(new File(testStore, "studio_remote.json"), UTF_8);
        String addonWithPrivateJSON = FileUtils.readFileToString(new File(testStore, "addon_with_private_remote.json"),
                UTF_8);
        NuxeoConnectClient.getConnectGatewayComponent()
                          .setTestConnector(new LocalConnectFakeConnector(addonJSON, hotfixJSON, studioJSON,
                                  addonWithPrivateJSON));

        // build env
        Environment.setDefault(null);
        FileUtils.deleteQuietly(nuxeoHome);
        nuxeoHome.mkdirs();
        System.setProperty(Environment.NUXEO_HOME, nuxeoHome.getPath());
        System.setProperty(ConfigurationConstants.TOMCAT_HOME, Environment.getDefault().getServerHome().getPath());

        // build test packages store
        buildInitialPackageStore();

        environment = Environment.getDefault();
        environment.setProperty(Environment.DISTRIBUTION_NAME, "server");
        environment.setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        environment.setProperty(ConnectBroker.LAUNCHER_CHANGED_PROPERTY, "false");

        connectBroker = new ConnectBroker(environment);
        connectBroker.setPendingFile(environment.getData().toPath().resolve("installAfterRestart.log"));
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
    }

    private void copyPackageToStore(File nuxeoStore, File uninstallFile, File pkgZip) {
        try {
            File pkgDir = new File(nuxeoStore, pkgZip.getName().replace(".zip", ""));
            ZipUtils.unzip(pkgZip, pkgDir);
            FileUtils.copyFileToDirectory(uninstallFile, pkgDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildInitialPackageStore() throws IOException {
        File nuxeoPackages = new File(nuxeoHome, "packages");
        File nuxeoStore = new File(nuxeoPackages, "store");
        File uninstallFile = new File(testStore, "uninstall.xml");

        // Copy all zip from testStore
        FileUtils.iterateFiles(testStore, new String[] { "zip" }, false)
                 .forEachRemaining(pkgZip -> copyPackageToStore(nuxeoStore, uninstallFile, pkgZip));
        // Copy only installed packages from testStore/local-only
        copyPackageToStore(nuxeoStore, uninstallFile, new File(TEST_LOCAL_ONLY_PATH, "K-1.0.0-SNAPSHOT.zip"));

        // Copy all unzipped packages
        String[] unzippedPkgs = { "NXP-24507-A-1.0.0", "NXP-24507-B-1.0.0" };
        for (String pkg : unzippedPkgs) {
            File sourceDir = new File(TEST_LOCAL_ONLY_PATH, pkg);
            File targetDir = new File(nuxeoStore, pkg);
            assertThat(sourceDir).exists();
            FileUtils.copyDirectory(sourceDir, targetDir);
        }

        FileUtils.copyFileToDirectory(new File(testStore, ".packages"), nuxeoPackages);
    }

    @After
    public void afterEach() {
        // clear system properties
        System.clearProperty(Environment.NUXEO_HOME);
        System.clearProperty(ConfigurationConstants.TOMCAT_HOME);
        // clear any potential registration
        LogicalInstanceIdentifier.cleanUp();
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testListAllPackages() throws Exception {
        // GIVEN we are unregistered

        // WHEN trying to list all packages
        connectBroker.pkgListAll();

        // THEN it shows all expected packages with "[REGISTRATION REQUIRED]" on relevant packages
        String expectedLogs = """
                All packages:
                studio     started\tstudioA (id: studioA-1.0.0)\s
                studio  downloaded\tstudioA (id: studioA-1.0.1)\s
                studio  downloaded\tstudioA (id: studioA-1.0.2-SNAPSHOT)\s
                hotfix     started\thfA (id: hfA-1.0.0)\s
                hotfix  downloaded\thfA (id: hfA-1.0.8)\s
                hotfix  downloaded\thfB (id: hfB-1.0.0)\s
                hotfix  downloaded\thfC (id: hfC-1.0.0-SNAPSHOT)\s
                 addon     started\tA (id: A-1.0.0)\s
                 addon  downloaded\tA (id: A-1.2.0)\s
                 addon  downloaded\tA (id: A-1.2.1-SNAPSHOT)\s
                 addon  downloaded\tA (id: A-1.2.2-SNAPSHOT)\s
                 addon  downloaded\tA (id: A-1.2.2)\s
                 addon  downloaded\tA (id: A-1.2.3-SNAPSHOT)\s
                 addon     started\tB (id: B-1.0.1-SNAPSHOT)\s
                 addon  downloaded\tB (id: B-1.0.1)\s
                 addon  downloaded\tB (id: B-1.0.2)\s
                 addon     started\tC (id: C-1.0.0)\s
                 addon  downloaded\tC (id: C-1.0.1-SNAPSHOT)\s
                 addon  downloaded\tC (id: C-1.0.2-SNAPSHOT)\s
                 addon     started\tD (id: D-1.0.2-SNAPSHOT)\s
                 addon  downloaded\tD (id: D-1.0.3-SNAPSHOT)\s
                 addon  downloaded\tD (id: D-1.0.4-SNAPSHOT)\s
                 addon     started\tG (id: G-1.0.1-SNAPSHOT)\s
                 addon     started\tH (id: H-1.0.1-SNAPSHOT)\s
                 addon     started\tJ (id: J-1.0.1)\s
                 addon     started\tK (id: K-1.0.0-SNAPSHOT)\s
                 addon  downloaded\tM (id: M-1.0.0-SNAPSHOT)\s
                 addon      remote\tM (id: M-1.0.1) [REGISTRATION REQUIRED]
                 addon  downloaded\tN (id: N-1.0.1-HF08-SNAPSHOT)\s
                 addon  downloaded\tNXP-24507-A (id: NXP-24507-A-1.0.0)\s
                 addon  downloaded\tNXP-24507-B (id: NXP-24507-B-1.0.0)\s
                """;
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // GIVEN we are registered
        LogicalInstanceIdentifier CLID = new LogicalInstanceIdentifier("toto--titi", "myInstance");
        CLID.save();

        // WHEN trying to list all packages
        connectBroker.refreshCache();
        connectBroker.pkgListAll();

        // THEN it shows all expected packages without the "[REGISTRATION REQUIRED]"
        expectedLogs = """
                All packages:
                studio     started\tstudioA (id: studioA-1.0.0)\s
                studio  downloaded\tstudioA (id: studioA-1.0.1)\s
                studio  downloaded\tstudioA (id: studioA-1.0.2-SNAPSHOT)\s
                hotfix     started\thfA (id: hfA-1.0.0)\s
                hotfix  downloaded\thfA (id: hfA-1.0.8)\s
                hotfix  downloaded\thfB (id: hfB-1.0.0)\s
                hotfix  downloaded\thfC (id: hfC-1.0.0-SNAPSHOT)\s
                 addon     started\tA (id: A-1.0.0)\s
                 addon  downloaded\tA (id: A-1.2.0)\s
                 addon  downloaded\tA (id: A-1.2.1-SNAPSHOT)\s
                 addon  downloaded\tA (id: A-1.2.2-SNAPSHOT)\s
                 addon  downloaded\tA (id: A-1.2.2)\s
                 addon  downloaded\tA (id: A-1.2.3-SNAPSHOT)\s
                 addon     started\tB (id: B-1.0.1-SNAPSHOT)\s
                 addon  downloaded\tB (id: B-1.0.1)\s
                 addon  downloaded\tB (id: B-1.0.2)\s
                 addon     started\tC (id: C-1.0.0)\s
                 addon  downloaded\tC (id: C-1.0.1-SNAPSHOT)\s
                 addon  downloaded\tC (id: C-1.0.2-SNAPSHOT)\s
                 addon     started\tD (id: D-1.0.2-SNAPSHOT)\s
                 addon  downloaded\tD (id: D-1.0.3-SNAPSHOT)\s
                 addon  downloaded\tD (id: D-1.0.4-SNAPSHOT)\s
                 addon     started\tG (id: G-1.0.1-SNAPSHOT)\s
                 addon     started\tH (id: H-1.0.1-SNAPSHOT)\s
                 addon     started\tJ (id: J-1.0.1)\s
                 addon     started\tK (id: K-1.0.0-SNAPSHOT)\s
                 addon  downloaded\tM (id: M-1.0.0-SNAPSHOT)\s
                 addon      remote\tM (id: M-1.0.1)\s
                 addon  downloaded\tN (id: N-1.0.1-HF08-SNAPSHOT)\s
                 addon  downloaded\tNXP-24507-A (id: NXP-24507-A-1.0.0)\s
                 addon  downloaded\tNXP-24507-B (id: NXP-24507-B-1.0.0)\s
                 addon      remote\tprivA (id: privA-1.0.1) [PRIVATE (Owner: customer1)]
                 addon      remote\tprivB (id: privB-1.0.0-SNAPSHOT) [PRIVATE (Owner: customer1)]
                """;
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testListAllPackagesWithPlatformRange() throws Exception {
        // GIVEN we are unregistered and current target platform is server-11.2
        Environment environment = Environment.getDefault();
        environment.setProperty(Environment.DISTRIBUTION_NAME, "server");
        environment.setProperty(Environment.DISTRIBUTION_VERSION, "11.2");
        connectBroker = new ConnectBroker(environment);
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);

        // WHEN trying to list all packages
        connectBroker.pkgListAll();

        // THEN it shows all expected packages with "[REGISTRATION REQUIRED]" on relevant packages
        String expectedLogs = """
                All packages:
                studio     started\tstudioA (id: studioA-1.0.0)\s
                studio  downloaded\tstudioA (id: studioA-1.0.1)\s
                studio  downloaded\tstudioA (id: studioA-1.0.2-SNAPSHOT)\s
                hotfix     started\thfA (id: hfA-1.0.0)\s
                hotfix  downloaded\thfA (id: hfA-1.0.8)\s
                hotfix  downloaded\thfB (id: hfB-1.0.0)\s
                hotfix  downloaded\thfC (id: hfC-1.0.0-SNAPSHOT)\s
                 addon     started\tA (id: A-1.0.0)\s
                 addon  downloaded\tA (id: A-1.2.0)\s
                 addon  downloaded\tA (id: A-1.2.1-SNAPSHOT)\s
                 addon  downloaded\tA (id: A-1.2.2-SNAPSHOT)\s
                 addon  downloaded\tA (id: A-1.2.2)\s
                 addon  downloaded\tA (id: A-1.2.3-SNAPSHOT)\s
                 addon     started\tB (id: B-1.0.1-SNAPSHOT)\s
                 addon  downloaded\tB (id: B-1.0.1)\s
                 addon  downloaded\tB (id: B-1.0.2)\s
                 addon     started\tC (id: C-1.0.0)\s
                 addon  downloaded\tC (id: C-1.0.1-SNAPSHOT)\s
                 addon  downloaded\tC (id: C-1.0.2-SNAPSHOT)\s
                 addon     started\tD (id: D-1.0.2-SNAPSHOT)\s
                 addon  downloaded\tD (id: D-1.0.3-SNAPSHOT)\s
                 addon  downloaded\tD (id: D-1.0.4-SNAPSHOT)\s
                 addon     started\tG (id: G-1.0.1-SNAPSHOT)\s
                 addon     started\tH (id: H-1.0.1-SNAPSHOT)\s
                 addon     started\tJ (id: J-1.0.1)\s
                 addon     started\tK (id: K-1.0.0-SNAPSHOT)\s
                 addon  downloaded\tM (id: M-1.0.0-SNAPSHOT)\s
                 addon  downloaded\tN (id: N-1.0.1-HF08-SNAPSHOT)\s
                 addon  downloaded\tNXP-24507-A (id: NXP-24507-A-1.0.0)\s
                 addon  downloaded\tNXP-24507-B (id: NXP-24507-B-1.0.0)\s
                 addon      remote\tP (id: P-1.0.1) [REGISTRATION REQUIRED]
                 addon      remote\tT (id: T-1.0.1) [REGISTRATION REQUIRED]
                """;
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // GIVEN we are registered
        LogicalInstanceIdentifier CLID = new LogicalInstanceIdentifier("toto--titi", "myInstance");
        CLID.save();

        // WHEN trying to list all packages
        connectBroker.refreshCache();
        connectBroker.pkgListAll();

        // THEN it shows all expected packages without the "[REGISTRATION REQUIRED]"
        expectedLogs = """
                All packages:
                studio     started\tstudioA (id: studioA-1.0.0)\s
                studio  downloaded\tstudioA (id: studioA-1.0.1)\s
                studio  downloaded\tstudioA (id: studioA-1.0.2-SNAPSHOT)\s
                hotfix     started\thfA (id: hfA-1.0.0)\s
                hotfix  downloaded\thfA (id: hfA-1.0.8)\s
                hotfix  downloaded\thfB (id: hfB-1.0.0)\s
                hotfix  downloaded\thfC (id: hfC-1.0.0-SNAPSHOT)\s
                 addon     started\tA (id: A-1.0.0)\s
                 addon  downloaded\tA (id: A-1.2.0)\s
                 addon  downloaded\tA (id: A-1.2.1-SNAPSHOT)\s
                 addon  downloaded\tA (id: A-1.2.2-SNAPSHOT)\s
                 addon  downloaded\tA (id: A-1.2.2)\s
                 addon  downloaded\tA (id: A-1.2.3-SNAPSHOT)\s
                 addon     started\tB (id: B-1.0.1-SNAPSHOT)\s
                 addon  downloaded\tB (id: B-1.0.1)\s
                 addon  downloaded\tB (id: B-1.0.2)\s
                 addon     started\tC (id: C-1.0.0)\s
                 addon  downloaded\tC (id: C-1.0.1-SNAPSHOT)\s
                 addon  downloaded\tC (id: C-1.0.2-SNAPSHOT)\s
                 addon     started\tD (id: D-1.0.2-SNAPSHOT)\s
                 addon  downloaded\tD (id: D-1.0.3-SNAPSHOT)\s
                 addon  downloaded\tD (id: D-1.0.4-SNAPSHOT)\s
                 addon     started\tG (id: G-1.0.1-SNAPSHOT)\s
                 addon     started\tH (id: H-1.0.1-SNAPSHOT)\s
                 addon     started\tJ (id: J-1.0.1)\s
                 addon     started\tK (id: K-1.0.0-SNAPSHOT)\s
                 addon  downloaded\tM (id: M-1.0.0-SNAPSHOT)\s
                 addon  downloaded\tN (id: N-1.0.1-HF08-SNAPSHOT)\s
                 addon  downloaded\tNXP-24507-A (id: NXP-24507-A-1.0.0)\s
                 addon  downloaded\tNXP-24507-B (id: NXP-24507-B-1.0.0)\s
                 addon      remote\tP (id: P-1.0.1)\s
                 addon      remote\tprivD (id: privD-1.0.1) [PRIVATE (Owner: customer1)]
                 addon      remote\tT (id: T-1.0.1)\s
                """;
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testShowPackages() throws Exception {
        // GIVEN we are unregistered

        // WHEN trying to show packages properties
        connectBroker.pkgShow(List.of("A-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "M-1.0.1", "unknown-package"));
        connectBroker.getCommandSet().log();

        // THEN it shows all expected properties
        String expectedLogs = """
                ****************************************
                Package: A-1.0.0
                State: started
                Version: 1.0.0
                Name: A
                Type: addon
                Target platforms: {server-8.3,server-8.4}
                Supports hot-reload: false
                Title: Package A
                Description: Description of A
                License: LGPL
                License URL: http://www.gnu.org/licenses/lgpl.html
                ****************************************
                Package: studioA-1.0.1
                State: downloaded
                Version: 1.0.1
                Name: studioA
                Type: studio
                Target platforms: {server-8.3,server-8.4}
                Supports hot-reload: false
                Title: Studio A
                Description: Description of studioA
                License: LGPL
                License URL: http://www.gnu.org/licenses/lgpl.html
                ****************************************
                Package: hfA-1.0.0
                State: started
                Version: 1.0.0
                Name: hfA
                Type: hotfix
                Target platforms: {server-8.3}
                Supports hot-reload: false
                Title: Hot fix NXP
                Description: Hot Fix for NXP
                License: LGPL
                License URL: http://www.gnu.org/licenses/lgpl.html
                ****************************************
                Package: M-1.0.1
                State: remote [REGISTRATION REQUIRED]
                Version: 1.0.1
                Name: M
                Type: addon
                Target platforms: {server-8.3,server-8.4}
                Supports hot-reload: false
                Title: Package M
                Description: description of M
                ****************************************
                \tCould not find a remote or local (relative to current directory or to NUXEO_HOME) package with name or ID unknown-package""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // GIVEN we are registered
        LogicalInstanceIdentifier CLID = new LogicalInstanceIdentifier("toto--titi", "myInstance");
        CLID.save();

        // WHEN trying to show packages properties
        connectBroker.refreshCache();
        connectBroker.pkgShow(List.of("privA-1.0.1", "M-1.0.1"));

        // THEN it shows all expected properties
        expectedLogs = """
                ****************************************
                Package: privA-1.0.1
                State: remote [PRIVATE (Owner: customer1)]
                Version: 1.0.1
                Name: privA
                Type: addon
                Target platforms: {server-8.3,server-8.4}
                Supports hot-reload: false
                Title: Package privA
                Description: description of privA
                ****************************************
                Package: M-1.0.1
                State: remote\s
                Version: 1.0.1
                Name: M
                Type: addon
                Target platforms: {server-8.3,server-8.4}
                Supports hot-reload: false
                Title: Package M
                Description: description of M
                ****************************************""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testDownloadUnknownPackage() {
        // GIVEN a non existing package
        checkPackagesState(null, "unknown-package");

        // WHEN trying to download it
        boolean isSuccessful = connectBroker.downloadPackages(new ArrayList<>(List.of("unknown-package")));
        assertThat(isSuccessful).isFalse();

        // THEN it fails and the package is still unknown
        checkPackagesState(null, "unknown-package");
        connectBroker.getCommandSet().log();
        String expectedLogs = """
                Downloading [unknown-package]...
                \tDownload failed (not found).""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testDownloadSubscriptionRequiredPackage() {
        // GIVEN a remote package with subscription required
        checkPackagesState(PackageState.REMOTE, "M-1.0.1");

        // WHEN trying to download it
        boolean isSuccessful = connectBroker.downloadPackages(new ArrayList<>(Collections.singleton("M-1.0.1")));
        assertThat(isSuccessful).isFalse();

        // THEN it fails and the package is still remote
        checkPackagesState(PackageState.REMOTE, "M-1.0.1");
        connectBroker.getCommandSet().log();
        String expectedLogs = """
                Downloading [M-1.0.1]...
                \tRegistration required.""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // GIVEN a remote downloaded snapshot package with subscription required
        checkPackagesState(PackageState.DOWNLOADED, "M-1.0.0-SNAPSHOT");

        // WHEN trying to re-download it
        isSuccessful = connectBroker.downloadPackages(new ArrayList<>(Collections.singleton("M-1.0.0-SNAPSHOT")));
        assertThat(isSuccessful).isTrue();

        // THEN request is successful but the download is skipped and a message is displayed
        checkPackagesState(PackageState.DOWNLOADED, "M-1.0.0-SNAPSHOT");
        expectedLogs = "Registration is required for package 'M-1.0.0-SNAPSHOT'. Download skipped."; //
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);

    }

    // NXP-24507
    @Test
    public void testPkgRequest_restartLauncherWithoutPendingCommand() {
        // Given 1 downloaded package, which requires launcher restart
        checkPackagesState(PackageState.DOWNLOADED, "NXP-24507-A-1.0.0");

        // When handling the install request
        try {
            connectBroker.pkgRequest(null, singletonList("NXP-24507-A-1.0.0"), null, null, true, false);
            fail();
        } catch (LauncherRestartException e) {
            // Then restarting launcher is required
            assertThat(connectBroker.isRestartRequired()).isTrue();
        }
        // And no file is created for pending changes (0 remaining).
        Path pending = connectBroker.getPendingFile();
        assertThat(pending).doesNotExist();
    }

    // NXP-24507
    @Test
    public void testPkgRequest_restartLauncherWithOnePendingCommand() {
        // Given 2 downloaded packages, where package A requires launcher restart
        String pkgA = "NXP-24507-A-1.0.0";
        String pkgB = "NXP-24507-B-1.0.0";
        checkPackagesState(PackageState.DOWNLOADED, pkgA, pkgB);

        // When handling the install request
        try {
            connectBroker.pkgRequest(null, List.of(pkgA, pkgB), null, null, true, false);
            fail();
        } catch (LauncherRestartException e) {
            // Then restarting launcher is required
            assertThat(connectBroker.isRestartRequired()).isTrue();
        }
        // And package A is installed
        checkPackagesState(PackageState.STARTED, pkgA);
        // And a file is created for pending changes
        Path pending = connectBroker.getPendingFile();
        assertThat(pending).hasContent("install " + pkgB);
    }

    // NXP-24507
    @Test
    public void testPkgInstall_restartLauncherWithoutPendingCommand() {
        // Given 1 downloaded package, which requires launcher restart
        checkPackagesState(PackageState.DOWNLOADED, "NXP-24507-A-1.0.0");

        Path pending = connectBroker.getPendingFile();
        assertThat(pending).doesNotExist();

        // When handling the install request
        try {
            connectBroker.pkgInstall(singletonList("NXP-24507-A-1.0.0"), false);
            fail();
        } catch (LauncherRestartException e) {
            // Then restarting launcher is required
            assertThat(connectBroker.isRestartRequired()).isTrue();
        }
        // And package A is installed
        checkPackagesState(PackageState.STARTED, "NXP-24507-A-1.0.0");
        // And no file is created for pending changes (0 remaining).
        pending = connectBroker.getPendingFile();
        assertThat(pending).doesNotExist();
    }

    // NXP-24507
    @Test
    public void testPkgInstall_restartLauncherWithOnePendingCommand() {
        // Given 2 downloaded packages, where package A requires launcher restart
        checkPackagesState(PackageState.DOWNLOADED, "NXP-24507-A-1.0.0");

        // When handling the install request
        try {
            connectBroker.pkgInstall(List.of("NXP-24507-A-1.0.0", "B", "C"), false);
            fail();
        } catch (LauncherRestartException e) {
            // Then restarting launcher is required
            assertThat(connectBroker.isRestartRequired()).isTrue();
        }
        // And package A is installed
        checkPackagesState(PackageState.STARTED, "NXP-24507-A-1.0.0");
        // And a file is created for pending changes
        Path pending = connectBroker.getPendingFile();
        assertThat(pending).hasContent("install B\ninstall C\n");
    }

    @Test
    public void testPkgUninstall_restartLauncherWithoutPendingCommand() {
        // Given 1 started package, which requires launcher restart
        connectBroker.pkgInstall("NXP-24507-A-1.0.0", false);
        checkPackagesState(PackageState.STARTED, "NXP-24507-A-1.0.0");

        // When handling the uninstall request
        try {
            connectBroker.pkgUninstall(singletonList("NXP-24507-A-1.0.0"));
            fail();
        } catch (LauncherRestartException e) {
            // Then restarting launcher is required
            assertThat(connectBroker.isRestartRequired()).isTrue();
        }
        // And package A is uninstalled
        checkPackagesState(PackageState.DOWNLOADED, "NXP-24507-A-1.0.0");
        assertThat(connectBroker.getPendingFile()).doesNotExist();
    }

    @Test
    public void testPkgUninstall_restartLauncherWithOnePendingCommand() {
        // Given 2 started packages, where A requires launcher restart
        String pkgA = "NXP-24507-A-1.0.0";
        String pkgB = "NXP-24507-B-1.0.0";
        connectBroker.pkgInstall(pkgA, false);
        connectBroker.pkgInstall(pkgB, false);
        checkPackagesState(PackageState.STARTED, pkgA, pkgB);

        // When handling the uninstall request
        try {
            connectBroker.pkgUninstall(List.of(pkgA, pkgB));
            fail();
        } catch (LauncherRestartException e) {
            // Then restarting launcher is required
            assertThat(connectBroker.isRestartRequired()).isTrue();
        }
        // And package A is uninstalled, package B is pending
        checkPackagesState(PackageState.DOWNLOADED, pkgA);
        assertThat(connectBroker.getPendingFile()).hasContent("uninstall " + pkgB);
    }

    @Test
    public void testPersistPendingCommand_createNewFile() throws Exception {
        // Given a nonexistent path for pending commands
        Path path = connectBroker.getPendingFile();
        assertThat(path).doesNotExist();

        // When persist new pending commands
        List.of("L1", "L2").forEach(connectBroker::persistCommand);

        // Then the file is created: new commands are present
        assertThat(Files.readAllLines(path)).containsExactly("L1", "L2");
    }

    @Test
    public void testPersistPendingCommand_appendExistingFile() throws Exception {
        // Given an existing path for pending commands
        Path path = connectBroker.getPendingFile();
        Files.write(path, List.of("L1", "L2"));

        // When persist new pending commands
        List.of("L3", "L4").forEach(connectBroker::persistCommand);

        // Then the file is created: both old and new commands are present
        assertThat(Files.readAllLines(path)).containsExactly("L1", "L2", "L3", "L4");
    }

    @Test
    public void testExecutePending_resumeCommands() throws Exception {
        // Given an exiting path for pending commands
        Path path = connectBroker.getPendingFile();
        Files.write(path, List.of("install A-1.2.0", "install B-1.0.1"));

        // When executing the pending changes
        connectBroker.executePending(path.toFile(), true, true, false);

        // Then the packages are installed and started
        checkPackagesState(PackageState.STARTED, "A-1.2.0", "B-1.0.1");
    }

    // NXP-24507
    @Test
    public void testExecutePending_restartAgain() throws Exception {
        // Given an exiting path for pending commands
        String pkgA = "NXP-24507-A-1.0.0";
        String pkgB = "NXP-24507-B-1.0.0";
        Path path = connectBroker.getPendingFile();
        Files.write(path, List.of("install " + pkgA, "install " + pkgB));

        // When executing the pending changes
        try {
            connectBroker.executePending(path.toFile(), true, true, false);
            fail("LauncherRestartException didn't thrown, isRestartRequired=" + connectBroker.isRestartRequired());
        } catch (LauncherRestartException e) {
            // Then restarting launcher is required
            assertThat(connectBroker.isRestartRequired()).isTrue();
            // And package A is installed and package B is pending
            checkPackagesState(PackageState.STARTED, pkgA);
            assertThat(path).hasContent("install " + pkgB);
        }

        // When launcher is restarted and executes the pending changes again
        // (hack: System.exit(int) is replaced by property changes)
        environment.setProperty(LAUNCHER_CHANGED_PROPERTY, "false");
        boolean result = connectBroker.executePending(path.toFile(), true, true, false);

        // Then execution is successful and both packages are installed
        assertThat(result).isTrue();
        checkPackagesState(PackageState.STARTED, pkgA, pkgB);
    }

    @Test
    public void testIsRemotePackageId() {
        assertThat(connectBroker.isRemotePackageId("A-1.0.0")).isTrue();
        assertThat(connectBroker.isRemotePackageId("B-1.0.1-SNAPSHOT")).isTrue();
        assertThat(connectBroker.isRemotePackageId("studioA-1.0.0")).isTrue();
        assertThat(connectBroker.isRemotePackageId("hfA-1.0.8")).isTrue();

        // Packages in path "$TEST_STORE_PATH/local-only" are not available in remote (local-only)
        assertThat(connectBroker.isRemotePackageId("E-1.0.1")).isFalse();
        assertThat(connectBroker.isRemotePackageId("F.1.0.0-SNAPSHOT")).isFalse();
        assertThat(connectBroker.isRemotePackageId("K.1.0.0-SNAPSHOT")).isFalse();
        assertThat(connectBroker.isRemotePackageId("unknown-package")).isFalse();
        assertThat(connectBroker.isRemotePackageId("NXP-24507-A-1.0.0")).isFalse();
        assertThat(connectBroker.isRemotePackageId("NXP-24507-B-1.0.0")).isFalse();
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testInstallPackageRequest() {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // B-1.0.2 is not available on platform version server-8.3
        assertThat(connectBroker.pkgRequest(null, List.of("A-1.2.0", "B-1.0.2"), null, null, true, false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "org.nuxeo.connect.update.PackageException: Package(s) B-1.0.2 not available on platform version server-8.3 (relax is not allowed)";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        connectBroker.setRelax("true");
        // restriction on target platform must be ignored and B-1.0.2 must be installed before A-1.2.0 because of
        // optional dependencies
        assertThat(connectBroker.pkgRequest(null, List.of("A-1.2.0", "B-1.0.2"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.0, B-1.0.2, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.2.0", "B-1.0.2", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT",
                        "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = """
                Relax restriction to target platform server-8.3 because of package(s) B-1.0.2

                Dependency resolution:
                  Installation order (2):        B-1.0.2/A-1.2.0
                  Unchanged packages (8):        hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Packages to upgrade (2):       A:1.0.0, B:1.0.1-SNAPSHOT
                  Local packages to install (2): A:1.2.0, B:1.0.2

                Uninstalling B-1.0.1-SNAPSHOT
                Uninstalling A-1.0.0
                Installing B-1.0.2
                Installing A-1.2.0""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // SNAPSHOTS must be replaced in local cache before installation and D-1.0.4-SNAPSHOT must be installed after
        // C-1.0.2-SNAPSHOT because of optional dependencies
        assertThat(connectBroker.pkgRequest(null, List.of("A-1.2.2-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.2-SNAPSHOT, B-1.0.2, C-1.0.2-SNAPSHOT, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.2.2-SNAPSHOT", "B-1.0.2",
                "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.0", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT", "B-1.0.1",
                        "C-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = """
                The following SNAPSHOT package(s) will be replaced in local cache (if available): [A-1.2.2-SNAPSHOT, C-1.0.2-SNAPSHOT, D-1.0.4-SNAPSHOT]
                Download of 'A-1.2.2-SNAPSHOT' will replace the one already in local cache.
                Downloading [A-1.2.2-SNAPSHOT]...
                Replacement of A-1.2.2-SNAPSHOT in local cache...
                Added A-1.2.2-SNAPSHOT
                Download of 'C-1.0.2-SNAPSHOT' will replace the one already in local cache.
                Downloading [C-1.0.2-SNAPSHOT]...
                Replacement of C-1.0.2-SNAPSHOT in local cache...
                Added C-1.0.2-SNAPSHOT
                Download of 'D-1.0.4-SNAPSHOT' will replace the one already in local cache.
                Downloading [D-1.0.4-SNAPSHOT]...
                Replacement of D-1.0.4-SNAPSHOT in local cache...
                Added D-1.0.4-SNAPSHOT
                Relax restriction to target platform server-8.3 because of package(s) A-1.2.2-SNAPSHOT, C-1.0.2-SNAPSHOT, D-1.0.4-SNAPSHOT

                Dependency resolution:
                  Installation order (3):        A-1.2.2-SNAPSHOT/D-1.0.4-SNAPSHOT/C-1.0.2-SNAPSHOT
                  Unchanged packages (7):        B:1.0.2, hfA:1.0.0, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Packages to upgrade (3):       A:1.2.0, C:1.0.0, D:1.0.2-SNAPSHOT
                  Local packages to install (3): A:1.2.2-SNAPSHOT, C:1.0.2-SNAPSHOT, D:1.0.4-SNAPSHOT

                Uninstalling C-1.0.0
                Uninstalling D-1.0.2-SNAPSHOT
                Uninstalling A-1.2.0
                Installing A-1.2.2-SNAPSHOT
                Installing D-1.0.4-SNAPSHOT
                Installing C-1.0.2-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    private String getExpectedLogsForOnePkgAndItsDep(String pkg, String pkgVersion, String dep, String depVersion) {
        return """

                Dependency resolution:
                  Installation order (2):        %s-%s/%s-%s
                  Unchanged packages (10):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Packages to download (2):      %s:%s, %s:%s

                Downloading [%s-%s, %s-%s]...
                Aborting packages change request""".formatted(
                dep, depVersion, pkg, pkgVersion, pkg, pkgVersion, dep, depVersion, pkg, pkgVersion, dep, depVersion);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testInstallPackageRequestSnapshotDependenciesResolution() throws Exception {
        Environment environment = Environment.getDefault();
        environment.setProperty(Environment.DISTRIBUTION_NAME, "server");
        environment.setProperty(Environment.DISTRIBUTION_VERSION, "11.3");
        connectBroker = new ConnectBroker(environment);
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBroker.setAllowSNAPSHOT(false);

        // U-1.0.0 depends on V:1.0.0+ so it should resolve to V-1.0.0 (snapshots are not allowed)
        connectBroker.pkgRequest(null, singletonList("U-1.0.0"), null, null, true, false);

        // check logs
        String expectedLogs = getExpectedLogsForOnePkgAndItsDep("U", "1.0.0", "V", "1.0.0");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // U-1.0.1-SNAPSHOT depends on V:1.0.1-SNAPSHOT+ so it should fail because there is no match with snapshots not
        // allowed
        connectBroker.pkgRequest(null, singletonList("U-1.0.1-SNAPSHOT"), null, null, true, false);

        // check logs
        expectedLogs = "\nFailed to resolve dependencies: Couldn't order [U-1.0.1-SNAPSHOT] missing [V:1.0.1-SNAPSHOT] (consider using --relax true or --snapshot).";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // UU-1.0.0 depends on VV so it should resolve to VV-1.0.0 (snapshots are not allowed)
        connectBroker.pkgRequest(null, singletonList("UU-1.0.0"), null, null, true, false);

        // check logs
        expectedLogs = getExpectedLogsForOnePkgAndItsDep("UU", "1.0.0", "VV", "1.0.0");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // UU-1.0.1-SNAPSHOT depends on VV so it should resolve to VV-1.0.0 (snapshots are not allowed and not
        // explicitly required)
        connectBroker.pkgRequest(null, singletonList("UU-1.0.1-SNAPSHOT"), null, null, true, false);

        // check logs
        expectedLogs = getExpectedLogsForOnePkgAndItsDep("UU", "1.0.1-SNAPSHOT", "VV", "1.0.0");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // UUU-1.0.0 depends on VVV:1.0.0:1.0.0 so it should resolve to VVV-1.0.0 (explicitly required)
        connectBroker.pkgRequest(null, singletonList("UUU-1.0.0"), null, null, true, false);

        // check logs
        expectedLogs = getExpectedLogsForOnePkgAndItsDep("UUU", "1.0.0", "VVV", "1.0.0");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // UUU-1.0.1-SNAPSHOT depends on VVV:1.0.1-SNAPSHOT:1.0.1-SNAPSHOT so it should resolve to VVV-1.0.1-SNAPSHOT
        // (explicitly required)
        connectBroker.pkgRequest(null, singletonList("UUU-1.0.1-SNAPSHOT"), null, null, true, false);

        // check logs
        expectedLogs = getExpectedLogsForOnePkgAndItsDep("UUU", "1.0.1-SNAPSHOT", "VVV", "1.0.1-SNAPSHOT");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        /* SET SNAPSHOT ALLOWED */
        connectBroker.setAllowSNAPSHOT(true);

        // U-1.0.0 depends on V:1.0.0+ so it should resolve to V-1.0.1-SNAPSHOT
        connectBroker.pkgRequest(null, singletonList("U-1.0.0"), null, null, true, false);

        // check logs
        expectedLogs = getExpectedLogsForOnePkgAndItsDep("U", "1.0.0", "V", "1.0.1-SNAPSHOT");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // U-1.0.1-SNAPSHOT depends on V:1.0.1-SNAPSHOT+ so it should resolve to V-1.0.1-SNAPSHOT
        connectBroker.pkgRequest(null, singletonList("U-1.0.1-SNAPSHOT"), null, null, true, false);

        // check logs
        expectedLogs = getExpectedLogsForOnePkgAndItsDep("U", "1.0.1-SNAPSHOT", "V", "1.0.1-SNAPSHOT");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // UU-1.0.0 depends on VV so it should resolve to VV-1.0.1-SNAPSHOT (snapshots are allowed)
        connectBroker.pkgRequest(null, singletonList("UU-1.0.0"), null, null, true, false);

        // check logs
        expectedLogs = getExpectedLogsForOnePkgAndItsDep("UU", "1.0.0", "VV", "1.0.1-SNAPSHOT");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // UU-1.0.1-SNAPSHOT depends on VV so it should resolve to VV-1.0.1-SNAPSHOT
        connectBroker.pkgRequest(null, singletonList("UU-1.0.1-SNAPSHOT"), null, null, true, false);

        // check logs
        expectedLogs = getExpectedLogsForOnePkgAndItsDep("UU", "1.0.1-SNAPSHOT", "VV", "1.0.1-SNAPSHOT");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // UUU-1.0.0 depends on VVV:1.0.0:1.0.0 so it should resolve to VVV-1.0.0 (explicitly required)
        connectBroker.pkgRequest(null, singletonList("UUU-1.0.0"), null, null, true, false);

        // check logs
        expectedLogs = getExpectedLogsForOnePkgAndItsDep("UUU", "1.0.0", "VVV", "1.0.0");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // UUU-1.0.1-SNAPSHOT depends on VVV:1.0.1-SNAPSHOT:1.0.1-SNAPSHOT so it should resolve to VVV-1.0.1-SNAPSHOT
        // (explicitly required)
        connectBroker.pkgRequest(null, singletonList("UUU-1.0.1-SNAPSHOT"), null, null, true, false);

        // check logs
        expectedLogs = getExpectedLogsForOnePkgAndItsDep("UUU", "1.0.1-SNAPSHOT", "VVV", "1.0.1-SNAPSHOT");
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testInstallPackageRequestWithMissingDependencies() throws Exception {
        Environment environment = Environment.getDefault();
        environment.setProperty(Environment.DISTRIBUTION_NAME, "server");
        environment.setProperty(Environment.DISTRIBUTION_VERSION, "8.4");
        connectBroker = new ConnectBroker(environment);
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // L-1.0.2 is missing hfB not available on platform version server-8.4
        assertThat(connectBroker.pkgRequest(null, singletonList("L-1.0.2"), null, null, true, false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "\nFailed to resolve dependencies: Couldn't order [L-1.0.2] missing [hfB] (consider using --relax true or --snapshot).";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // L-1.0.3 (last version of L) depends on hfD which is missing hfB not available on platform version server-8.4
        assertThat(connectBroker.pkgRequest(null, singletonList("L"), null, null, true, false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = "\nFailed to resolve dependencies: Couldn't order [L-1.0.3, hfD-1.0.0] missing [hfB, hfD] (consider using --relax true or --snapshot).";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testInstallLocalPackageRequest() {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // F-1.0.0-SNAPSHOT is not available on platform version server-8.3
        assertThat(connectBroker.pkgRequest(null,
                List.of(TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip", TEST_LOCAL_ONLY_PATH + "/E-1.0.1"), null, null,
                true, false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT", "E-1.0.1",
                        "F-1.0.0-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check that local files and directory have not been removed
        assertThat(new File(TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip").exists()).isTrue();
        assertThat(new File(TEST_LOCAL_ONLY_PATH + "/E-1.0.1").exists()).isTrue();

        // check logs
        String expectedLogs = """
                Added %s/F-1.0.0-SNAPSHOT.zip
                Added %s/E-1.0.1
                org.nuxeo.connect.update.PackageException: Package(s) F-1.0.0-SNAPSHOT not available on platform version server-8.3 (relax is not allowed)""".formatted(
                TEST_LOCAL_ONLY_PATH, TEST_LOCAL_ONLY_PATH);
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        connectBroker.setRelax("true");
        assertThat(connectBroker.pkgRequest(null,
                List.of(TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip", TEST_LOCAL_ONLY_PATH + "/E-1.0.1"), null, null,
                true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.0, B-1.0.2, C-1.0.0, D-1.0.2-SNAPSHOT, E-1.0.1,
        // F-1.0.0-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "E-1.0.1", "F-1.0.0-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = """
                The following SNAPSHOT package(s) will be replaced in local cache (if available): [src/test/resources/packages/store/local-only/F-1.0.0-SNAPSHOT.zip]
                Replacement of F-1.0.0-SNAPSHOT in local cache...
                Added src/test/resources/packages/store/local-only/F-1.0.0-SNAPSHOT.zip
                Relax restriction to target platform server-8.3 because of package(s) F-1.0.0-SNAPSHOT

                Dependency resolution:
                  Installation order (2):        E-1.0.1/F-1.0.0-SNAPSHOT
                  Unchanged packages (10):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Local packages to install (2): E:1.0.1, F:1.0.0-SNAPSHOT

                Installing E-1.0.1
                Installing F-1.0.0-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testInstallLocalPackageRequestWithRange() throws Exception {
        // GIVEN current target platform is server-11.2
        Environment environment = Environment.getDefault();
        environment.setProperty(Environment.DISTRIBUTION_NAME, "server");
        environment.setProperty(Environment.DISTRIBUTION_VERSION, "11.2.3");
        connectBroker = new ConnectBroker(environment);
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // Q-1.0.0 is not available on platform version server-11.2.3
        assertThat(connectBroker.pkgRequest(null, List.of(TEST_LOCAL_ONLY_PATH + "/Q-1.0.0.zip",
                TEST_LOCAL_ONLY_PATH + "/R-1.0.2-SNAPSHOT", TEST_LOCAL_ONLY_PATH + "/S-1.0.0.zip"), null, null, true,
                false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT", "Q-1.0.0",
                        "R-1.0.2-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check that local files and directory have not been removed
        assertThat(new File(TEST_LOCAL_ONLY_PATH + "/Q-1.0.0.zip").exists()).isTrue();
        assertThat(new File(TEST_LOCAL_ONLY_PATH + "/R-1.0.2-SNAPSHOT").exists()).isTrue();
        assertThat(new File(TEST_LOCAL_ONLY_PATH + "/S-1.0.0.zip").exists()).isTrue();

        // check logs
        String expectedLogs = """
                Added %s/Q-1.0.0.zip
                Added %s/R-1.0.2-SNAPSHOT
                Added %s/S-1.0.0.zip
                org.nuxeo.connect.update.PackageException: Package(s) Q-1.0.0, R-1.0.2-SNAPSHOT not available on platform version server-11.2.3 (relax is not allowed)""".formatted(
                TEST_LOCAL_ONLY_PATH, TEST_LOCAL_ONLY_PATH, TEST_LOCAL_ONLY_PATH);
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        connectBroker.setRelax("true");
        assertThat(connectBroker.pkgRequest(null, List.of(TEST_LOCAL_ONLY_PATH + "/Q-1.0.0.zip",
                TEST_LOCAL_ONLY_PATH + "/R-1.0.2-SNAPSHOT", TEST_LOCAL_ONLY_PATH + "/S-1.0.0.zip"), null, null, true,
                false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.0, B-1.0.2, C-1.0.0, D-1.0.2-SNAPSHOT, E-1.0.1,
        // F-1.0.0-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "Q-1.0.0", "R-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = """
                The following SNAPSHOT package(s) will be replaced in local cache (if available): [src/test/resources/packages/store/local-only/R-1.0.2-SNAPSHOT]
                Replacement of R-1.0.2-SNAPSHOT in local cache...
                Added src/test/resources/packages/store/local-only/R-1.0.2-SNAPSHOT
                Relax restriction to target platform server-11.2.3 because of package(s) Q-1.0.0, R-1.0.2-SNAPSHOT

                Dependency resolution:
                  Installation order (3):        Q-1.0.0/R-1.0.2-SNAPSHOT/S-1.0.0
                  Unchanged packages (10):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Local packages to install (3): Q:1.0.0, R:1.0.2-SNAPSHOT, S:1.0.0

                Installing Q-1.0.0
                Installing R-1.0.2-SNAPSHOT
                Installing S-1.0.0""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testReInstallPackageRequest() {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // A-1.0.0 and C-1.0.0 are releases and are already installed
        assertThat(connectBroker.pkgRequest(null, List.of("A-1.0.0", "C"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """

                Dependency resolution:
                  Unchanged packages (10):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                """;
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // B-1.0.1-SNAPSHOT and D-1.0.2-SNAPSHOT must be uninstalled then reinstalled as they are SNAPSHOTS
        // C-1.0.0 must be reinstalled as it has an optional dependency on D
        assertThat(connectBroker.pkgRequest(null, List.of("B-1.0.1-SNAPSHOT", "D"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = """
                The following SNAPSHOT package(s) will be replaced in local cache (if available): [D-1.0.2-SNAPSHOT, B-1.0.1-SNAPSHOT]
                Uninstalling D-1.0.2-SNAPSHOT
                Uninstalling B-1.0.1-SNAPSHOT
                Download of 'D-1.0.2-SNAPSHOT' will replace the one already in local cache.
                Downloading [D-1.0.2-SNAPSHOT]...
                Replacement of D-1.0.2-SNAPSHOT in local cache...
                Added D-1.0.2-SNAPSHOT
                Download of 'B-1.0.1-SNAPSHOT' will replace the one already in local cache.
                Downloading [B-1.0.1-SNAPSHOT]...
                Replacement of B-1.0.1-SNAPSHOT in local cache...
                Added B-1.0.1-SNAPSHOT

                As package 'C-1.0.0' has an optional dependency on package(s) [D-1.0.2-SNAPSHOT] currently being installed, it will be reinstalled.
                Dependency resolution:
                  Installation order (3):        B-1.0.1-SNAPSHOT/D-1.0.2-SNAPSHOT/C-1.0.0
                  Uninstallation order (1):      C-1.0.0
                  Unchanged packages (7):        A:1.0.0, hfA:1.0.0, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Local packages to install (3): B:1.0.1-SNAPSHOT, C:1.0.0, D:1.0.2-SNAPSHOT
                  Local packages to remove (1):  C:1.0.0

                Uninstalling C-1.0.0
                Installing B-1.0.1-SNAPSHOT
                Installing D-1.0.2-SNAPSHOT
                Installing C-1.0.0""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testReInstallLocalPackageRequest() {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // A-1.0.0 is a release and is already installed
        assertThat(connectBroker.pkgRequest(null, singletonList(TEST_STORE_PATH + "/A-1.0.0.zip"), null, null, true,
                false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """

                Dependency resolution:
                  Unchanged packages (10):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                """;
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // B-1.0.1-SNAPSHOT must be uninstalled then reinstalled as it is a SNAPSHOT
        // it must be replaced in local cache because a new file is provided
        assertThat(connectBroker.pkgRequest(null, singletonList(TEST_STORE_PATH + "/B-1.0.1-SNAPSHOT.zip"), null, null,
                true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = """
                The following SNAPSHOT package(s) will be replaced in local cache (if available): [src/test/resources/packages/store/B-1.0.1-SNAPSHOT.zip]
                Uninstalling B-1.0.1-SNAPSHOT
                Replacement of B-1.0.1-SNAPSHOT in local cache...
                Added src/test/resources/packages/store/B-1.0.1-SNAPSHOT.zip

                Dependency resolution:
                  Installation order (1):        B-1.0.1-SNAPSHOT
                  Unchanged packages (9):        A:1.0.0, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Local packages to install (1): B:1.0.1-SNAPSHOT

                Installing B-1.0.1-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // K-1.0.0-SNAPSHOT must be uninstalled then reinstalled as it is a SNAPSHOT, even if not available remotely
        // it must be replaced in local cache because a new file is provided
        assertThat(connectBroker.pkgRequest(null, singletonList(TEST_LOCAL_ONLY_PATH + "/K-1.0.0-SNAPSHOT.zip"), null,
                null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, K-1.0.0-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "K-1.0.0-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = """
                The following SNAPSHOT package(s) will be replaced in local cache (if available): [src/test/resources/packages/store/local-only/K-1.0.0-SNAPSHOT.zip]
                Uninstalling K-1.0.0-SNAPSHOT
                Replacement of K-1.0.0-SNAPSHOT in local cache...
                Added src/test/resources/packages/store/local-only/K-1.0.0-SNAPSHOT.zip

                Dependency resolution:
                  Installation order (1):        K-1.0.0-SNAPSHOT
                  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1
                  Local packages to install (1): K:1.0.0-SNAPSHOT

                Installing K-1.0.0-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // K-1.0.0-SNAPSHOT must be uninstalled then reinstalled as it is a SNAPSHOT, even if not available remotely
        // it must not be replaced in local cache because no new file is provided
        assertThat(connectBroker.pkgRequest(null, singletonList("K"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, K-1.0.0-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "K-1.0.0-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = """
                The following SNAPSHOT package(s) will be replaced in local cache (if available): [K-1.0.0-SNAPSHOT]
                Uninstalling K-1.0.0-SNAPSHOT
                The SNAPSHOT package K-1.0.0-SNAPSHOT is not available remotely, local cache will be used.

                Dependency resolution:
                  Installation order (1):        K-1.0.0-SNAPSHOT
                  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1
                  Local packages to install (1): K:1.0.0-SNAPSHOT

                Installing K-1.0.0-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithRelax() throws Exception {
        environment.setProperty(Environment.DISTRIBUTION_NAME, "nomatching");
        environment.setProperty(Environment.DISTRIBUTION_VERSION, "1.0");
        connectBroker = new ConnectBroker(environment);
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBroker.setAllowSNAPSHOT(false);
        connectBroker.setRelax("true");

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available release version, snapshots must be replaced
        // and optional dependencies order must be respected
        assertThat(connectBroker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.2, B-1.0.2, C-1.0.0, D-1.0.4-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, List.of("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.2", "B-1.0.2",
                "C-1.0.0", "D-1.0.4-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.0", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT",
                        "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """
                Relax restriction to target platform nomatching-1.0 because of package(s) studioA, hfA, A, B, C, D, G, H, J, K

                Dependency resolution:
                  Installation order (7):        A-1.2.2/B-1.0.2/D-1.0.4-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT
                  Unchanged packages (5):        C:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Packages to upgrade (5):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0
                  Local packages to install (5): A:1.2.2, B:1.0.2, hfA:1.0.8, D:1.0.4-SNAPSHOT, studioA:1.0.2-SNAPSHOT

                Uninstalling studioA-1.0.0
                Uninstalling hfA-1.0.0
                Uninstalling D-1.0.2-SNAPSHOT
                Uninstalling B-1.0.1-SNAPSHOT
                Uninstalling A-1.0.0
                Installing A-1.2.2
                Installing B-1.0.2
                Installing D-1.0.4-SNAPSHOT
                Updating package G-1.0.1-SNAPSHOT...
                Uninstalling G-1.0.1-SNAPSHOT
                Removed G-1.0.1-SNAPSHOT
                Downloading [G-1.0.1-SNAPSHOT]...
                Added G-1.0.1-SNAPSHOT
                Installing G-1.0.1-SNAPSHOT
                Updating package H-1.0.1-SNAPSHOT...
                Uninstalling H-1.0.1-SNAPSHOT
                Removed H-1.0.1-SNAPSHOT
                Downloading [H-1.0.1-SNAPSHOT]...
                Added H-1.0.1-SNAPSHOT
                Installing H-1.0.1-SNAPSHOT
                Installing hfA-1.0.8
                Installing studioA-1.0.2-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithRelaxAndSnapshot() throws Exception {
        environment.setProperty(Environment.DISTRIBUTION_NAME, "nomatching");
        environment.setProperty(Environment.DISTRIBUTION_VERSION, "1.0");
        connectBroker = new ConnectBroker(environment);
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBroker.setAllowSNAPSHOT(true);
        connectBroker.setRelax("true");

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available snapshot version, snapshots must be replaced
        // and optional dependencies order must be respected
        assertThat(connectBroker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.3-SNAPSHOT, B-1.0.2, C-1.0.2-SNAPSHOT, D-1.0.4-SNAPSHOT]
        checkPackagesState(
                connectBroker, List.of("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.3-SNAPSHOT", "B-1.0.2",
                        "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.0", "B-1.0.1-SNAPSHOT", "B-1.0.1",
                        "C-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """
                Relax restriction to target platform nomatching-1.0 because of package(s) studioA, hfA, A, B, C, D, G, H, J, K

                Dependency resolution:
                  Installation order (8):        A-1.2.3-SNAPSHOT/B-1.0.2/D-1.0.4-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT/C-1.0.2-SNAPSHOT
                  Unchanged packages (4):        G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Packages to upgrade (6):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0
                  Local packages to install (6): A:1.2.3-SNAPSHOT, B:1.0.2, hfA:1.0.8, C:1.0.2-SNAPSHOT, D:1.0.4-SNAPSHOT, studioA:1.0.2-SNAPSHOT

                Uninstalling C-1.0.0
                Uninstalling studioA-1.0.0
                Uninstalling hfA-1.0.0
                Uninstalling D-1.0.2-SNAPSHOT
                Uninstalling B-1.0.1-SNAPSHOT
                Uninstalling A-1.0.0
                Installing A-1.2.3-SNAPSHOT
                Installing B-1.0.2
                Installing D-1.0.4-SNAPSHOT
                Updating package G-1.0.1-SNAPSHOT...
                Uninstalling G-1.0.1-SNAPSHOT
                Removed G-1.0.1-SNAPSHOT
                Downloading [G-1.0.1-SNAPSHOT]...
                Added G-1.0.1-SNAPSHOT
                Installing G-1.0.1-SNAPSHOT
                Updating package H-1.0.1-SNAPSHOT...
                Uninstalling H-1.0.1-SNAPSHOT
                Removed H-1.0.1-SNAPSHOT
                Downloading [H-1.0.1-SNAPSHOT]...
                Added H-1.0.1-SNAPSHOT
                Installing H-1.0.1-SNAPSHOT
                Installing hfA-1.0.8
                Installing studioA-1.0.2-SNAPSHOT
                Installing C-1.0.2-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithTargetPlatform() {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available release version on the current target
        // platform, snapshots must be replaced and optional dependencies order must be respected
        assertThat(connectBroker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.0, B-1.0.1, C-1.0.0, D-1.0.3-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.0", "B-1.0.1",
                "C-1.0.0", "D-1.0.3-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """
                Optional dependencies [B:1.0.2] will be ignored for 'A-1.2.0'.

                Dependency resolution:
                  Installation order (7):        A-1.2.0/B-1.0.1/D-1.0.3-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT
                  Unchanged packages (5):        C:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Packages to upgrade (5):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0
                  Local packages to install (5): A:1.2.0, B:1.0.1, hfA:1.0.8, D:1.0.3-SNAPSHOT, studioA:1.0.2-SNAPSHOT

                Uninstalling studioA-1.0.0
                Uninstalling hfA-1.0.0
                Uninstalling D-1.0.2-SNAPSHOT
                Uninstalling B-1.0.1-SNAPSHOT
                Uninstalling A-1.0.0
                Installing A-1.2.0
                Installing B-1.0.1
                Installing D-1.0.3-SNAPSHOT
                Updating package G-1.0.1-SNAPSHOT...
                Uninstalling G-1.0.1-SNAPSHOT
                Removed G-1.0.1-SNAPSHOT
                Downloading [G-1.0.1-SNAPSHOT]...
                Added G-1.0.1-SNAPSHOT
                Installing G-1.0.1-SNAPSHOT
                Updating package H-1.0.1-SNAPSHOT...
                Uninstalling H-1.0.1-SNAPSHOT
                Removed H-1.0.1-SNAPSHOT
                Downloading [H-1.0.1-SNAPSHOT]...
                Added H-1.0.1-SNAPSHOT
                Installing H-1.0.1-SNAPSHOT
                Installing hfA-1.0.8
                Installing studioA-1.0.2-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithTargetPlatformAndSnapshot() {
        connectBroker.setAllowSNAPSHOT(true);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available snapshot version on the current target
        // platform, snapshots must be replaced and optional dependencies order must be respected
        assertThat(connectBroker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.1-SNAPSHOT, B-1.0.1, C-1.0.1-SNAPSHOT, D-1.0.3-SNAPSHOT]
        checkPackagesState(
                connectBroker, List.of("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.1-SNAPSHOT", "B-1.0.1",
                        "C-1.0.1-SNAPSHOT", "D-1.0.3-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.0", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT", "B-1.0.2",
                        "C-1.0.0", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """

                Dependency resolution:
                  Installation order (8):        A-1.2.1-SNAPSHOT/B-1.0.1/D-1.0.3-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT/C-1.0.1-SNAPSHOT
                  Unchanged packages (4):        G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Packages to upgrade (6):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0
                  Local packages to install (6): A:1.2.1-SNAPSHOT, B:1.0.1, hfA:1.0.8, C:1.0.1-SNAPSHOT, D:1.0.3-SNAPSHOT, studioA:1.0.2-SNAPSHOT

                Uninstalling C-1.0.0
                Uninstalling studioA-1.0.0
                Uninstalling hfA-1.0.0
                Uninstalling D-1.0.2-SNAPSHOT
                Uninstalling B-1.0.1-SNAPSHOT
                Uninstalling A-1.0.0
                Installing A-1.2.1-SNAPSHOT
                Installing B-1.0.1
                Installing D-1.0.3-SNAPSHOT
                Updating package G-1.0.1-SNAPSHOT...
                Uninstalling G-1.0.1-SNAPSHOT
                Removed G-1.0.1-SNAPSHOT
                Downloading [G-1.0.1-SNAPSHOT]...
                Added G-1.0.1-SNAPSHOT
                Installing G-1.0.1-SNAPSHOT
                Updating package H-1.0.1-SNAPSHOT...
                Uninstalling H-1.0.1-SNAPSHOT
                Removed H-1.0.1-SNAPSHOT
                Downloading [H-1.0.1-SNAPSHOT]...
                Added H-1.0.1-SNAPSHOT
                Installing H-1.0.1-SNAPSHOT
                Installing hfA-1.0.8
                Installing studioA-1.0.2-SNAPSHOT
                Installing C-1.0.1-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testHotfixPackageRequest() throws Exception {
        connectBroker.setAllowSNAPSHOT(false);
        // Make sure we are registered
        LogicalInstanceIdentifier CLID = new LogicalInstanceIdentifier("toto--titi", "myInstance");
        CLID.save();

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertThat(connectBroker.pkgHotfix()).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.8, hfB-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.8", "hfB-1.0.0", "A-1.0.0",
                "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """

                Dependency resolution:
                  Installation order (2):        hfA-1.0.8/hfB-1.0.0
                  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Packages to upgrade (1):       hfA:1.0.0
                  Local packages to install (2): hfA:1.0.8, hfB:1.0.0

                Uninstalling hfA-1.0.0
                Installing hfA-1.0.8
                Installing hfB-1.0.0""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        connectBroker.setAllowSNAPSHOT(true);
        assertThat(connectBroker.pkgHotfix()).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.8, hfB-1.0.0, hfC-1.0.0-SNAPSHOT, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0,
        // D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.0", "A-1.2.0", "A-1.2.1-SNAPSHOT",
                        "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT",
                        "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = """
                The following SNAPSHOT package(s) will be replaced in local cache (if available): [hfC-1.0.0-SNAPSHOT]
                Download of 'hfC-1.0.0-SNAPSHOT' will replace the one already in local cache.
                Downloading [hfC-1.0.0-SNAPSHOT]...
                Replacement of hfC-1.0.0-SNAPSHOT in local cache...
                Added hfC-1.0.0-SNAPSHOT

                Dependency resolution:
                  Installation order (1):        hfC-1.0.0-SNAPSHOT
                  Unchanged packages (11):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.8, C:1.0.0, D:1.0.2-SNAPSHOT, hfB:1.0.0, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Local packages to install (1): hfC:1.0.0-SNAPSHOT

                Installing hfC-1.0.0-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testReInstallPackageRequestWithOptionalDependencies() {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // G-1.0.1-SNAPSHOT and H-1.0.1-SNAPSHOT are snapshots and must be replaced
        // J-1.0.1 has optional dependencies on G and H and must be reinstalled in last position
        assertThat(connectBroker.pkgRequest(null, List.of("H", "G"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """
                The following SNAPSHOT package(s) will be replaced in local cache (if available): [G-1.0.1-SNAPSHOT, H-1.0.1-SNAPSHOT]
                Uninstalling G-1.0.1-SNAPSHOT
                Uninstalling H-1.0.1-SNAPSHOT
                Download of 'G-1.0.1-SNAPSHOT' will replace the one already in local cache.
                Downloading [G-1.0.1-SNAPSHOT]...
                Replacement of G-1.0.1-SNAPSHOT in local cache...
                Added G-1.0.1-SNAPSHOT
                Download of 'H-1.0.1-SNAPSHOT' will replace the one already in local cache.
                Downloading [H-1.0.1-SNAPSHOT]...
                Replacement of H-1.0.1-SNAPSHOT in local cache...
                Added H-1.0.1-SNAPSHOT

                As package 'J-1.0.1' has an optional dependency on package(s) [G-1.0.1-SNAPSHOT, H-1.0.1-SNAPSHOT] currently being installed, it will be reinstalled.
                Dependency resolution:
                  Installation order (3):        G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/J-1.0.1
                  Uninstallation order (1):      J-1.0.1
                  Unchanged packages (7):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, K:1.0.0-SNAPSHOT
                  Local packages to install (3): G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1
                  Local packages to remove (1):  J:1.0.1

                Uninstalling J-1.0.1
                Installing G-1.0.1-SNAPSHOT
                Installing H-1.0.1-SNAPSHOT
                Installing J-1.0.1""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUninstallLocalPackageRequest() {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(
                connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0",
                        "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1", "K-1.0.0-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // K-1.0.0-SNAPSHOT must be uninstalled even if not available remotely
        assertThat(connectBroker.pkgRequest(null, null, singletonList("K"), null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT",
                        "K-1.0.0-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """

                Dependency resolution:
                  Uninstallation order (1):      K-1.0.0-SNAPSHOT
                  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1
                  Local packages to remove (1):  K:1.0.0-SNAPSHOT

                Uninstalling K-1.0.0-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUninstallPackageRequestWithOptionalDependencies() {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // J-1.0.1 has optional dependencies on G and H and must be reinstalled after uninstalling G and H
        assertThat(connectBroker.pkgRequest(null, null, List.of("H", "G"), null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "J-1.0.1"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT",
                        "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """

                As package 'J-1.0.1' has an optional dependency on package(s) [G-1.0.1-SNAPSHOT, H-1.0.1-SNAPSHOT] currently being uninstalled, it will be reinstalled.
                Dependency resolution:
                  Installation order (1):        J-1.0.1
                  Uninstallation order (3):      J-1.0.1/H-1.0.1-SNAPSHOT/G-1.0.1-SNAPSHOT
                  Unchanged packages (7):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, K:1.0.0-SNAPSHOT
                  Local packages to install (1): J:1.0.1
                  Local packages to remove (3):  G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1

                Uninstalling J-1.0.1
                Uninstalling H-1.0.1-SNAPSHOT
                Uninstalling G-1.0.1-SNAPSHOT
                Installing J-1.0.1""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testInstallPackageRequestWithCustomTargetPlatforms() throws Exception {
        Environment environment = Environment.getDefault();
        environment.setProperty(Environment.DISTRIBUTION_NAME, "server");
        environment.setProperty(Environment.DISTRIBUTION_VERSION, "10.3-I20181011_1121");
        connectBroker = new ConnectBroker(environment);
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // M-1.0.0-I20181011_1121 should be installed correctly
        assertThat(connectBroker.pkgRequest(null, singletonList("M"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT,
        // M-1.0.0-I20181011_1121]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "M-1.0.0-I20181011_1121"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = """

                Dependency resolution:
                  Installation order (1):        M-1.0.0-I20181011_1121
                  Unchanged packages (10):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT
                  Packages to download (1):      M:1.0.0-I20181011_1121

                Package 'M-1.0.0-I20181011_1121' is already in local cache.
                Installing M-1.0.0-I20181011_1121""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        environment.setProperty(Environment.DISTRIBUTION_NAME, "server");
        environment.setProperty(Environment.DISTRIBUTION_VERSION, "9.10-HF08-SNAPSHOT");
        connectBroker = new ConnectBroker(environment);
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBroker.setAllowSNAPSHOT(true);

        // N-1.0.1-HF08 should be installed correctly
        assertThat(connectBroker.pkgRequest(null, singletonList("N"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT,
        // M-1.0.0-I20181011_1121, N-1.0.1-HF08-SNAPSHOT]
        checkPackagesState(connectBroker, List.of("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "M-1.0.0-I20181011_1121", "N-1.0.1-HF08-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                List.of("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = """

                Dependency resolution:
                  Installation order (1):        N-1.0.1-HF08-SNAPSHOT
                  Unchanged packages (11):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT, M:1.0.0-I20181011_1121
                  Local packages to install (1): N:1.0.1-HF08-SNAPSHOT

                Download of 'N-1.0.1-HF08-SNAPSHOT' will replace the one already in local cache.
                Downloading [N-1.0.1-HF08-SNAPSHOT]...
                Replacement of N-1.0.1-HF08-SNAPSHOT in local cache...
                Added N-1.0.1-HF08-SNAPSHOT
                Installing N-1.0.1-HF08-SNAPSHOT""";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    protected void checkPackagesState(PackageState expectedState, String... packageIds) {
        checkPackagesState(connectBroker, List.of(packageIds), expectedState);
    }

    private void checkPackagesState(ConnectBroker connectBrocker, List<String> packageIdList,
            PackageState expectedState) {
        Map<String, PackageState> states = connectBrocker.getUpdateService().getPersistence().getStates();
        for (String pkgId : packageIdList) {
            assertThat(states.get(pkgId)).as("Checking state of %s", pkgId).isEqualTo(expectedState);
        }
    }

    protected static String logOf(LogCaptureFeature.Result logCaptureResult) {
        return String.join("\n", logCaptureResult.getCaughtEventMessages());
    }

}
