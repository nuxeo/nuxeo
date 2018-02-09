/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.connector.http.ConnectUrlConfig;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.launcher.config.TomcatConfigurator;
import org.nuxeo.launcher.connect.fake.LocalConnectFakeConnector;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.JettyFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

/**
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features({ LogCaptureFeature.class, JettyFeature.class })
@Jetty(port = ConnectUrlConfig.CONNECT_TEST_MODE_PORT)
public class TestConnectBroker {

    final String TEST_STORE_PATH = "src/test/resources/packages/store";

    final String TEST_LOCAL_ONLY_PATH = TEST_STORE_PATH + "/local-only";

    final File testStore = new File(TEST_STORE_PATH);

    final File nuxeoHome = new File("target/launcher");

    protected ConnectBroker connectBroker;

    @Inject
    Server server;

    @Inject
    LogCaptureFeature.Result logCaptureResult;

    public static class PkgRequestLogFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LoggingEvent event) {
            return event.getLevel().isGreaterOrEqual(Level.INFO) && (event.getLoggerName().contains("ConnectBroker")
                    || event.getLoggerName().contains("PackagePersistence")
                    || event.getLoggerName().contains("PackageManagerImpl")
                    || event.getLoggerName().contains("MessageInfo")
                    || event.getLoggerName().contains("LocalDownloadingPackage"));
        }
    }

    public class FakeConnectDownloadHandler extends AbstractHandler {

        @Override
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
                throws IOException, ServletException {
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
        NuxeoConnectClient.getConnectGatewayComponent().setTestConnector(
                new LocalConnectFakeConnector(addonJSON, hotfixJSON, studioJSON));

        // build env
        Environment.setDefault(null);
        FileUtils.deleteQuietly(nuxeoHome);
        nuxeoHome.mkdirs();
        System.setProperty(Environment.NUXEO_HOME, nuxeoHome.getPath());
        System.setProperty(TomcatConfigurator.TOMCAT_HOME, Environment.getDefault().getServerHome().getPath());

        // build test packages store
        buildInitialPackageStore();

        // add fake connect request handler for package downloads
        server.setHandler(new FakeConnectDownloadHandler());

        Environment environment = Environment.getDefault();
        environment.setProperty(Environment.DISTRIBUTION_NAME, "server");
        environment.setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        connectBroker = new ConnectBroker(environment);
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
    }

    private void copyPackageToStore(File nuxeoStrore, File uninstallFile, File pkgZip) {
        try {
            File pkgDir = new File(nuxeoStrore, pkgZip.getName().replace(".zip", ""));
            ZipUtils.unzip(pkgZip, pkgDir);
            FileUtils.copyFileToDirectory(uninstallFile, pkgDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildInitialPackageStore() throws IOException {
        File nuxeoPackages = new File(nuxeoHome, "packages");
        File nuxeoStrore = new File(nuxeoPackages, "store");
        File uninstallFile = new File(testStore, "uninstall.xml");
        // Copy all zip from testStore
        FileUtils.iterateFiles(testStore, new String[] { "zip" }, false).forEachRemaining(
                pkgZip -> copyPackageToStore(nuxeoStrore, uninstallFile, pkgZip));
        // Copy only installed packages from testStore/local-only
        copyPackageToStore(nuxeoStrore, uninstallFile, new File(TEST_LOCAL_ONLY_PATH, "K-1.0.0-SNAPSHOT.zip"));

        FileUtils.copyFileToDirectory(new File(testStore, ".packages"), nuxeoPackages);
    }

    @After
    public void afterEach() {
        // clear system properties
        System.clearProperty(Environment.NUXEO_HOME);
        System.clearProperty(TomcatConfigurator.TOMCAT_HOME);
    }

    @Test
    public void testIsRemotePackageId() throws Exception {
        Set<String> remotePackageIds = collectIdsFrom("addon_remote.json", "hotfix_remote.json", "studio_remote.json");
        remotePackageIds.forEach(id -> assertThat(connectBroker.isRemotePackageId(id)).isTrue());

        // Packages in path "$TEST_STORE_PATH/local-only" are not available in remote (local-only)
        assertThat(connectBroker.isRemotePackageId("E-1.0.1")).isFalse();
        assertThat(connectBroker.isRemotePackageId("F.1.0.0-SNAPSHOT")).isFalse();
        assertThat(connectBroker.isRemotePackageId("K.1.0.0-SNAPSHOT")).isFalse();
        assertThat(connectBroker.isRemotePackageId("unknown-package")).isFalse();
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testDownloadUnknownPackage() throws Exception {
        // GIVEN a non existing package
        checkPackagesState(null, "unknown-package");

        // WHEN trying to download it
        boolean isSuccessful = connectBroker.downloadPackages(Arrays.asList("unknown-package"));
        assertThat(isSuccessful).isFalse();

        // THEN it fails and the package is still unknown
        checkPackagesState(null, "unknown-package");
        connectBroker.getCommandSet().log();
        String expectedLogs = "Downloading [unknown-package]...\n" //
                + "\tDownload failed (not found)."; //
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testInstallPackageRequest() throws Exception {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // B-1.0.2 is not available on platform version server-8.3
        assertThat(
                connectBroker.pkgRequest(null, Arrays.asList("A-1.2.0", "B-1.0.2"), null, null, true, false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
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
        assertThat(
                connectBroker.pkgRequest(null, Arrays.asList("A-1.2.0", "B-1.0.2"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.0, B-1.0.2, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.2.0", "B-1.0.2", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT",
                        "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = "Relax restriction to target platform server-8.3 because of package(s) B-1.0.2\n" //
                + "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (2):        B-1.0.2/A-1.2.0\n" //
                + "  Unchanged packages (8):        hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Packages to upgrade (2):       A:1.0.0, B:1.0.1-SNAPSHOT\n" //
                + "  Local packages to install (2): A:1.2.0, B:1.0.2\n" //
                + "\n" //
                + "Uninstalling B-1.0.1-SNAPSHOT\n" //
                + "Uninstalling A-1.0.0\n" //
                + "Installing B-1.0.2\n" //
                + "Installing A-1.2.0";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // SNAPSHOTS must be replaced in local cache before installation and D-1.0.4-SNAPSHOT must be installed after
        // C-1.0.2-SNAPSHOT because of optional dependencies
        assertThat(connectBroker.pkgRequest(null,
                Arrays.asList("A-1.2.2-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"), null, null, true,
                false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.2-SNAPSHOT, B-1.0.2, C-1.0.2-SNAPSHOT, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.2.2-SNAPSHOT", "B-1.0.2",
                "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.0", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT", "B-1.0.1",
                        "C-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = "The following SNAPSHOT package(s) will be replaced in local cache (if available): [A-1.2.2-SNAPSHOT, C-1.0.2-SNAPSHOT, D-1.0.4-SNAPSHOT]\n" //
                + "Download of 'A-1.2.2-SNAPSHOT' will replace the one already in local cache.\n" //
                + "Downloading [A-1.2.2-SNAPSHOT]...\n" //
                + "Replacement of A-1.2.2-SNAPSHOT in local cache...\n" //
                + "Added A-1.2.2-SNAPSHOT\n" //
                + "Download of 'C-1.0.2-SNAPSHOT' will replace the one already in local cache.\n" //
                + "Downloading [C-1.0.2-SNAPSHOT]...\n" //
                + "Replacement of C-1.0.2-SNAPSHOT in local cache...\n" //
                + "Added C-1.0.2-SNAPSHOT\n" //
                + "Download of 'D-1.0.4-SNAPSHOT' will replace the one already in local cache.\n" //
                + "Downloading [D-1.0.4-SNAPSHOT]...\n" //
                + "Replacement of D-1.0.4-SNAPSHOT in local cache...\n" //
                + "Added D-1.0.4-SNAPSHOT\n" //
                + "Relax restriction to target platform server-8.3 because of package(s) A-1.2.2-SNAPSHOT, C-1.0.2-SNAPSHOT, D-1.0.4-SNAPSHOT\n" //
                + "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (3):        A-1.2.2-SNAPSHOT/D-1.0.4-SNAPSHOT/C-1.0.2-SNAPSHOT\n" //
                + "  Unchanged packages (7):        B:1.0.2, hfA:1.0.0, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Packages to upgrade (3):       A:1.2.0, C:1.0.0, D:1.0.2-SNAPSHOT\n" //
                + "  Local packages to install (3): A:1.2.2-SNAPSHOT, C:1.0.2-SNAPSHOT, D:1.0.4-SNAPSHOT\n" //
                + "\n" //
                + "Uninstalling C-1.0.0\n" //
                + "Uninstalling D-1.0.2-SNAPSHOT\n" //
                + "Uninstalling A-1.2.0\n" //
                + "Installing A-1.2.2-SNAPSHOT\n" //
                + "Installing D-1.0.4-SNAPSHOT\n" //
                + "Installing C-1.0.2-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
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
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // L-1.0.2 is missing hfB not available on platform version server-8.4
        assertThat(connectBroker.pkgRequest(null, Arrays.asList("L-1.0.2"), null, null, true, false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "\nFailed to resolve dependencies: Couldn't order [L-1.0.2] missing [hfB].";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // L-1.0.3 (last version of L) depends on hfD which is missing hfB not available on platform version server-8.4
        assertThat(connectBroker.pkgRequest(null, Arrays.asList("L"), null, null, true, false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = "\nFailed to resolve dependencies: Couldn't order [L-1.0.3, hfD-1.0.0] missing [hfB, hfD].";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testInstallLocalPackageRequest() throws Exception {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // F-1.0.0-SNAPSHOT is not available on platform version server-8.3
        assertThat(connectBroker.pkgRequest(null,
                Arrays.asList(TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip", TEST_LOCAL_ONLY_PATH + "/E-1.0.1"), null,
                null, true, false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT", "E-1.0.1",
                        "F-1.0.0-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check that local files and directory have not been removed
        assertThat(new File(TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip").exists()).isTrue();
        assertThat(new File(TEST_LOCAL_ONLY_PATH + "/E-1.0.1").exists()).isTrue();

        // check logs
        String expectedLogs = "Added " + TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip\n" //
                + "Added " + TEST_LOCAL_ONLY_PATH + "/E-1.0.1\n" //
                + "org.nuxeo.connect.update.PackageException: Package(s) F-1.0.0-SNAPSHOT not available on platform version server-8.3 (relax is not allowed)";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        connectBroker.setRelax("true");
        assertThat(connectBroker.pkgRequest(null,
                Arrays.asList(TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip", TEST_LOCAL_ONLY_PATH + "/E-1.0.1"), null,
                null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.0, B-1.0.2, C-1.0.0, D-1.0.2-SNAPSHOT, E-1.0.1-SNAPSHOT,
        // F-1.0.0-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "E-1.0.1", "F-1.0.0-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = "The following SNAPSHOT package(s) will be replaced in local cache (if available): [src/test/resources/packages/store/local-only/F-1.0.0-SNAPSHOT.zip]\n" //
                + "Replacement of F-1.0.0-SNAPSHOT in local cache...\n" //
                + "Added src/test/resources/packages/store/local-only/F-1.0.0-SNAPSHOT.zip\n" //
                + "Relax restriction to target platform server-8.3 because of package(s) F-1.0.0-SNAPSHOT\n" //
                + "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (2):        E-1.0.1/F-1.0.0-SNAPSHOT\n" //
                + "  Unchanged packages (10):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Local packages to install (2): E:1.0.1, F:1.0.0-SNAPSHOT\n" //
                + "\n" //
                + "Installing E-1.0.1\n" //
                + "Installing F-1.0.0-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testReInstallPackageRequest() throws Exception {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // A-1.0.0 and C-1.0.0 are releases and are already installed
        assertThat(connectBroker.pkgRequest(null, Arrays.asList("A-1.0.0", "C"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "\n" //
                + "Dependency resolution:\n" //
                + "  Unchanged packages (10):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // B-1.0.1-SNAPSHOT and D-1.0.2-SNAPSHOT must be uninstalled then reinstalled as they are SNAPSHOTS
        // C-1.0.0 must be reinstalled as it has an optional dependency on D
        assertThat(connectBroker.pkgRequest(null, Arrays.asList("B-1.0.1-SNAPSHOT", "D"), null, null, true,
                false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = "The following SNAPSHOT package(s) will be replaced in local cache (if available): [D-1.0.2-SNAPSHOT, B-1.0.1-SNAPSHOT]\n" //
                + "Uninstalling D-1.0.2-SNAPSHOT\n" //
                + "Uninstalling B-1.0.1-SNAPSHOT\n" //
                + "Download of 'D-1.0.2-SNAPSHOT' will replace the one already in local cache.\n" //
                + "Downloading [D-1.0.2-SNAPSHOT]...\n" //
                + "Replacement of D-1.0.2-SNAPSHOT in local cache...\n" //
                + "Added D-1.0.2-SNAPSHOT\n" //
                + "Download of 'B-1.0.1-SNAPSHOT' will replace the one already in local cache.\n" //
                + "Downloading [B-1.0.1-SNAPSHOT]...\n" //
                + "Replacement of B-1.0.1-SNAPSHOT in local cache...\n" //
                + "Added B-1.0.1-SNAPSHOT\n" //
                + "\n"
                + "As package 'C-1.0.0' has an optional dependency on package(s) [D-1.0.2-SNAPSHOT] currently being installed, it will be reinstalled.\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (3):        B-1.0.1-SNAPSHOT/D-1.0.2-SNAPSHOT/C-1.0.0\n" //
                + "  Uninstallation order (1):      C-1.0.0\n" //
                + "  Unchanged packages (7):        A:1.0.0, hfA:1.0.0, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Local packages to install (3): B:1.0.1-SNAPSHOT, C:1.0.0, D:1.0.2-SNAPSHOT\n" //
                + "  Local packages to remove (1):  C:1.0.0\n" //
                + "\n" //
                + "Uninstalling C-1.0.0\n" //
                + "Installing B-1.0.1-SNAPSHOT\n" //
                + "Installing D-1.0.2-SNAPSHOT\n" //
                + "Installing C-1.0.0";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testReInstallLocalPackageRequest() throws Exception {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // A-1.0.0 is a release and is already installed
        assertThat(connectBroker.pkgRequest(null, Arrays.asList(TEST_STORE_PATH + "/A-1.0.0.zip"), null, null, true,
                false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "\n" //
                + "Dependency resolution:\n" //
                + "  Unchanged packages (10):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // B-1.0.1-SNAPSHOT must be uninstalled then reinstalled as it is a SNAPSHOT
        // it must be replaced in local cache because a new file is provided
        assertThat(connectBroker.pkgRequest(null, Arrays.asList(TEST_STORE_PATH + "/B-1.0.1-SNAPSHOT.zip"), null, null,
                true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = "The following SNAPSHOT package(s) will be replaced in local cache (if available): [src/test/resources/packages/store/B-1.0.1-SNAPSHOT.zip]\n" //
                + "Uninstalling B-1.0.1-SNAPSHOT\n" //
                + "Replacement of B-1.0.1-SNAPSHOT in local cache...\n" //
                + "Added src/test/resources/packages/store/B-1.0.1-SNAPSHOT.zip\n" //
                + "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (1):        B-1.0.1-SNAPSHOT\n" //
                + "  Unchanged packages (9):        A:1.0.0, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Local packages to install (1): B:1.0.1-SNAPSHOT\n" //
                + "\n" //
                + "Installing B-1.0.1-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // K-1.0.0-SNAPSHOT must be uninstalled then reinstalled as it is a SNAPSHOT, even if not available remotely
        // it must be replaced in local cache because a new file is provided
        assertThat(connectBroker.pkgRequest(null, Arrays.asList(TEST_LOCAL_ONLY_PATH + "/K-1.0.0-SNAPSHOT.zip"), null,
                null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, K-1.0.0-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "K-1.0.0-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = "The following SNAPSHOT package(s) will be replaced in local cache (if available): [src/test/resources/packages/store/local-only/K-1.0.0-SNAPSHOT.zip]\n" //
                + "Uninstalling K-1.0.0-SNAPSHOT\n" //
                + "Replacement of K-1.0.0-SNAPSHOT in local cache...\n" //
                + "Added src/test/resources/packages/store/local-only/K-1.0.0-SNAPSHOT.zip\n" //
                + "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (1):        K-1.0.0-SNAPSHOT\n" //
                + "  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n" //
                + "  Local packages to install (1): K:1.0.0-SNAPSHOT\n" //
                + "\n" //
                + "Installing K-1.0.0-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        // K-1.0.0-SNAPSHOT must be uninstalled then reinstalled as it is a SNAPSHOT, even if not available remotely
        // it must not be replaced in local cache because no new file is provided
        assertThat(connectBroker.pkgRequest(null, Arrays.asList("K"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, K-1.0.0-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "K-1.0.0-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = "The following SNAPSHOT package(s) will be replaced in local cache (if available): [K-1.0.0-SNAPSHOT]\n" //
                + "Uninstalling K-1.0.0-SNAPSHOT\n" //
                + "The SNAPSHOT package K-1.0.0-SNAPSHOT is not available remotely, local cache will be used.\n" //
                + "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (1):        K-1.0.0-SNAPSHOT\n" //
                + "  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n" //
                + "  Local packages to install (1): K:1.0.0-SNAPSHOT\n" //
                + "\n" //
                + "Installing K-1.0.0-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithRelax() throws Exception {
        Environment.getDefault().getProperties().remove(Environment.DISTRIBUTION_NAME);
        Environment.getDefault().getProperties().remove(Environment.DISTRIBUTION_VERSION);
        connectBroker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBroker.setAllowSNAPSHOT(false);
        connectBroker.setRelax("true");

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available release version, snapshots must be replaced
        // and optional dependencies order must be respected
        assertThat(connectBroker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.2, B-1.0.2, C-1.0.0, D-1.0.4-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.2", "B-1.0.2",
                "C-1.0.0", "D-1.0.4-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.0", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT",
                        "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "Relax restriction to target platform null-null because of package(s) studioA, hfA, A, B, C, D, G, H, J, K\n" //
                + "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (7):        A-1.2.2/B-1.0.2/D-1.0.4-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT\n" //
                + "  Unchanged packages (5):        C:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Packages to upgrade (5):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n" //
                + "  Local packages to install (5): A:1.2.2, B:1.0.2, hfA:1.0.8, D:1.0.4-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n" //
                + "\n" //
                + "Uninstalling studioA-1.0.0\n" //
                + "Uninstalling hfA-1.0.0\n" //
                + "Uninstalling D-1.0.2-SNAPSHOT\n" //
                + "Uninstalling B-1.0.1-SNAPSHOT\n" //
                + "Uninstalling A-1.0.0\n" //
                + "Installing A-1.2.2\n" //
                + "Installing B-1.0.2\n" //
                + "Installing D-1.0.4-SNAPSHOT\n" //
                + "Updating package G-1.0.1-SNAPSHOT...\n" //
                + "Uninstalling G-1.0.1-SNAPSHOT\n" //
                + "Removed G-1.0.1-SNAPSHOT\n" //
                + "Downloading [G-1.0.1-SNAPSHOT]...\n" //
                + "Added G-1.0.1-SNAPSHOT\n" //
                + "Installing G-1.0.1-SNAPSHOT\n" //
                + "Updating package H-1.0.1-SNAPSHOT...\n" //
                + "Uninstalling H-1.0.1-SNAPSHOT\n" //
                + "Removed H-1.0.1-SNAPSHOT\n" //
                + "Downloading [H-1.0.1-SNAPSHOT]...\n" //
                + "Added H-1.0.1-SNAPSHOT\n" //
                + "Installing H-1.0.1-SNAPSHOT\n" //
                + "Installing hfA-1.0.8\n" //
                + "Installing studioA-1.0.2-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithRelaxAndSnapshot() throws Exception {
        Environment.getDefault().getProperties().remove(Environment.DISTRIBUTION_NAME);
        Environment.getDefault().getProperties().remove(Environment.DISTRIBUTION_VERSION);
        connectBroker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBroker.setAllowSNAPSHOT(true);
        connectBroker.setRelax("true");

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available snapshot version, snapshots must be replaced
        // and optional dependencies order must be respected
        assertThat(connectBroker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.3-SNAPSHOT, B-1.0.2, C-1.0.2-SNAPSHOT, D-1.0.4-SNAPSHOT]
        checkPackagesState(
                connectBroker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.3-SNAPSHOT", "B-1.0.2",
                        "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.0", "B-1.0.1-SNAPSHOT",
                        "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "Relax restriction to target platform null-null because of package(s) studioA, hfA, A, B, C, D, G, H, J, K\n" //
                + "\nDependency resolution:\n" //
                + "  Installation order (8):        A-1.2.3-SNAPSHOT/B-1.0.2/D-1.0.4-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT/C-1.0.2-SNAPSHOT\n" //
                + "  Unchanged packages (4):        G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Packages to upgrade (6):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n" //
                + "  Local packages to install (6): A:1.2.3-SNAPSHOT, B:1.0.2, hfA:1.0.8, C:1.0.2-SNAPSHOT, D:1.0.4-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n" //
                + "\n" //
                + "Uninstalling C-1.0.0\n" //
                + "Uninstalling studioA-1.0.0\n" //
                + "Uninstalling hfA-1.0.0\n" //
                + "Uninstalling D-1.0.2-SNAPSHOT\n" //
                + "Uninstalling B-1.0.1-SNAPSHOT\n" //
                + "Uninstalling A-1.0.0\n" //
                + "Installing A-1.2.3-SNAPSHOT\n" //
                + "Installing B-1.0.2\n" //
                + "Installing D-1.0.4-SNAPSHOT\n" //
                + "Updating package G-1.0.1-SNAPSHOT...\n" //
                + "Uninstalling G-1.0.1-SNAPSHOT\n" //
                + "Removed G-1.0.1-SNAPSHOT\n" //
                + "Downloading [G-1.0.1-SNAPSHOT]...\n" //
                + "Added G-1.0.1-SNAPSHOT\n" //
                + "Installing G-1.0.1-SNAPSHOT\n" //
                + "Updating package H-1.0.1-SNAPSHOT...\n" //
                + "Uninstalling H-1.0.1-SNAPSHOT\n" //
                + "Removed H-1.0.1-SNAPSHOT\n" //
                + "Downloading [H-1.0.1-SNAPSHOT]...\n" //
                + "Added H-1.0.1-SNAPSHOT\n" //
                + "Installing H-1.0.1-SNAPSHOT\n" //
                + "Installing hfA-1.0.8\n" //
                + "Installing studioA-1.0.2-SNAPSHOT\n" //
                + "Installing C-1.0.2-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithTargetPlatform() throws Exception {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available release version on the current target
        // platform, snapshots must be replaced and optional dependencies order must be respected
        assertThat(connectBroker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.0, B-1.0.1, C-1.0.0, D-1.0.3-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.0", "B-1.0.1",
                "C-1.0.0", "D-1.0.3-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "Optional dependencies [B:1.0.2] will be ignored for 'A-1.2.0'.\n" //
                + "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (7):        A-1.2.0/B-1.0.1/D-1.0.3-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT\n" //
                + "  Unchanged packages (5):        C:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Packages to upgrade (5):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n" //
                + "  Local packages to install (5): A:1.2.0, B:1.0.1, hfA:1.0.8, D:1.0.3-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n" //
                + "\n" //
                + "Uninstalling studioA-1.0.0\n" //
                + "Uninstalling hfA-1.0.0\n" //
                + "Uninstalling D-1.0.2-SNAPSHOT\n" //
                + "Uninstalling B-1.0.1-SNAPSHOT\n" //
                + "Uninstalling A-1.0.0\n" //
                + "Installing A-1.2.0\n" //
                + "Installing B-1.0.1\n" //
                + "Installing D-1.0.3-SNAPSHOT\n" //
                + "Updating package G-1.0.1-SNAPSHOT...\n" //
                + "Uninstalling G-1.0.1-SNAPSHOT\n" //
                + "Removed G-1.0.1-SNAPSHOT\n" //
                + "Downloading [G-1.0.1-SNAPSHOT]...\n" //
                + "Added G-1.0.1-SNAPSHOT\n" //
                + "Installing G-1.0.1-SNAPSHOT\n" //
                + "Updating package H-1.0.1-SNAPSHOT...\n" //
                + "Uninstalling H-1.0.1-SNAPSHOT\n" //
                + "Removed H-1.0.1-SNAPSHOT\n" //
                + "Downloading [H-1.0.1-SNAPSHOT]...\n" //
                + "Added H-1.0.1-SNAPSHOT\n" //
                + "Installing H-1.0.1-SNAPSHOT\n" //
                + "Installing hfA-1.0.8\n" //
                + "Installing studioA-1.0.2-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithTargetPlatformAndSnapshot() throws Exception {
        connectBroker.setAllowSNAPSHOT(true);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available snapshot version on the current target
        // platform, snapshots must be replaced and optional dependencies order must be respected
        assertThat(connectBroker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.1-SNAPSHOT, B-1.0.1, C-1.0.1-SNAPSHOT, D-1.0.3-SNAPSHOT]
        checkPackagesState(
                connectBroker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.1-SNAPSHOT", "B-1.0.1",
                        "C-1.0.1-SNAPSHOT", "D-1.0.3-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.0", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT",
                        "B-1.0.2", "C-1.0.0", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (8):        A-1.2.1-SNAPSHOT/B-1.0.1/D-1.0.3-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT/C-1.0.1-SNAPSHOT\n" //
                + "  Unchanged packages (4):        G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Packages to upgrade (6):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n" //
                + "  Local packages to install (6): A:1.2.1-SNAPSHOT, B:1.0.1, hfA:1.0.8, C:1.0.1-SNAPSHOT, D:1.0.3-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n" //
                + "\n" //
                + "Uninstalling C-1.0.0\n" //
                + "Uninstalling studioA-1.0.0\n" //
                + "Uninstalling hfA-1.0.0\n" //
                + "Uninstalling D-1.0.2-SNAPSHOT\n" //
                + "Uninstalling B-1.0.1-SNAPSHOT\n" //
                + "Uninstalling A-1.0.0\n" //
                + "Installing A-1.2.1-SNAPSHOT\n" //
                + "Installing B-1.0.1\n" //
                + "Installing D-1.0.3-SNAPSHOT\n" //
                + "Updating package G-1.0.1-SNAPSHOT...\n" //
                + "Uninstalling G-1.0.1-SNAPSHOT\n" //
                + "Removed G-1.0.1-SNAPSHOT\n" //
                + "Downloading [G-1.0.1-SNAPSHOT]...\n" //
                + "Added G-1.0.1-SNAPSHOT\n" //
                + "Installing G-1.0.1-SNAPSHOT\n" //
                + "Updating package H-1.0.1-SNAPSHOT...\n" //
                + "Uninstalling H-1.0.1-SNAPSHOT\n" //
                + "Removed H-1.0.1-SNAPSHOT\n" //
                + "Downloading [H-1.0.1-SNAPSHOT]...\n" //
                + "Added H-1.0.1-SNAPSHOT\n" //
                + "Installing H-1.0.1-SNAPSHOT\n" //
                + "Installing hfA-1.0.8\n" //
                + "Installing studioA-1.0.2-SNAPSHOT\n" //
                + "Installing C-1.0.1-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testHotfixPackageRequest() throws Exception {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertThat(connectBroker.pkgHotfix()).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.8, hfB-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.8", "hfB-1.0.0", "A-1.0.0",
                "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (2):        hfA-1.0.8/hfB-1.0.0\n" //
                + "  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Packages to upgrade (1):       hfA:1.0.0\n" //
                + "  Local packages to install (2): hfA:1.0.8, hfB:1.0.0\n" //
                + "\n" //
                + "Uninstalling hfA-1.0.0\n" //
                + "Installing hfA-1.0.8\n" //
                + "Installing hfB-1.0.0";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
        logCaptureResult.clear();

        connectBroker.setAllowSNAPSHOT(true);
        assertThat(connectBroker.pkgHotfix()).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.8, hfB-1.0.0, hfC-1.0.0-SNAPSHOT, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0,
        // D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.0", "A-1.2.0", "A-1.2.1-SNAPSHOT",
                        "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT",
                        "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = "The following SNAPSHOT package(s) will be replaced in local cache (if available): [hfC-1.0.0-SNAPSHOT]\n" //
                + "Download of 'hfC-1.0.0-SNAPSHOT' will replace the one already in local cache.\n" //
                + "Downloading [hfC-1.0.0-SNAPSHOT]...\n" //
                + "Replacement of hfC-1.0.0-SNAPSHOT in local cache...\n" //
                + "Added hfC-1.0.0-SNAPSHOT\n" //
                + "\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (1):        hfC-1.0.0-SNAPSHOT\n" //
                + "  Unchanged packages (11):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.8, C:1.0.0, D:1.0.2-SNAPSHOT, hfB:1.0.0, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1, K:1.0.0-SNAPSHOT\n" //
                + "  Local packages to install (1): hfC:1.0.0-SNAPSHOT\n" //
                + "\n" //
                + "Installing hfC-1.0.0-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testReInstallPackageRequestWithOptionalDependencies() throws Exception {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // G-1.0.1-SNAPSHOT and H-1.0.1-SNAPSHOT are snapshots and must be replaced
        // J-1.0.1 has optional dependencies on G and H and must be reinstalled in last position
        assertThat(connectBroker.pkgRequest(null, Arrays.asList("H", "G"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "The following SNAPSHOT package(s) will be replaced in local cache (if available): [G-1.0.1-SNAPSHOT, H-1.0.1-SNAPSHOT]\n" //
                + "Uninstalling G-1.0.1-SNAPSHOT\n" //
                + "Uninstalling H-1.0.1-SNAPSHOT\n" //
                + "Download of 'G-1.0.1-SNAPSHOT' will replace the one already in local cache.\n" //
                + "Downloading [G-1.0.1-SNAPSHOT]...\n" //
                + "Replacement of G-1.0.1-SNAPSHOT in local cache...\n" //
                + "Added G-1.0.1-SNAPSHOT\n" //
                + "Download of 'H-1.0.1-SNAPSHOT' will replace the one already in local cache.\n" //
                + "Downloading [H-1.0.1-SNAPSHOT]...\n" //
                + "Replacement of H-1.0.1-SNAPSHOT in local cache...\n" //
                + "Added H-1.0.1-SNAPSHOT\n" //
                + "\n" //
                + "As package 'J-1.0.1' has an optional dependency on package(s) [G-1.0.1-SNAPSHOT, H-1.0.1-SNAPSHOT] currently being installed, it will be reinstalled.\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (3):        G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/J-1.0.1\n" //
                + "  Uninstallation order (1):      J-1.0.1\n" //
                + "  Unchanged packages (7):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, K:1.0.0-SNAPSHOT\n" //
                + "  Local packages to install (3): G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n" //
                + "  Local packages to remove (1):  J:1.0.1\n" //
                + "\n" //
                + "Uninstalling J-1.0.1\n" //
                + "Installing G-1.0.1-SNAPSHOT\n" //
                + "Installing H-1.0.1-SNAPSHOT\n" //
                + "Installing J-1.0.1";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUninstallLocalPackageRequest() throws Exception {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0",
                        "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1", "K-1.0.0-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // K-1.0.0-SNAPSHOT must be uninstalled even if not available remotely
        assertThat(connectBroker.pkgRequest(null, null, Arrays.asList("K"), null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT",
                        "K-1.0.0-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "\n" //
                + "Dependency resolution:\n" //
                + "  Uninstallation order (1):      K-1.0.0-SNAPSHOT\n" //
                + "  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n" //
                + "  Local packages to remove (1):  K:1.0.0-SNAPSHOT\n" //
                + "\n" //
                + "Uninstalling K-1.0.0-SNAPSHOT";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUninstallPackageRequestWithOptionalDependencies() throws Exception {
        connectBroker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // J-1.0.1 has optional dependencies on G and H and must be reinstalled after uninstalling G and H
        assertThat(connectBroker.pkgRequest(null, null, Arrays.asList("H", "G"), null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBroker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "J-1.0.1"), PackageState.STARTED);
        checkPackagesState(connectBroker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT",
                        "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        String expectedLogs = "\n" //
                + "As package 'J-1.0.1' has an optional dependency on package(s) [G-1.0.1-SNAPSHOT, H-1.0.1-SNAPSHOT] currently being uninstalled, it will be reinstalled.\n" //
                + "Dependency resolution:\n" //
                + "  Installation order (1):        J-1.0.1\n" //
                + "  Uninstallation order (3):      J-1.0.1/H-1.0.1-SNAPSHOT/G-1.0.1-SNAPSHOT\n" //
                + "  Unchanged packages (7):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, K:1.0.0-SNAPSHOT\n" //
                + "  Local packages to install (1): J:1.0.1\n" //
                + "  Local packages to remove (3):  G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n" //
                + "\n" //
                + "Uninstalling J-1.0.1\n" //
                + "Uninstalling H-1.0.1-SNAPSHOT\n" //
                + "Uninstalling G-1.0.1-SNAPSHOT\n" //
                + "Installing J-1.0.1";
        assertThat(logOf(logCaptureResult)).isEqualTo(expectedLogs);
    }

    protected void checkPackagesState(PackageState expectedState, String... packageIds) {
        checkPackagesState(connectBroker, Arrays.asList(packageIds), expectedState);
    }

    private void checkPackagesState(ConnectBroker connectBrocker, List<String> packageIdList,
            PackageState expectedState) {
        Map<String, PackageState> states = connectBrocker.getUpdateService().getPersistence().getStates();
        for (String pkgId : packageIdList) {
            assertThat(states.get(pkgId)).as("Checking state of %s", pkgId).isEqualTo(expectedState);
        }
    }

    protected Set<String> collectIdsFrom(String... jsonFileNames) throws JSONException, IOException {
        Set<String> result = new HashSet<>();

        for (String jsonFileName : jsonFileNames) {
            File jsonFile = new File(testStore, jsonFileName);
            JSONArray array = new JSONArray(FileUtils.readFileToString(jsonFile, UTF_8));
            Set<String> ids = new HashSet<>();

            for (int i = 0; i < array.length(); i++) {
                String id = (String) array.getJSONObject(i).get("id");
                ids.add(id);
            }
            assertThat(ids).hasSize(array.length());
            result.addAll(ids);
        }
        return result;
    }

    protected static String logOf(LogCaptureFeature.Result logCaptureResult) {
        return logCaptureResult.getCaughtEvents().stream().map(LoggingEvent::getRenderedMessage).collect(
                Collectors.joining("\n"));
    }

}
