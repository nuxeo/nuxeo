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
package org.nuxeo.apidoc.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestPackageInfoMarketplaceURL {

    protected PackageInfo getMockPackage() {
        PackageInfo mockPackage = mock(PackageInfo.class);
        when(mockPackage.getName()).thenReturn("platform-explorer");
        when(mockPackage.getVersion()).thenReturn("1.8.3");
        return mockPackage;
    }

    @Test
    public void testMarketplaceURL() {
        PackageInfo pkg = getMockPackage();
        assertEquals("https://connect.nuxeo.com/nuxeo/site/marketplace/package/platform-explorer?version=1.8.3",
                PackageInfo.getMarketplaceURL(pkg, false));

        when(pkg.getName()).thenReturn("platform-explorer-foo");
        assertEquals("https://connect.nuxeo.com/nuxeo/site/marketplace/package/platform-explorer-foo?version=1.8.3",
                PackageInfo.getMarketplaceURL(pkg, false));
    }

    @Ignore("Helps checking marketplace actual behavior on valid and invalid packages.")
    @Test
    public void testMarketplaceURLCheckValidity() {
        PackageInfo pkg = getMockPackage();
        assertEquals("https://connect.nuxeo.com/nuxeo/site/marketplace/package/platform-explorer?version=1.8.3",
                PackageInfo.getMarketplaceURL(pkg, true));

        when(pkg.getName()).thenReturn("platform-explorer-foo");
        assertNull(PackageInfo.getMarketplaceURL(pkg, true));
    }

}
