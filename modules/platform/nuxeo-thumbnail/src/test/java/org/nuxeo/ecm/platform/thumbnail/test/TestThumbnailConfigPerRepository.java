/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.thumbnail.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailConfigDescriptor;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.core.test.MultiRepositoryFeature;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests that {@link ThumbnailService#getConfiguration(String)} returns per-repository configuration when a contribution
 * targets a specific repository via its {@code repository} attribute.
 *
 * @since 2025.22
 */
@RunWith(FeaturesRunner.class)
@Features({ ThumbnailFeature.class, MultiRepositoryFeature.class })
public class TestThumbnailConfigPerRepository {

    @Inject
    protected ThumbnailService thumbnailService;

    @Test
    public void testGetConfigurationDefaultDisabled() {
        ThumbnailService service = Framework.getService(ThumbnailService.class);
        ThumbnailConfigDescriptor config = service.getConfiguration("default");
        assertNotNull(config);
        assertFalse(config.isDisabled());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.thumbnail.test:test-thumbnail-disabled-contrib.xml")
    public void testGetConfigurationDisabled() {
        ThumbnailService service = Framework.getService(ThumbnailService.class);
        ThumbnailConfigDescriptor config = service.getConfiguration("default");
        assertNotNull(config);
        assertTrue(config.isDisabled());
    }

    @Test
    public void testDescriptorDefaults() {
        ThumbnailConfigDescriptor d = new ThumbnailConfigDescriptor();
        assertEquals(ThumbnailConfigDescriptor.DEFAULT_ID, d.getId());
        assertNull(d.getRepository());
        assertFalse(d.isDisabled());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.thumbnail.test:test-thumbnail-disabled-other-contrib.xml")
    public void testPerRepositoryConfigurationIsolation() {
        // default repository has no explicit contribution: thumbnails are enabled
        ThumbnailConfigDescriptor defaultConfig = thumbnailService.getConfiguration("default");
        assertNotNull(defaultConfig);
        assertFalse("Thumbnail should be enabled on default repository", defaultConfig.isDisabled());

        // "other" repository has a contribution disabling thumbnail
        ThumbnailConfigDescriptor otherConfig = thumbnailService.getConfiguration("other");
        assertNotNull(otherConfig);
        assertTrue("Thumbnail should be disabled on 'other' repository", otherConfig.isDisabled());
        assertEquals("other", otherConfig.getRepository());
    }

    @Test
    public void testUnknownRepositoryFallsBackToDefault() {
        // an unknown repository name falls back to the default contribution (or an empty descriptor)
        ThumbnailConfigDescriptor unknown = thumbnailService.getConfiguration("does-not-exist");
        assertNotNull(unknown);
        assertFalse(unknown.isDisabled());
    }

}
