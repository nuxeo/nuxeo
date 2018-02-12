/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Pierre-Gildas MILLON
 *
 */

package org.nuxeo.connect.client.jsf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.inject.Inject;

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

import com.google.common.collect.Lists;

/**
 * @author Pierre-Gildas MILLON <pgmillon@nuxeo.com>
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
@Deploy("org.nuxeo.connect.client")
@Deploy("org.nuxeo.connect.client.wrapper")
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
