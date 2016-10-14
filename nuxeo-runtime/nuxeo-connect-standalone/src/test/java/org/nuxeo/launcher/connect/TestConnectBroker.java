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
                    try (ServletOutputStream os = response.getOutputStream()) {
                        byte[] byteArray = FileUtils.readFileToByteArray(pkgZip);
                        os.write(byteArray, 0, byteArray.length);
                    }
                    response.setStatus(HttpServletResponse.SC_OK);
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
        server.addHandler(new FakeConnectDownloadHandler());
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
                e.printStackTrace();
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
        List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(1, caughtEvents.size());
        assertEquals(
                "org.nuxeo.connect.update.PackageException: Package(s) B-1.0.2 not available on platform version server-8.3 (relax is not allowed)",
                caughtEvents.get(0).getRenderedMessage());
        logCaptureResult.clear();

        connectBrocker.setRelax("true");
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
        caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(6, caughtEvents.size());
        assertEquals("Relax restriction to target platform server-8.3 because of package(s) B-1.0.2",
                caughtEvents.get(0).getRenderedMessage());
        assertEquals(
                "\nDependency resolution:\n" + "  Installation order (2):        A-1.2.0/B-1.0.2\n"
                        + "  Unchanged packages (4):        hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Packages to upgrade (2):       A:1.0.0, B:1.0.1-SNAPSHOT\n"
                        + "  Local packages to install (2): A:1.2.0, B:1.0.2\n",
                caughtEvents.get(1).getRenderedMessage());
        assertEquals("Uninstalling B-1.0.1-SNAPSHOT", caughtEvents.get(2).getRenderedMessage());
        assertEquals("Uninstalling A-1.0.0", caughtEvents.get(3).getRenderedMessage());
        assertEquals("Installing A-1.2.0", caughtEvents.get(4).getRenderedMessage());
        assertEquals("Installing B-1.0.2", caughtEvents.get(5).getRenderedMessage());
        logCaptureResult.clear();

        // SNAPSHOTS must be replaced in local cache before installation
        assertThat(connectBrocker.pkgRequest(null, Arrays.asList("A-1.2.2-SNAPSHOT", "C-1.0.2-SNAPSHOT"), null, null,
                true, false)).isTrue();

        // After: [studioA-1.0.0, hfA-1.0.0, A-1.2.2-SNAPSHOT, B-1.0.2, C-1.0.2-SNAPSHOT, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.2.2-SNAPSHOT", "B-1.0.2",
                "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                        "A-1.2.1-SNAPSHOT", "A-1.2.0", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT", "B-1.0.1",
                        "C-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(14, caughtEvents.size());
        assertEquals("Download of 'A-1.2.2-SNAPSHOT' will replace the one already in local cache.",
                caughtEvents.get(0).getRenderedMessage());
        assertEquals("Downloading [A-1.2.2-SNAPSHOT]...", caughtEvents.get(1).getRenderedMessage());
        assertEquals("Replacement of A-1.2.2-SNAPSHOT in local cache...", caughtEvents.get(2).getRenderedMessage());
        assertEquals("Added A-1.2.2-SNAPSHOT", caughtEvents.get(3).getRenderedMessage());
        assertEquals("Download of 'C-1.0.2-SNAPSHOT' will replace the one already in local cache.",
                caughtEvents.get(4).getRenderedMessage());
        assertEquals("Downloading [C-1.0.2-SNAPSHOT]...", caughtEvents.get(5).getRenderedMessage());
        assertEquals("Replacement of C-1.0.2-SNAPSHOT in local cache...", caughtEvents.get(6).getRenderedMessage());
        assertEquals("Added C-1.0.2-SNAPSHOT", caughtEvents.get(7).getRenderedMessage());
        assertEquals(
                "Relax restriction to target platform server-8.3 because of package(s) A-1.2.2-SNAPSHOT, C-1.0.2-SNAPSHOT",
                caughtEvents.get(8).getRenderedMessage());
        assertEquals(
                "\nDependency resolution:\n" + "  Installation order (2):        A-1.2.2-SNAPSHOT/C-1.0.2-SNAPSHOT\n"
                        + "  Unchanged packages (4):        B:1.0.2, hfA:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Packages to upgrade (2):       A:1.2.0, C:1.0.0\n"
                        + "  Local packages to install (2): A:1.2.2-SNAPSHOT, C:1.0.2-SNAPSHOT\n",
                caughtEvents.get(9).getRenderedMessage());
        assertEquals("Uninstalling C-1.0.0", caughtEvents.get(10).getRenderedMessage());
        assertEquals("Uninstalling A-1.2.0", caughtEvents.get(11).getRenderedMessage());
        assertEquals("Installing A-1.2.2-SNAPSHOT", caughtEvents.get(12).getRenderedMessage());
        assertEquals("Installing C-1.0.2-SNAPSHOT", caughtEvents.get(13).getRenderedMessage());
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
        List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(3, caughtEvents.size());
        assertEquals("Added " + TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip",
                caughtEvents.get(0).getRenderedMessage());
        assertEquals("Added " + TEST_LOCAL_ONLY_PATH + "/E-1.0.1", caughtEvents.get(1).getRenderedMessage());
        assertEquals(
                "org.nuxeo.connect.update.PackageException: Package(s) F-1.0.0-SNAPSHOT not available on platform version server-8.3 (relax is not allowed)",
                caughtEvents.get(2).getRenderedMessage());
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
        caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(6, caughtEvents.size());
        assertEquals("Replacement of F-1.0.0-SNAPSHOT in local cache...", caughtEvents.get(0).getRenderedMessage());
        assertEquals("Added " + TEST_LOCAL_ONLY_PATH + "/F-1.0.0-SNAPSHOT.zip",
                caughtEvents.get(1).getRenderedMessage());
        assertEquals("Relax restriction to target platform server-8.3 because of package(s) F-1.0.0-SNAPSHOT",
                caughtEvents.get(2).getRenderedMessage());
        assertEquals(
                "\nDependency resolution:\n" + "  Installation order (2):        E-1.0.1/F-1.0.0-SNAPSHOT\n"
                        + "  Unchanged packages (6):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (2): E:1.0.1, F:1.0.0-SNAPSHOT\n",
                caughtEvents.get(3).getRenderedMessage());
        assertEquals("Installing E-1.0.1", caughtEvents.get(4).getRenderedMessage());
        assertEquals("Installing F-1.0.0-SNAPSHOT", caughtEvents.get(5).getRenderedMessage());
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
        List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(1, caughtEvents.size());
        assertEquals(
                "\nDependency resolution:\n"
                        + "  Unchanged packages (6):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n",
                caughtEvents.get(0).getRenderedMessage());
        logCaptureResult.clear();

        // B-1.0.1-SNAPSHOT and D-1.0.2-SNAPSHOT must be uninstalled then reinstalled as they are SNAPSHOTS
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
        caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(13, caughtEvents.size());
        assertEquals("Uninstalling B-1.0.1-SNAPSHOT", caughtEvents.get(0).getRenderedMessage());
        assertEquals("Download of 'B-1.0.1-SNAPSHOT' will replace the one already in local cache.",
                caughtEvents.get(1).getRenderedMessage());
        assertEquals("Downloading [B-1.0.1-SNAPSHOT]...", caughtEvents.get(2).getRenderedMessage());
        assertEquals("Replacement of B-1.0.1-SNAPSHOT in local cache...", caughtEvents.get(3).getRenderedMessage());
        assertEquals("Added B-1.0.1-SNAPSHOT", caughtEvents.get(4).getRenderedMessage());
        assertEquals("Uninstalling D-1.0.2-SNAPSHOT", caughtEvents.get(5).getRenderedMessage());
        assertEquals("Download of 'D-1.0.2-SNAPSHOT' will replace the one already in local cache.",
                caughtEvents.get(6).getRenderedMessage());
        assertEquals("Downloading [D-1.0.2-SNAPSHOT]...", caughtEvents.get(7).getRenderedMessage());
        assertEquals("Replacement of D-1.0.2-SNAPSHOT in local cache...", caughtEvents.get(8).getRenderedMessage());
        assertEquals("Added D-1.0.2-SNAPSHOT", caughtEvents.get(9).getRenderedMessage());
        assertEquals(
                "\nDependency resolution:\n" + "  Installation order (2):        B-1.0.1-SNAPSHOT/D-1.0.2-SNAPSHOT\n"
                        + "  Unchanged packages (4):        A:1.0.0, hfA:1.0.0, C:1.0.0, studioA:1.0.0\n"
                        + "  Local packages to install (2): B:1.0.1-SNAPSHOT, D:1.0.2-SNAPSHOT\n",
                caughtEvents.get(10).getRenderedMessage());
        assertEquals("Installing B-1.0.1-SNAPSHOT", caughtEvents.get(11).getRenderedMessage());
        assertEquals("Installing D-1.0.2-SNAPSHOT", caughtEvents.get(12).getRenderedMessage());
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
        List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(1, caughtEvents.size());
        assertEquals(
                "\nDependency resolution:\n"
                        + "  Unchanged packages (6):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n",
                caughtEvents.get(0).getRenderedMessage());
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
        caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(5, caughtEvents.size());
        assertEquals("Uninstalling B-1.0.1-SNAPSHOT", caughtEvents.get(0).getRenderedMessage());
        assertEquals("Replacement of B-1.0.1-SNAPSHOT in local cache...", caughtEvents.get(1).getRenderedMessage());
        assertEquals("Added " + TEST_STORE_PATH + "/B-1.0.1-SNAPSHOT.zip", caughtEvents.get(2).getRenderedMessage());
        assertEquals(
                "\nDependency resolution:\n" + "  Installation order (1):        B-1.0.1-SNAPSHOT\n"
                        + "  Unchanged packages (5):        A:1.0.0, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (1): B:1.0.1-SNAPSHOT\n",
                caughtEvents.get(3).getRenderedMessage());
        assertEquals("Installing B-1.0.1-SNAPSHOT", caughtEvents.get(4).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterWith(PkgRequestLogFilter.class)
    public void testUpgradePackageRequestWithRelax() throws Exception {
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        ((StandaloneCallbackHolder) NuxeoConnectClient.getCallBackHolder()).setTestMode(true);
        connectBrocker.setAllowSNAPSHOT(false);
        connectBrocker.setRelax("true");

        // Before: [studioA-1.0.0, hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.0", "hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT",
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertThat(connectBrocker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.2, B-1.0.2, C-1.0.0, D-1.0.4-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.2", "B-1.0.2",
                "C-1.0.0", "D-1.0.4-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.0", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT",
                        "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(12, caughtEvents.size());
        assertEquals("Relax restriction to target platform null-null because of package(s) studioA, hfA, A, B, C, D",
                caughtEvents.get(0).getRenderedMessage());
        assertEquals(
                "\nDependency resolution:\n"
                        + "  Installation order (5):        A-1.2.2/B-1.0.2/D-1.0.4-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT\n"
                        + "  Unchanged packages (1):        C:1.0.0\n"
                        + "  Packages to upgrade (5):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (5): A:1.2.2, B:1.0.2, hfA:1.0.8, D:1.0.4-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n",
                caughtEvents.get(1).getRenderedMessage());
        assertEquals("Uninstalling studioA-1.0.0", caughtEvents.get(2).getRenderedMessage());
        assertEquals("Uninstalling hfA-1.0.0", caughtEvents.get(3).getRenderedMessage());
        assertEquals("Uninstalling D-1.0.2-SNAPSHOT", caughtEvents.get(4).getRenderedMessage());
        assertEquals("Uninstalling B-1.0.1-SNAPSHOT", caughtEvents.get(5).getRenderedMessage());
        assertEquals("Uninstalling A-1.0.0", caughtEvents.get(6).getRenderedMessage());
        assertEquals("Installing A-1.2.2", caughtEvents.get(7).getRenderedMessage());
        assertEquals("Installing B-1.0.2", caughtEvents.get(8).getRenderedMessage());
        assertEquals("Installing D-1.0.4-SNAPSHOT", caughtEvents.get(9).getRenderedMessage());
        assertEquals("Installing hfA-1.0.8", caughtEvents.get(10).getRenderedMessage());
        assertEquals("Installing studioA-1.0.2-SNAPSHOT", caughtEvents.get(11).getRenderedMessage());
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
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertThat(connectBrocker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.3-SNAPSHOT, B-1.0.2, C-1.0.2-SNAPSHOT, D-1.0.4-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.3-SNAPSHOT",
                "B-1.0.2", "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.0", "B-1.0.1-SNAPSHOT",
                        "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(14, caughtEvents.size());
        assertEquals("Relax restriction to target platform null-null because of package(s) studioA, hfA, A, B, C, D",
                caughtEvents.get(0).getRenderedMessage());
        assertEquals(
                "\nDependency resolution:\n"
                        + "  Installation order (6):        A-1.2.3-SNAPSHOT/B-1.0.2/C-1.0.2-SNAPSHOT/D-1.0.4-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT\n"
                        + "  Packages to upgrade (6):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (6): A:1.2.3-SNAPSHOT, B:1.0.2, hfA:1.0.8, C:1.0.2-SNAPSHOT, D:1.0.4-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n",
                caughtEvents.get(1).getRenderedMessage());
        assertEquals("Uninstalling studioA-1.0.0", caughtEvents.get(2).getRenderedMessage());
        assertEquals("Uninstalling hfA-1.0.0", caughtEvents.get(3).getRenderedMessage());
        assertEquals("Uninstalling D-1.0.2-SNAPSHOT", caughtEvents.get(4).getRenderedMessage());
        assertEquals("Uninstalling C-1.0.0", caughtEvents.get(5).getRenderedMessage());
        assertEquals("Uninstalling B-1.0.1-SNAPSHOT", caughtEvents.get(6).getRenderedMessage());
        assertEquals("Uninstalling A-1.0.0", caughtEvents.get(7).getRenderedMessage());
        assertEquals("Installing A-1.2.3-SNAPSHOT", caughtEvents.get(8).getRenderedMessage());
        assertEquals("Installing B-1.0.2", caughtEvents.get(9).getRenderedMessage());
        assertEquals("Installing C-1.0.2-SNAPSHOT", caughtEvents.get(10).getRenderedMessage());
        assertEquals("Installing D-1.0.4-SNAPSHOT", caughtEvents.get(11).getRenderedMessage());
        assertEquals("Installing hfA-1.0.8", caughtEvents.get(12).getRenderedMessage());
        assertEquals("Installing studioA-1.0.2-SNAPSHOT", caughtEvents.get(13).getRenderedMessage());
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
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertThat(connectBrocker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.0, B-1.0.1, C-1.0.0, D-1.0.3-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.0", "B-1.0.1",
                "C-1.0.0", "D-1.0.3-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(11, caughtEvents.size());
        assertEquals(
                "\nDependency resolution:\n"
                        + "  Installation order (5):        A-1.2.0/B-1.0.1/D-1.0.3-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT\n"
                        + "  Unchanged packages (1):        C:1.0.0\n"
                        + "  Packages to upgrade (5):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (5): A:1.2.0, B:1.0.1, hfA:1.0.8, D:1.0.3-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n",
                caughtEvents.get(0).getRenderedMessage());
        assertEquals("Uninstalling studioA-1.0.0", caughtEvents.get(1).getRenderedMessage());
        assertEquals("Uninstalling hfA-1.0.0", caughtEvents.get(2).getRenderedMessage());
        assertEquals("Uninstalling D-1.0.2-SNAPSHOT", caughtEvents.get(3).getRenderedMessage());
        assertEquals("Uninstalling B-1.0.1-SNAPSHOT", caughtEvents.get(4).getRenderedMessage());
        assertEquals("Uninstalling A-1.0.0", caughtEvents.get(5).getRenderedMessage());
        assertEquals("Installing A-1.2.0", caughtEvents.get(6).getRenderedMessage());
        assertEquals("Installing B-1.0.1", caughtEvents.get(7).getRenderedMessage());
        assertEquals("Installing D-1.0.3-SNAPSHOT", caughtEvents.get(8).getRenderedMessage());
        assertEquals("Installing hfA-1.0.8", caughtEvents.get(9).getRenderedMessage());
        assertEquals("Installing studioA-1.0.2-SNAPSHOT", caughtEvents.get(10).getRenderedMessage());
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
                "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.1", "studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1",
                        "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertThat(connectBrocker.pkgUpgrade()).isTrue();

        // After: [studioA-1.0.2-SNAPSHOT, hfA-1.0.8, A-1.2.1-SNAPSHOT, B-1.0.1, C-1.0.1-SNAPSHOT, D-1.0.3-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("studioA-1.0.2-SNAPSHOT", "hfA-1.0.8", "A-1.2.1-SNAPSHOT",
                "B-1.0.1", "C-1.0.1-SNAPSHOT", "D-1.0.3-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("studioA-1.0.0", "studioA-1.0.1", "hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT",
                        "A-1.0.0", "A-1.2.0", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT", "B-1.0.1-SNAPSHOT",
                        "B-1.0.2", "C-1.0.0", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // check logs
        List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(13, caughtEvents.size());
        assertEquals(
                "\nDependency resolution:\n"
                        + "  Installation order (6):        A-1.2.1-SNAPSHOT/B-1.0.1/C-1.0.1-SNAPSHOT/D-1.0.3-SNAPSHOT/hfA-1.0.8/studioA-1.0.2-SNAPSHOT\n"
                        + "  Packages to upgrade (6):       A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                        + "  Local packages to install (6): A:1.2.1-SNAPSHOT, B:1.0.1, hfA:1.0.8, C:1.0.1-SNAPSHOT, D:1.0.3-SNAPSHOT, studioA:1.0.2-SNAPSHOT\n",
                caughtEvents.get(0).getRenderedMessage());
        assertEquals("Uninstalling studioA-1.0.0", caughtEvents.get(1).getRenderedMessage());
        assertEquals("Uninstalling hfA-1.0.0", caughtEvents.get(2).getRenderedMessage());
        assertEquals("Uninstalling D-1.0.2-SNAPSHOT", caughtEvents.get(3).getRenderedMessage());
        assertEquals("Uninstalling C-1.0.0", caughtEvents.get(4).getRenderedMessage());
        assertEquals("Uninstalling B-1.0.1-SNAPSHOT", caughtEvents.get(5).getRenderedMessage());
        assertEquals("Uninstalling A-1.0.0", caughtEvents.get(6).getRenderedMessage());
        assertEquals("Installing A-1.2.1-SNAPSHOT", caughtEvents.get(7).getRenderedMessage());
        assertEquals("Installing B-1.0.1", caughtEvents.get(8).getRenderedMessage());
        assertEquals("Installing C-1.0.1-SNAPSHOT", caughtEvents.get(9).getRenderedMessage());
        assertEquals("Installing D-1.0.3-SNAPSHOT", caughtEvents.get(10).getRenderedMessage());
        assertEquals("Installing hfA-1.0.8", caughtEvents.get(11).getRenderedMessage());
        assertEquals("Installing studioA-1.0.2-SNAPSHOT", caughtEvents.get(12).getRenderedMessage());
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
        List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(2, caughtEvents.size());
        assertEquals("\nDependency resolution:\n" + "  Installation order (1):        hfB-1.0.0\n"
                + "  Unchanged packages (6):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, studioA:1.0.0\n"
                + "  Local packages to install (1): hfB:1.0.0\n", caughtEvents.get(0).getRenderedMessage());
        assertEquals("Installing hfB-1.0.0", caughtEvents.get(1).getRenderedMessage());
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
        caughtEvents = logCaptureResult.getCaughtEvents();
        assertEquals(6, caughtEvents.size());
        assertEquals(
                "\nDependency resolution:\n" + "  Installation order (1):        hfC-1.0.0-SNAPSHOT\n"
                        + "  Unchanged packages (7):        A:1.0.0, B:1.0.1-SNAPSHOT, hfA:1.0.0, C:1.0.0, D:1.0.2-SNAPSHOT, hfB:1.0.0, studioA:1.0.0\n"
                        + "  Local packages to install (1): hfC:1.0.0-SNAPSHOT\n",
                caughtEvents.get(0).getRenderedMessage());
        assertEquals("Download of 'hfC-1.0.0-SNAPSHOT' will replace the one already in local cache.",
                caughtEvents.get(1).getRenderedMessage());
        assertEquals("Downloading [hfC-1.0.0-SNAPSHOT]...", caughtEvents.get(2).getRenderedMessage());
        assertEquals("Replacement of hfC-1.0.0-SNAPSHOT in local cache...", caughtEvents.get(3).getRenderedMessage());
        assertEquals("Added hfC-1.0.0-SNAPSHOT", caughtEvents.get(4).getRenderedMessage());
        assertEquals("Installing hfC-1.0.0-SNAPSHOT", caughtEvents.get(5).getRenderedMessage());
    }

    private void checkPackagesState(ConnectBroker connectBrocker, List<String> packageIdList,
            PackageState expectedState) {
        Map<String, PackageState> states = connectBrocker.getUpdateService().getPersistence().getStates();
        for (String pkgId : packageIdList) {
            assertThat(states.get(pkgId)).as("Checking state of %s", pkgId).isEqualTo(expectedState);
        }
    }

}
