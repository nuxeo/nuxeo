/*
 * (C) Copyright 2018-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.convert.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.convert.tests.ConvertFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(ConvertFeature.class)
@Deploy("org.nuxeo.ecm.core.mimetype")
@Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib6.xml")
public class TestMimeTypeTranslationHelper {

    @Inject
    protected MimeTypeTranslationHelper mimeTypeTranslationHelper;

    @Test
    public void testGetConverterName() {
        assertEquals("toText2", mimeTypeTranslationHelper.getConverterName("application/pdf", "text/plain"));
        assertEquals("wildcard", mimeTypeTranslationHelper.getConverterName("foo", "text/plain"));
        assertEquals("wildcard-subtype", mimeTypeTranslationHelper.getConverterName("image/bmp", "image/jpg"));
        assertEquals("toJpg1", mimeTypeTranslationHelper.getConverterName("image/png", "image/jpg"));

        String mimeType = "text/html; charset=ISO-8859-15;\n"
                + " name=\"Nuxeo: open source ECM - Enterprise Content Management.html\"";
        assertEquals("html2text", mimeTypeTranslationHelper.getConverterName(mimeType, "text/plain"));
    }

    @Test
    public void testGetConverterNames() {
        List<String> converterNames = mimeTypeTranslationHelper.getConverterNames("application/pdf", "text/plain");
        assertEquals(2, converterNames.size());
        assertTrue(converterNames.contains("toText1"));
        assertTrue(converterNames.contains("toText2"));
        converterNames = mimeTypeTranslationHelper.getConverterNames("foo", "text/plain");
        assertEquals(1, converterNames.size());
        assertEquals("wildcard", converterNames.get(0));
        converterNames = mimeTypeTranslationHelper.getConverterNames("image/bmp", "image/jpg");
        assertEquals(1, converterNames.size());
        assertEquals("wildcard-subtype", converterNames.get(0));
        converterNames = mimeTypeTranslationHelper.getConverterNames("image/png", "image/jpg");
        assertEquals(1, converterNames.size());
        assertEquals("toJpg1", converterNames.get(0));
    }

    // NXP-30483
    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib9.xml")
    public void testGetConverterNamesWithSrcMatchingExactlyButNotDstWhereasSrcWithSubWildcardAndDstMatch() {
        List<String> converterNames = mimeTypeTranslationHelper.getConverterNames("image/tiff", "application/pdf");
        assertEquals(1, converterNames.size());
        assertTrue(converterNames.contains("image2Pdf"));
    }

    @Test
    public void testHasCompatibleMimeType() {
        List<String> mimeTypes = Arrays.asList("application/pdf", "text/plain");
        assertTrue(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "application/pdf"));
        assertTrue(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "text/plain"));
        assertFalse(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "image/png"));
        assertFalse(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "text/html"));
        mimeTypes = Arrays.asList("video/*", "image/png");
        assertTrue(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "video/mp4"));
        assertTrue(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "video/avi"));
        assertTrue(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "image/png"));
        assertFalse(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "image/jpeg"));
        assertFalse(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "image/bmp"));
        mimeTypes = Collections.singletonList("*");
        assertTrue(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "video/mp4"));
        assertTrue(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "image/jpeg"));
        assertTrue(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "text/plain"));
        assertTrue(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "text/html"));
        assertTrue(mimeTypeTranslationHelper.hasCompatibleMimeType(mimeTypes, "application/pdf"));
    }
}
