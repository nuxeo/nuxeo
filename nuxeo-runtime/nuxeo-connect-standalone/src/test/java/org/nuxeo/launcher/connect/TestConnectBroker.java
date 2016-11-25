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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
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
        String addonJSON = FileUtils.readFileToString(new File(testStore, "addon_remote.json"));
        String hotfixJSON = FileUtils.readFileToString(new File(testStore, "hotfix_remote.json"));
        String studioJSON = FileUtils.readFileToString(new File(testStore, "studio_remote.json"));
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
    }

    private void buildInitialPackageStore() throws IOException {
        File nuxeoPackages = new File(nuxeoHome, "packages");
        File nuxeoStrore = new File(nuxeoPackages, "store");
        File uninstallFile = new File(testStore, "uninstall.xml");
        FileUtils.iterateFiles(testStore, new String[] { "zip" }, false).forEachRemaining(pkgZip -> {
            try {
                File pkgDir = new File(nuxeoStrore, pkgZip.getName().replace(".zip", ""));
                ZipUtils.unzip(pkgZip, pkgDir);
                FileUtils.copyFileToDirectory(uninstallFile, pkgDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        FileUtils.copyFileToDirectory(new File(testStore, ".packages"), nuxeoPackages);
    }

    @After
    public void afterEach() {
        // clear system properties
        System.clearProperty(Environment.NUXEO_HOME);
        System.clearProperty(TomcatConfigurator.TOMCAT_HOME);
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testInstallPackageRequest() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // B-1.0.2 is not available on platform version server-8.3
        assertThat(connectBrocker.pkgRequest(null, Arrays.asList("A-1.2.0", "B-1.0.2"), null, null, true,
                false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<String> expectedLogs = Arrays.asList(
                "org.nuxeo.connect.update.PackageException: Package(s) B-1.0.2 not available on platform version server-8.3 (relax is not allowed)");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
        logCaptureResult.clear();

        connectBrocker.setRelax("true");
        // restriction on target platform must be ignored and B-1.0.2 must be installed before A-1.2.0 because of
        // optional dependencies
        assertThat(
                connectBrocker.pkgRequest(null, Arrays.asList("A-1.2.0", "B-1.0.2"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.0, B-1.0.2, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.2.0", "B-1.0.2", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT",
                        "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = Arrays.asList("Relax restriction to target platform server-8.3 because of package(s) B-1.0.2",
                "\nDependency resolution:\n" + "  Installation order (2):        B-1.0.2/A-1.2.0\n"
                        + "  Unchanged packages (7):        hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Packages to upgrade (2):       A:1.0.0, B:1.0.1-SNAPSHOT\n"
                        + "  Local packages to install (2): A:1.2.0, B:1.0.2\n",
                "Uninstalling B-1.0.1-SNAPSHOT", "Uninstalling A-1.0.0", "Installing B-1.0.2", "Installing A-1.2.0");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
        logCaptureResult.clear();

        // SNAPSHOTS must be replaced in local cache before installation and D-1.0.4-SNAPSHOT must be installed after
        // C-1.0.2-SNAPSHOT because of optional dependencies
        assertThat(connectBrocker.pkgRequest(null,
                Arrays.asList("A-1.2.2-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"), null, null, true,
                false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.2-SNAPSHOT, B-1.0.2, C-1.0.2-SNAPSHOT, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.2.2-SNAPSHOT", "B-1.0.2",
                "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.0", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT", "B-1.0.1",
                        "C-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = Arrays.asList(
                "The following SNAPSHOT package(s) will be replaced in local cache : [A-1.2.2-SNAPSHOT, C-1.0.2-SNAPSHOT, D-1.0.4-SNAPSHOT]",
                "Download of 'A-1.2.2-SNAPSHOT' will replace the one already in local cache.",
                "Downloading [A-1.2.2-SNAPSHOT]...", "Replacement of A-1.2.2-SNAPSHOT in local cache...",
                "Added A-1.2.2-SNAPSHOT", "Download of 'C-1.0.2-SNAPSHOT' will replace the one already in local cache.",
                "Downloading [C-1.0.2-SNAPSHOT]...", "Replacement of C-1.0.2-SNAPSHOT in local cache...",
                "Added C-1.0.2-SNAPSHOT", "Download of 'D-1.0.4-SNAPSHOT' will replace the one already in local cache.",
                "Downloading [D-1.0.4-SNAPSHOT]...", "Replacement of D-1.0.4-SNAPSHOT in local cache...",
                "Added D-1.0.4-SNAPSHOT",
                "Relax restriction to target platform server-8.3 because of package(s) A-1.2.2-SNAPSHOT, C-1.0.2-SNAPSHOT, D-1.0.4-SNAPSHOT",
                "\nDependency resolution:\n"
                        + "  Installation order (3):        A-1.2.2-SNAPSHOT/D-1.0.4-SNAPSHOT/C-1.0.2-SNAPSHOT\n"
                        + "  Unchanged packages (6):        B:1.0.2, hfA:1.0.0, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Packages to upgrade (3):       A:1.2.0, C:1.0.0, D:1.0.2-SNAPSHOT\n"
                        + "  Local packages to install (3): A:1.2.2-SNAPSHOT, C:1.0.2-SNAPSHOT, D:1.0.4-SNAPSHOT\n",
                "Uninstalling C-1.0.0", "Uninstalling D-1.0.2-SNAPSHOT", "Uninstalling A-1.2.0",
                "Installing A-1.2.2-SNAPSHOT", "Installing D-1.0.4-SNAPSHOT", "Installing C-1.0.2-SNAPSHOT");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testInstallLocalPackageRequest() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // F-1.0.0-SNAPSHOT is not available on platform version server-8.3
        assertThat(connectBrocker.pkgRequest(null,
                Arrays.asList(TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip", TEST_LOCAL_ONLY_PATH + "/E-1.0.1"), null,
                null, true, false)).isFalse();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT", "E-1.0.1",
                        "F-1.0.0-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check that local files and directory have not been removed
        assertThat(new File(TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip").exists()).isTrue();
        assertThat(new File(TEST_LOCAL_ONLY_PATH + "/E-1.0.1").exists()).isTrue();

        // check logs
        List<String> expectedLogs = Arrays.asList("Added " + TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip",
                "Added " + TEST_LOCAL_ONLY_PATH + "/E-1.0.1",
                "org.nuxeo.connect.update.PackageException: Package(s) F-1.0.0-SNAPSHOT not available on platform version server-8.3 (relax is not allowed)");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
        logCaptureResult.clear();

        connectBrocker.setRelax("true");
        assertThat(connectBrocker.pkgRequest(null,
                Arrays.asList(TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip", TEST_LOCAL_ONLY_PATH + "/E-1.0.1"), null,
                null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.0, B-1.0.2, C-1.0.0, D-1.0.2-SNAPSHOT, E-1.0.1-SNAPSHOT,
        // F-1.0.0-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "E-1.0.1", "F-1.0.0-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = Arrays.asList(
                "The following SNAPSHOT package(s) will be replaced in local cache : [src/test/resources/packages/store/local-only/F-1.0.0-SNAPSHOT.zip]",
                "Replacement of F-1.0.0-SNAPSHOT in local cache...",
                "Added " + TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip",
                "Relax restriction to target platform server-8.3 because of package(s) F-1.0.0-SNAPSHOT",
                "\nDependency resolution:\n" + "  Installation order (2):        E-1.0.1/F-1.0.0-SNAPSHOT\n"
                        + "  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Local packages to install (2): E:1.0.1, F:1.0.0-SNAPSHOT\n",
                "Installing E-1.0.1", "Installing F-1.0.0-SNAPSHOT");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testReInstallPackageRequest() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // A-1.0.0 and C-1.0.0 are releases and are already installed
        assertThat(connectBrocker.pkgRequest(null, Arrays.asList("A-1.0.0", "C"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<String> expectedLogs = Arrays.asList("\nDependency resolution:\n"
                + "  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
        logCaptureResult.clear();

        // B-1.0.1-SNAPSHOT and D-1.0.2-SNAPSHOT must be uninstalled then reinstalled as they are SNAPSHOTS
        // C-1.0.0 must be reinstalled as it has an optional dependency on D
        assertThat(connectBrocker.pkgRequest(null, Arrays.asList("B-1.0.1-SNAPSHOT", "D"), null, null, true,
                false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = Arrays.asList(
                "The following SNAPSHOT package(s) will be replaced in local cache : [D-1.0.2-SNAPSHOT, B-1.0.1-SNAPSHOT]",
                "Uninstalling D-1.0.2-SNAPSHOT", "Uninstalling B-1.0.1-SNAPSHOT",
                "Download of 'D-1.0.2-SNAPSHOT' will replace the one already in local cache.",
                "Downloading [D-1.0.2-SNAPSHOT]...", "Replacement of D-1.0.2-SNAPSHOT in local cache...",
                "Added D-1.0.2-SNAPSHOT", "Download of 'B-1.0.1-SNAPSHOT' will replace the one already in local cache.",
                "Downloading [B-1.0.1-SNAPSHOT]...", "Replacement of B-1.0.1-SNAPSHOT in local cache...",
                "Added B-1.0.1-SNAPSHOT",
                "\nAs package 'C-1.0.0' has an optional dependency on package(s) [D-1.0.2-SNAPSHOT] currently being installed, it will be reinstalled."
                        + "\nDependency resolution:\n"
                        + "  Installation order (3):        B-1.0.1-SNAPSHOT/D-1.0.2-SNAPSHOT/C-1.0.0\n"
                        + "  Uninstallation order (1):      C-1.0.0\n"
                        + "  Unchanged packages (6):        A:1.0.0, hfA:1.0.0, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Local packages to install (3): B:1.0.1-SNAPSHOT, C:1.0.0, D:1.0.2-SNAPSHOT\n"
                        + "  Local packages to remove (1):  C:1.0.0\n",
                "Uninstalling C-1.0.0", "Installing B-1.0.1-SNAPSHOT", "Installing D-1.0.2-SNAPSHOT",
                "Installing C-1.0.0");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testReInstallLocalPackageRequest() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // A-1.0.0 is a release and is already installed
        assertThat(connectBrocker.pkgRequest(null, Arrays.asList(TEST_STORE_PATH + "/A-1.0.0.zip"), null, null, true,
                false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<String> expectedLogs = Arrays.asList("\nDependency resolution:\n"
                + "  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
        logCaptureResult.clear();

        // B-1.0.1-SNAPSHOT must be uninstall then reinstall as it is a SNAPSHOT
        assertThat(connectBrocker.pkgRequest(null, Arrays.asList(TEST_STORE_PATH + "/B-1.0.1-SNAPSHOT.zip"), null, null,
                true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = Arrays.asList(
                "The following SNAPSHOT package(s) will be replaced in local cache : [src/test/resources/packages/store/B-1.0.1-SNAPSHOT.zip]",
                "Uninstalling B-1.0.1-SNAPSHOT", "Replacement of B-1.0.1-SNAPSHOT in local cache...",
                "Added " + TEST_STORE_PATH + "/B-1.0.1-SNAPSHOT.zip",
                "\nDependency resolution:\n" + "  Installation order (1):        B-1.0.1-SNAPSHOT\n"
                        + "  Unchanged packages (8):        A:1.0.0, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Local packages to install (1): B:1.0.1-SNAPSHOT\n",
                "Installing B-1.0.1-SNAPSHOT");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithRelax() throws Exception {
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(false);
        connectBrocker.setRelax("true");

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available release version, snapshots must be replaced
        // and optional dependencies order must be respected
        assertThat(connectBrocker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.2, B-1.0.2, C-1.0.0, D-1.0.4-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.2", "B-1.0.2",
                "C-1.0.0", "D-1.0.4-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.0", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT",
                        "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<String> expectedLogs = Arrays.asList(
                "Relax restriction to target platform null-null because of package(s) studioA, hfA, A, B, C, D, G, H, J",
                "\nDependency resolution:\n"
                        + "  Installation order (7):        A-1.2.2/B-1.0.2/D-1.0.4-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT\n"
                        + "  Unchanged packages (4):        C:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Packages to upgrade (5):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (5): A:1.2.2, B:1.0.2, hfA:1.0.8, D:1.0.4-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n",
                "Uninstalling studioA-1.0.0", "Uninstalling hfA-1.0.0", "Uninstalling D-1.0.2-SNAPSHOT",
                "Uninstalling B-1.0.1-SNAPSHOT", "Uninstalling A-1.0.0", "Installing A-1.2.2", "Installing B-1.0.2",
                "Installing D-1.0.4-SNAPSHOT", "Updating package G-1.0.1-SNAPSHOT...", "Uninstalling G-1.0.1-SNAPSHOT",
                "Removed G-1.0.1-SNAPSHOT", "Downloading [G-1.0.1-SNAPSHOT]...", "Added G-1.0.1-SNAPSHOT",
                "Installing G-1.0.1-SNAPSHOT", "Updating package H-1.0.1-SNAPSHOT...", "Uninstalling H-1.0.1-SNAPSHOT",
                "Removed H-1.0.1-SNAPSHOT", "Downloading [H-1.0.1-SNAPSHOT]...", "Added H-1.0.1-SNAPSHOT",
                "Installing H-1.0.1-SNAPSHOT", "Installing hfA-1.0.8", "Installing studioA-1.0.2-SNAPSHOT");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithRelaxAndSnapshot() throws Exception {
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(true);
        connectBrocker.setRelax("true");

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available snapshot version, snapshots must be replaced
        // and optional dependencies order must be respected
        assertThat(connectBrocker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.3-SNAPSHOT, B-1.0.2, C-1.0.2-SNAPSHOT, D-1.0.4-SNAPSHOT]
        checkPackagesState(
                connectBrocker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.3-SNAPSHOT", "B-1.0.2",
                        "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.0", "B-1.0.1-SNAPSHOT",
                        "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<String> expectedLogs = Arrays.asList(
                "Relax restriction to target platform null-null because of package(s) studioA, hfA, A, B, C, D, G, H, J",
                "\nDependency resolution:\n"
                        + "  Installation order (8):        A-1.2.3-SNAPSHOT/B-1.0.2/D-1.0.4-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT/C-1.0.2-SNAPSHOT\n"
                        + "  Unchanged packages (3):        G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Packages to upgrade (6):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (6): A:1.2.3-SNAPSHOT, B:1.0.2, hfA:1.0.8, C:1.0.2-SNAPSHOT, D:1.0.4-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n",
                "Uninstalling C-1.0.0", "Uninstalling studioA-1.0.0", "Uninstalling hfA-1.0.0",
                "Uninstalling D-1.0.2-SNAPSHOT", "Uninstalling B-1.0.1-SNAPSHOT", "Uninstalling A-1.0.0",
                "Installing A-1.2.3-SNAPSHOT", "Installing B-1.0.2", "Installing D-1.0.4-SNAPSHOT",
                "Updating package G-1.0.1-SNAPSHOT...", "Uninstalling G-1.0.1-SNAPSHOT", "Removed G-1.0.1-SNAPSHOT",
                "Downloading [G-1.0.1-SNAPSHOT]...", "Added G-1.0.1-SNAPSHOT", "Installing G-1.0.1-SNAPSHOT",
                "Updating package H-1.0.1-SNAPSHOT...", "Uninstalling H-1.0.1-SNAPSHOT", "Removed H-1.0.1-SNAPSHOT",
                "Downloading [H-1.0.1-SNAPSHOT]...", "Added H-1.0.1-SNAPSHOT", "Installing H-1.0.1-SNAPSHOT",
                "Installing hfA-1.0.8", "Installing studioA-1.0.2-SNAPSHOT", "Installing C-1.0.2-SNAPSHOT");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithTargetPlatform() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available release version on the current target
        // platform, snapshots must be replaced and optional dependencies order must be respected
        assertThat(connectBrocker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.0, B-1.0.1, C-1.0.0, D-1.0.3-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.0", "B-1.0.1",
                "C-1.0.0", "D-1.0.3-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<String> expectedLogs = Arrays.asList("Optional dependencies [B:1.0.2] will be ignored for 'A-1.2.0'.",
                "\nDependency resolution:\n"
                        + "  Installation order (7):        A-1.2.0/B-1.0.1/D-1.0.3-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT\n"
                        + "  Unchanged packages (4):        C:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Packages to upgrade (5):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (5): A:1.2.0, B:1.0.1, hfA:1.0.8, D:1.0.3-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n",
                "Uninstalling studioA-1.0.0", "Uninstalling hfA-1.0.0", "Uninstalling D-1.0.2-SNAPSHOT",
                "Uninstalling B-1.0.1-SNAPSHOT", "Uninstalling A-1.0.0", "Installing A-1.2.0", "Installing B-1.0.1",
                "Installing D-1.0.3-SNAPSHOT", "Updating package G-1.0.1-SNAPSHOT...", "Uninstalling G-1.0.1-SNAPSHOT",
                "Removed G-1.0.1-SNAPSHOT", "Downloading [G-1.0.1-SNAPSHOT]...", "Added G-1.0.1-SNAPSHOT",
                "Installing G-1.0.1-SNAPSHOT", "Updating package H-1.0.1-SNAPSHOT...", "Uninstalling H-1.0.1-SNAPSHOT",
                "Removed H-1.0.1-SNAPSHOT", "Downloading [H-1.0.1-SNAPSHOT]...", "Added H-1.0.1-SNAPSHOT",
                "Installing H-1.0.1-SNAPSHOT", "Installing hfA-1.0.8", "Installing studioA-1.0.2-SNAPSHOT");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithTargetPlatformAndSnapshot() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(true);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // all installed packages must be upgraded to their last available snapshot version on the current target
        // platform, snapshots must be replaced and optional dependencies order must be respected
        assertThat(connectBrocker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.1-SNAPSHOT, B-1.0.1, C-1.0.1-SNAPSHOT, D-1.0.3-SNAPSHOT]
        checkPackagesState(
                connectBrocker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.1-SNAPSHOT", "B-1.0.1",
                        "C-1.0.1-SNAPSHOT", "D-1.0.3-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.0", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT",
                        "B-1.0.2", "C-1.0.0", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<String> expectedLogs = Arrays.asList(
                "\nDependency resolution:\n"
                        + "  Installation order (8):        A-1.2.1-SNAPSHOT/B-1.0.1/D-1.0.3-SNAPSHOT/G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT/C-1.0.1-SNAPSHOT\n"
                        + "  Unchanged packages (3):        G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Packages to upgrade (6):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (6): A:1.2.1-SNAPSHOT, B:1.0.1, hfA:1.0.8, C:1.0.1-SNAPSHOT, D:1.0.3-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n",
                "Uninstalling C-1.0.0", "Uninstalling studioA-1.0.0", "Uninstalling hfA-1.0.0",
                "Uninstalling D-1.0.2-SNAPSHOT", "Uninstalling B-1.0.1-SNAPSHOT", "Uninstalling A-1.0.0",
                "Installing A-1.2.1-SNAPSHOT", "Installing B-1.0.1", "Installing D-1.0.3-SNAPSHOT",
                "Updating package G-1.0.1-SNAPSHOT...", "Uninstalling G-1.0.1-SNAPSHOT", "Removed G-1.0.1-SNAPSHOT",
                "Downloading [G-1.0.1-SNAPSHOT]...", "Added G-1.0.1-SNAPSHOT", "Installing G-1.0.1-SNAPSHOT",
                "Updating package H-1.0.1-SNAPSHOT...", "Uninstalling H-1.0.1-SNAPSHOT", "Removed H-1.0.1-SNAPSHOT",
                "Downloading [H-1.0.1-SNAPSHOT]...", "Added H-1.0.1-SNAPSHOT", "Installing H-1.0.1-SNAPSHOT",
                "Installing hfA-1.0.8", "Installing studioA-1.0.2-SNAPSHOT", "Installing C-1.0.1-SNAPSHOT");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testHotfixPackageRequest() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertThat(connectBrocker.pkgHotfix()).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, hfB-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "hfB-1.0.0", "A-1.0.0",
                "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<String> expectedLogs = Arrays.asList(
                "\nDependency resolution:\n" + "  Installation order (1):        hfB-1.0.0\n"
                        + "  Unchanged packages (9):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Local packages to install (1): hfB:1.0.0\n",
                "Installing hfB-1.0.0");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
        logCaptureResult.clear();

        connectBrocker.setAllowSNAPSHOT(true);
        assertThat(connectBrocker.pkgHotfix()).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, hfB-1.0.0, hfC-1.0.0-SNAPSHOT, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0,
        // D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "hfB-1.0.0",
                "hfC-1.0.0-SNAPSHOT", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.0", "A-1.2.1-SNAPSHOT",
                        "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT",
                        "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        expectedLogs = Arrays.asList(
                "\nDependency resolution:\n" + "  Installation order (1):        hfC-1.0.0-SNAPSHOT\n"
                        + "  Unchanged packages (10):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, hfB:1.0.0, studioA:1.0.0, G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Local packages to install (1): hfC:1.0.0-SNAPSHOT\n",
                "Download of 'hfC-1.0.0-SNAPSHOT' will replace the one already in local cache.",
                "Downloading [hfC-1.0.0-SNAPSHOT]...", "Replacement of hfC-1.0.0-SNAPSHOT in local cache...",
                "Added hfC-1.0.0-SNAPSHOT", "Installing hfC-1.0.0-SNAPSHOT");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testReInstallPackageRequestWithOptionalDependencies() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // G-1.0.1-SNAPSHOT and H-1.0.1-SNAPSHOT are snapshots and must be replaced
        // J-1.0.1 has optional dependencies on G and H and must be reinstalled in last position
        assertThat(connectBrocker.pkgRequest(null, Arrays.asList("H", "G"), null, null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<String> expectedLogs = Arrays.asList(
                "The following SNAPSHOT package(s) will be replaced in local cache : [G-1.0.1-SNAPSHOT, H-1.0.1-SNAPSHOT]",
                "Uninstalling G-1.0.1-SNAPSHOT", "Uninstalling H-1.0.1-SNAPSHOT",
                "Download of 'G-1.0.1-SNAPSHOT' will replace the one already in local cache.",
                "Downloading [G-1.0.1-SNAPSHOT]...", "Replacement of G-1.0.1-SNAPSHOT in local cache...",
                "Added G-1.0.1-SNAPSHOT", "Download of 'H-1.0.1-SNAPSHOT' will replace the one already in local cache.",
                "Downloading [H-1.0.1-SNAPSHOT]...", "Replacement of H-1.0.1-SNAPSHOT in local cache...",
                "Added H-1.0.1-SNAPSHOT",
                "\nAs package 'J-1.0.1' has an optional dependency on package(s) [G-1.0.1-SNAPSHOT, H-1.0.1-SNAPSHOT] currently being installed, it will be reinstalled."
                        + "\nDependency resolution:\n"
                        + "  Installation order (3):        G-1.0.1-SNAPSHOT/H-1.0.1-SNAPSHOT/J-1.0.1\n"
                        + "  Uninstallation order (1):      J-1.0.1\n"
                        + "  Unchanged packages (6):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (3): G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n"
                        + "  Local packages to remove (1):  J:1.0.1\n",
                "Uninstalling J-1.0.1", "Installing G-1.0.1-SNAPSHOT", "Installing H-1.0.1-SNAPSHOT",
                "Installing J-1.0.1");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUninstallPackageRequestWithOptionalDependencies() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, G-1.0.1-SNAPSHOT,
        // H-1.0.1-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT", "J-1.0.1"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // J-1.0.1 has optional dependencies on G and H and must be reinstalled after uninstalling G and H
        assertThat(connectBrocker.pkgRequest(null, null, Arrays.asList("H", "G"), null, true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT, J-1.0.1]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT", "J-1.0.1"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2",
                        "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT",
                        "G-1.0.1-SNAPSHOT", "H-1.0.1-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<String> expectedLogs = Arrays.asList(
                "\nAs package 'J-1.0.1' has an optional dependency on package(s) [G-1.0.1-SNAPSHOT, H-1.0.1-SNAPSHOT] currently being uninstalled, it will be reinstalled."
                        + "\nDependency resolution:\n" + "  Installation order (1):        J-1.0.1\n"
                        + "  Uninstallation order (3):      J-1.0.1/H-1.0.1-SNAPSHOT/G-1.0.1-SNAPSHOT\n"
                        + "  Unchanged packages (6):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (1): J:1.0.1\n"
                        + "  Local packages to remove (3):  G:1.0.1-SNAPSHOT, H:1.0.1-SNAPSHOT, J:1.0.1\n",
                "Uninstalling J-1.0.1", "Uninstalling H-1.0.1-SNAPSHOT", "Uninstalling G-1.0.1-SNAPSHOT",
                "Installing J-1.0.1");
        checkLogEvents(expectedLogs, logCaptureResult.getCaughtEvents());
    }

    private void checkPackagesState(ConnectBroker connectBrocker, List<String> packageIdList,
            PackageState expectedState) {
        Map<String, PackageState> states = connectBrocker.getUpdateService().getPersistence().getStates();
        for (String pkgId : packageIdList) {
            assertThat(states.get(pkgId)).as("Checking state of %s", pkgId).isEqualTo(expectedState);
        }
    }

    private void checkLogEvents(List<String> expectedLogs, List<LoggingEvent> caughtEvents) {
        assertEquals(expectedLogs.size(), caughtEvents.size());
        for (int i = 0; i < caughtEvents.size(); i++) {
            assertEquals(expectedLogs.get(i), caughtEvents.get(i).getRenderedMessage());
        }
    }

}
