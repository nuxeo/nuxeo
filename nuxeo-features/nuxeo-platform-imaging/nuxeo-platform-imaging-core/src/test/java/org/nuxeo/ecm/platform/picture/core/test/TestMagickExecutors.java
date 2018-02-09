/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageConverter;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageCropper;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageCropperAndResizer;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageIdentifier;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageResizer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.platform.picture.core" })
@Deploy({ "org.nuxeo.ecm.platform.picture.core:OSGI-INF/commandline-imagemagick-contrib.xml" })
public class TestMagickExecutors {

    private static final String TMP_FILE_PREFIX = TestMagickExecutors.class.getName() + "_";

    @Test
    public void testIdentify() throws Exception {
        File file = FileUtils.getResourceFileFromContext("images/test.jpg");

        ImageInfo info = ImageIdentifier.getInfo(file.getAbsolutePath());

        assertNotNull(info);
        assertEquals("JPEG", info.getFormat());
        assertFalse(info.getWidth() == 0);
        assertFalse(info.getHeight() == 0);
        assertTrue(info.getColorSpace().endsWith("RGB"));

        System.out.print(info);
    }

    @Test
    public void testJpegSimplier() throws Exception {
        File out = Framework.createTempFile(TMP_FILE_PREFIX, ".test_small.jpg");
        File file = FileUtils.getResourceFileFromContext("images/test.jpg");

        ImageInfo info = ImageResizer.resize(file.getAbsolutePath(), out.getAbsolutePath(), 20, 20, 8);
        assertNotNull(info);

        assertTrue(out.exists());
        assertTrue(out.length() < file.length());
        out.delete();
    }

    @Test
    public void testCropper() throws Exception {
        File out = Framework.createTempFile(TMP_FILE_PREFIX, ".test_crop.jpg");
        File file = FileUtils.getResourceFileFromContext("images/test.jpg");

        ImageCropper.crop(file.getAbsolutePath(), out.getAbsolutePath(), 255, 255, 10, 10);

        assertTrue(out.exists());
        assertTrue(out.length() < file.length());

        ImageInfo info = ImageIdentifier.getInfo(out.getAbsolutePath());
        assertNotNull(info);
        assertEquals(255, info.getWidth());
        assertEquals(255, info.getHeight());
        out.delete();
    }

    @Test
    public void testCropperAndResize() throws Exception {
        File out = Framework.createTempFile(TMP_FILE_PREFIX, ".test_crop_resized.jpg");
        File file = FileUtils.getResourceFileFromContext("images/test.jpg");

        ImageCropperAndResizer.cropAndResize(file.getAbsolutePath(), out.getAbsolutePath(), 255, 255, 10, 10, 200, 200);

        assertTrue(out.exists());
        assertTrue(out.length() < file.length());

        ImageInfo info = ImageIdentifier.getInfo(out.getAbsolutePath());
        assertNotNull(info);
        assertEquals(200, info.getWidth());
        assertEquals(200, info.getHeight());

        out.delete();
    }

    @Test
    public void testConverterWithBmp() throws Exception {
        File file = FileUtils.getResourceFileFromContext("images/andy.bmp");
        File out = Framework.createTempFile(TMP_FILE_PREFIX, ".andy.jpg");

        ImageConverter.convert(file.getAbsolutePath(), out.getAbsolutePath());

        ImageInfo info = ImageIdentifier.getInfo(out.getAbsolutePath());
        assertNotNull(info);
        assertEquals("JPEG", info.getFormat());

        out.delete();
    }

    @Test
    public void testConverterWithGif() throws Exception {
        File file = FileUtils.getResourceFileFromContext("images/cat.gif");
        File out = Framework.createTempFile(TMP_FILE_PREFIX, ".cat.jpg");

        ImageConverter.convert(file.getAbsolutePath(), out.getAbsolutePath());

        ImageInfo info = ImageIdentifier.getInfo(out.getAbsolutePath());
        assertNotNull(info);
        assertEquals("JPEG", info.getFormat());

        out.delete();
    }

    @Test
    public void testConverterToPDF() throws Exception {
        File file = FileUtils.getResourceFileFromContext("images/test.jpg");
        File out = Framework.createTempFile(TMP_FILE_PREFIX, ".document.pdf");

        ImageConverter.convert(file.getAbsolutePath(), out.getAbsolutePath());

        assertEquals("pdf", FilenameUtils.getExtension(out.getAbsolutePath()));
        PDDocument doc = PDDocument.load(out);
        assertNotNull(doc);

        out.delete();
    }
}
