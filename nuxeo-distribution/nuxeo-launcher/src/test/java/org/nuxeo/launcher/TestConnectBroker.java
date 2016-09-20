/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * @since TODO
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

    private void checkPackagesState(ConnectBroker connectBrocker, List<String> packageIdList,
            PackageState expectedState) {
        Map<String, PackageState> states = connectBrocker.getUpdateService().getPersistence().getStates();
        for (String pkgId : packageIdList) {
            assertEquals(expectedState, states.get(pkgId));
        }
    }

}
