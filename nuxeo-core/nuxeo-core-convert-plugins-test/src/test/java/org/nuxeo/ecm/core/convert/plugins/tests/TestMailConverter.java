/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.convert.plugins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Anahide Tchertchian
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestMailConverter extends SimpleConverterTest {

    private static final String CONVERTER_NAME = "rfc822totext";

    private static Blob getTestBlob(String filePath) throws IOException {
        File file = FileUtils.getResourceFileFromContext(filePath);
        return Blobs.createBlob(file);
    }

    @Inject
    protected ConversionService cs;

    @Test
    public void testTextEmailTransformation() throws Exception {
        BlobHolder bh;
        if (SystemUtils.IS_OS_WINDOWS) {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs\\email\\text.eml"), null);
        } else {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs/email/text.eml"), null);
        }
        assertNotNull(bh);

        Blob result = bh.getBlob();
        assertNotNull(result);
        assertEquals("text/plain", result.getMimeType());

        Blob expected;
        if (SystemUtils.IS_OS_WINDOWS) {
            expected = getTestBlob("test-docs\\email\\text.txt");
        } else {
            expected = getTestBlob("test-docs/email/text.txt");
        }
        assertEquals(expected.getString().trim(), result.getString().trim());
    }

    protected boolean textEquals(String txt1, String txt2) {
        txt1 = txt1.replaceAll("\n\r", " ");
        txt1 = txt1.replaceAll("\n", " ");

        txt2 = txt2.replaceAll("\n\r", " ");
        txt2 = txt2.replaceAll("\n", " ");

        txt1 = txt1.trim();
        txt2 = txt2.trim();

        return txt1.equals(txt2);
    }

    @Test
    public void testTextAndHtmlEmailTransformation() throws Exception {
        BlobHolder bh;
        if (SystemUtils.IS_OS_WINDOWS) {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs\\email\\text_and_html_with_attachments.eml"),
                    null);
        } else {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs/email/text_and_html_with_attachments.eml"), null);
        }
        assertNotNull(bh);

        Blob result = bh.getBlob();
        assertNotNull(result);
        assertEquals("text/plain", result.getMimeType());

        String actual = result.getString();
        String expected;
        if (SystemUtils.IS_OS_WINDOWS) {
            expected = getTestBlob("test-docs\\email\\text_and_html_with_attachments.txt").getString();
        } else {
            expected = getTestBlob("test-docs/email/text_and_html_with_attachments.txt").getString();
        }
        assertTrue(FileUtils.areFilesContentEquals(expected.trim(), actual.trim()));
    }

    @Test
    public void testOnlyHtmlEmailTransformation() throws Exception {
        BlobHolder bh;
        if (SystemUtils.IS_OS_WINDOWS) {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs\\email\\only_html_with_attachments.eml"), null);
        } else {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs/email/only_html_with_attachments.eml"), null);
        }
        assertNotNull(bh);

        Blob result = bh.getBlob();
        assertNotNull(result);
        assertEquals("text/plain", result.getMimeType());
        Blob expected;
        if (SystemUtils.IS_OS_WINDOWS) {
            expected = getTestBlob("test-docs\\email\\only_html_with_attachments.txt");
        } else {
            expected = getTestBlob("test-docs/email/only_html_with_attachments.txt");
        }
        assertTrue(FileUtils.areFilesContentEquals(expected.getString().trim(), result.getString().trim()));
    }

}
