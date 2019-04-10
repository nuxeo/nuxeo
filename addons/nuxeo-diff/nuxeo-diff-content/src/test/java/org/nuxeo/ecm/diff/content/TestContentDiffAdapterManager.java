/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.diff.content.adapter.ContentDiffAdapterManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Tests the {@link ContentDiffAdapterManager}.
 *
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@RepositoryConfig(init = ContentDiffRepositoryInit.class)
@Deploy("org.nuxeo.diff.content")
public class TestContentDiffAdapterManager {

    @Inject
    protected ContentDiffAdapterManager contentDiffAdapterManager;

    @Test
    public void testDefaultHtmlConversionBlacklistedMimeTypes() {
        Set<String> mimeTypes = contentDiffAdapterManager.getHtmlConversionBlacklistedMimeTypes();
        assertEquals(13, mimeTypes.size());
        assertTrue(mimeTypes.contains("application/pdf"));
        assertTrue(mimeTypes.contains("application/vnd.ms-excel"));
        assertTrue(mimeTypes.contains("application/vnd.ms-powerpoint"));
    }

    @Test
    @Deploy("org.nuxeo.diff.content:OSGI-INF/test-blacklisted-mime-types-contrib.xml")
    public void testCustomHtmlConversionBlacklistedMimeTypes() {
        Set<String> mimeTypes = contentDiffAdapterManager.getHtmlConversionBlacklistedMimeTypes();
        assertEquals(12, mimeTypes.size());
        assertTrue(mimeTypes.contains("application/vnd.ms-excel"));
        assertTrue(mimeTypes.contains("application/msword"));
        assertTrue(mimeTypes.contains("application/rtf"));
        assertFalse(mimeTypes.contains("application/pdf"));
        assertFalse(mimeTypes.contains("application/vnd.ms-powerpoint"));
        assertFalse(mimeTypes.contains("application/vnd.sun.xml.impress"));
    }

    @Test
    @Deploy("org.nuxeo.diff.content:OSGI-INF/test-blacklisted-mime-types-override-contrib.xml")
    public void testOverriddenHtmlConversionBlacklistedMimeTypes() {
        Set<String> mimeTypes = contentDiffAdapterManager.getHtmlConversionBlacklistedMimeTypes();
        assertEquals(3, mimeTypes.size());
        assertTrue(mimeTypes.contains("application/pdf"));
        assertTrue(mimeTypes.contains("application/msword"));
        assertTrue(mimeTypes.contains("application/rtf"));
        assertFalse(mimeTypes.contains("application/vnd.ms-excel"));
        assertFalse(mimeTypes.contains("application/vnd.ms-powerpoint"));
    }
}
