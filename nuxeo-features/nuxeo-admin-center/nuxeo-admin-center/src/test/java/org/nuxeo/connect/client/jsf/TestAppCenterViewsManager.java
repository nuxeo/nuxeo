/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Pierre-Gildas MILLON
 *
 */

package org.nuxeo.connect.client.jsf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.PackageDescriptor;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.packages.PackageManagerImpl;
import org.nuxeo.connect.packages.PackageSource;
import org.nuxeo.connect.pm.tests.DummyPackageSource;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.Version;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Pierre-Gildas MILLON <pgmillon@nuxeo.com>
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
@Deploy({ "org.nuxeo.connect.client", "org.nuxeo.connect.client.wrapper" })
public class TestAppCenterViewsManager {

    private AppCenterViewsManager appCenterViewsManager;

    private static final String TEST_PROJECT_NAME = "Test Project";

    @Inject
    private PackageManager packageManager;

    @Before
    public void setUp() throws Exception {
        appCenterViewsManager = new AppCenterViewsManager();

        ((PackageManagerImpl) packageManager).resetSources();
    }

    @Test
    public void testStudioProjectWithoutSnapshot() {
        packageManager.registerSource(new DummyPackageSource(getPackages(false), "local"), false);

        assertNull(appCenterViewsManager.getStudioProjectSnapshot());
        assertNull(appCenterViewsManager.getSnapshotStudioProjectName());
    }

    @Test
    public void testGetStudioProjectWithSnapshot() {
        packageManager.registerSource(new DummyPackageSource(getPackages(true), "local"), false);

        assertNotNull(appCenterViewsManager.getStudioProjectSnapshot());
        assertEquals(TEST_PROJECT_NAME, appCenterViewsManager.getSnapshotStudioProjectName());
    }

    @Test
    public void testGetStudioProjectCache() {
        PackageSource packageSource = mock(DummyPackageSource.class);
        when(packageSource.listStudioPackages()).thenReturn(getPackages(false));
        packageManager.registerSource(packageSource, false);

        appCenterViewsManager.getStudioProjectSnapshot();
        appCenterViewsManager.getStudioProjectSnapshot();

        verify(packageSource, times(1)).listStudioPackages();
    }

    public List<DownloadablePackage> getPackages(Boolean withSnapshot) {
        PackageDescriptor snapshot = new PackageDescriptor();
        snapshot.setName(TEST_PROJECT_NAME);
        snapshot.setVersion(new Version(0, 0, 0, "SNAPSHOT"));
        snapshot.setType(PackageType.STUDIO);

        PackageDescriptor version = new PackageDescriptor();
        version.setName(TEST_PROJECT_NAME);
        version.setVersion(new Version(1, 0, 0));
        version.setType(PackageType.STUDIO);

        return Lists.newArrayList(version, withSnapshot ? snapshot : new PackageDescriptor());
    }

}
