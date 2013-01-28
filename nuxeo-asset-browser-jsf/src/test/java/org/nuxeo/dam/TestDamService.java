/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.dam;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.dam.jsf:OSGI-INF/dam-service.xml",
        "org.nuxeo.dam.jsf:OSGI-INF/dam-service-contrib.xml" })
public class TestDamService {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected DamService damService;

    @Test
    public void testAssetLibrary() {
        assertNotNull(damService);

        AssetLibrary assetLibrary = damService.getAssetLibrary();
        assertNotNull(assetLibrary);

        assertEquals(DamConstants.ASSET_LIBRARY_TYPE, assetLibrary.getDocType());
        assertEquals("Asset Library", assetLibrary.getTitle());
        assertEquals("/asset-library", assetLibrary.getPath());
        assertNull(assetLibrary.getDescription());
    }

    @Test
    public void testAssetLibraryOverride() throws Exception {
        URL url = TestDamService.class.getClassLoader().getResource(
                "dam-service-contrib-test.xml");
        harness.deployTestContrib("org.nuxeo.ecm.core", url);

        AssetLibrary assetLibrary = damService.getAssetLibrary();
        assertNotNull(assetLibrary);

        assertEquals(DamConstants.ASSET_LIBRARY_TYPE, assetLibrary.getDocType());
        assertEquals("Media Library", assetLibrary.getTitle());
        assertEquals("/media-library", assetLibrary.getPath());
        assertEquals("desc", assetLibrary.getDescription());
    }

}
