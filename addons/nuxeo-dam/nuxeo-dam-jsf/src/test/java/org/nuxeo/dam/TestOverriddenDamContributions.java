/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.types.api", "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.webapp.base:OSGI-INF/ecm-types-contrib.xml",
        "org.nuxeo.ecm.platform.picture.jsf:OSGI-INF/imaging-types-contrib.xml",
        "org.nuxeo.ecm.platform.video.jsf:OSGI-INF/ui-types-contrib.xml",
        "org.nuxeo.ecm.platform.audio.jsf:OSGI-INF/ecm-types-contrib.xml",
        "org.nuxeo.dam.jsf:OSGI-INF/dam-service.xml", "org.nuxeo.dam.jsf:OSGI-INF/dam-service-contrib.xml" })
@LocalDeploy("org.nuxeo.ecm.platform.types.api:dam-service-contrib-test.xml")
public class TestOverriddenDamContributions {

    @Inject
    protected DamService damService;

    @Test
    public void testAssetLibraryOverride() throws Exception {
        AssetLibrary assetLibrary = damService.getAssetLibrary();
        assertNotNull(assetLibrary);

        assertEquals(DamConstants.ASSET_LIBRARY_TYPE, assetLibrary.getDocType());
        assertEquals("Media Library", assetLibrary.getTitle());
        assertEquals("/media-library", assetLibrary.getPath());
        assertEquals("desc", assetLibrary.getDescription());
    }

    @Test
    public void testAllowedAssetTypesOverride() throws Exception {
        List<Type> types = damService.getAllowedAssetTypes();
        assertNotNull(types);
        assertFalse(types.isEmpty());
        assertEquals(3, types.size());

        List<String> expectedTypes = Arrays.asList("Audio", "File", "Folder");
        for (Type type : types) {
            assertTrue(expectedTypes.contains(type.getId()));
        }
    }
}
