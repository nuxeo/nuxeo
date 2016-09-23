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

package org.nuxeo.launcher;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.DefaultCallbackHolder;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.pm.tests.AbstractPackageManagerTestCase;
import org.nuxeo.connect.pm.tests.DummyPackageSource;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.launcher.config.TomcatConfigurator;
import org.nuxeo.launcher.connect.ConnectBroker;

/**
 * @since 8.4
 */
public class TestConnectBroker extends AbstractPackageManagerTestCase {

    @Override
    public void setUp() throws Exception {
        // reset CallBackHolder
        NuxeoConnectClient.setCallBackHolder(new DefaultCallbackHolder());
        super.setUp();
        // build package manager sources
        List<DownloadablePackage> local = getDownloads("local1.json");
        assertTrue(CollectionUtils.isNotEmpty(local));
        pm.registerSource(new DummyPackageSource(local, "local1"), true);

        // build env
        Environment.setDefault(null);
        File nuxeoHome = new File("target/launcher");
        FileUtils.deleteQuietly(nuxeoHome);
        nuxeoHome.mkdirs();
        System.setProperty(Environment.NUXEO_HOME, nuxeoHome.getPath());
        System.setProperty(TomcatConfigurator.TOMCAT_HOME, Environment.getDefault().getServerHome().getPath());

        // build test packages store
        File testStore = new File("src/test/resources/packages");
        FileUtils.copyDirectory(testStore, new File(nuxeoHome, "packages"));
    }

    public void testInstallPackageRequest() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        // B-1.0.2 is not available on platform version server-8.3
        assertFalse(connectBrocker.pkgRequest(null, Arrays.asList("A-1.2.0", "B-1.0.2"), null, null, true, false));

        // After: [A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        connectBrocker.setRelax("true");
        assertTrue(connectBrocker.pkgRequest(null, Arrays.asList("A-1.2.0", "B-1.0.2"), null, null, true, false));

        // After: [A-1.2.0, B-1.0.2, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("A-1.2.0", "B-1.0.2", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertTrue(connectBrocker.pkgRequest(null, Arrays.asList("A-1.2.2-SNAPSHOT", "C-1.0.2-SNAPSHOT"), null, null,
                true, false));

        // After: [A-1.2.2-SNAPSHOT, B-1.0.2, C-1.0.2-SNAPSHOT, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.2.2-SNAPSHOT", "B-1.0.2", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.0", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.3-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);
    }

    public void testUpgradePackageRequestWithRelax() throws Exception {
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        connectBrocker.setAllowSNAPSHOT(false);
        connectBrocker.setRelax("true");

        // Before: [A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertTrue(connectBrocker.pkgUpgrade());

        // After: [A-1.2.2, B-1.0.2, C-1.0.0, D-1.0.4-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("A-1.2.2", "B-1.0.2", "C-1.0.0", "D-1.0.4-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.0", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT",
                        "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);
    }

    public void testUpgradePackageRequestWithRelaxAndSnapshot() throws Exception {
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        connectBrocker.setAllowSNAPSHOT(true);
        connectBrocker.setRelax("true");

        // Before: [A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertTrue(connectBrocker.pkgUpgrade());

        // After: [A:1.2.3-SNAPSHOT, B:1.0.2, C:1.0.2-SNAPSHOT, D:1.0.4-SNAPSHOT]
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.2.3-SNAPSHOT", "B-1.0.2", "C-1.0.2-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.0",
                        "B-1.0.1-SNAPSHOT", "B-1.0.1", "C-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT",
                        "D-1.0.3-SNAPSHOT"),
                PackageState.DOWNLOADED);
    }

    public void testUpgradePackageRequestWithTargetPlatform() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertTrue(connectBrocker.pkgUpgrade());

        // After: [A-1.2.0, B-1.0.1, C-1.0.0, D-1.0.3-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("A-1.2.0", "B-1.0.1", "C-1.0.0", "D-1.0.3-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.0.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);
    }

    public void testUpgradePackageRequestWithTargetPlatformAndSnapshot() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        connectBrocker.setAllowSNAPSHOT(true);

        // Before: [A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertTrue(connectBrocker.pkgUpgrade());

        // After: [A-1.2.1-SNAPSHOT, B-1.0.1, C-1.0.1-SNAPSHOT, D-1.0.3-SNAPSHOT]
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.2.1-SNAPSHOT", "B-1.0.1", "C-1.0.1-SNAPSHOT", "D-1.0.3-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.0.0", "A-1.2.0", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1-SNAPSHOT", "B-1.0.2", "C-1.0.0", "C-1.0.2-SNAPSHOT", "D-1.0.2-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);
    }

    public void testHotfixPackageRequest() throws Exception {
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_NAME, "server");
        Environment.getDefault().setProperty(Environment.DISTRIBUTION_VERSION, "8.3");
        ConnectBroker connectBrocker = new ConnectBroker(Environment.getDefault());
        connectBrocker.setAllowSNAPSHOT(false);

        // Before: [hfA-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker,
                Arrays.asList("hfA-1.0.0", "A-1.0.0", "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"),
                PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2",
                        "A-1.2.3-SNAPSHOT", "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT",
                        "D-1.0.3-SNAPSHOT", "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        assertTrue(connectBrocker.pkgHotfix());

        // After: [hfA-1.0.0, hfB-1.0.0, hfC-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("hfA-1.0.0", "hfB-1.0.0", "A-1.0.0",
                "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("hfC-1.0.0-SNAPSHOT", "A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);

        connectBrocker.setAllowSNAPSHOT(true);
        assertTrue(connectBrocker.pkgHotfix());

        // After: [hfA-1.0.0, hfB-1.0.0, hfC-1.0.0, A-1.0.0, B-1.0.1-SNAPSHOT, C-1.0.0, D-1.0.2-SNAPSHOT]
        checkPackagesState(connectBrocker, Arrays.asList("hfA-1.0.0", "hfB-1.0.0", "hfC-1.0.0-SNAPSHOT", "A-1.0.0",
                "B-1.0.1-SNAPSHOT", "C-1.0.0", "D-1.0.2-SNAPSHOT"), PackageState.STARTED);
        checkPackagesState(connectBrocker,
                Arrays.asList("A-1.2.0", "A-1.2.1-SNAPSHOT", "A-1.2.2-SNAPSHOT", "A-1.2.2", "A-1.2.3-SNAPSHOT",
                        "B-1.0.1", "B-1.0.2", "C-1.0.1-SNAPSHOT", "C-1.0.2-SNAPSHOT", "D-1.0.3-SNAPSHOT",
                        "D-1.0.4-SNAPSHOT"),
                PackageState.DOWNLOADED);
    }

    private void checkPackagesState(ConnectBroker connectBrocker, List<String> packageIdList,
            PackageState expectedState) {
        Map<String, PackageState> states = connectBrocker.getUpdateService().getPersistence().getStates();
        for (String pkgId : packageIdList) {
            assertEquals("Wrong state for " + pkgId, expectedState, states.get(pkgId));
        }
    }

}
