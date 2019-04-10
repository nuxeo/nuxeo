/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 */

package org.nuxeo.diff.pictures.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.diff.pictures.DiffPictures;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 7.4
 */

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.diff.pictures")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.tag")
public class DiffPicturesTest {

    protected static final Log log = LogFactory.getLog(DiffPicturesTest.class);

    protected static final String ISLAND_PNG = "island.png";

    protected static final int ISLAND_W = 400;

    protected static final int ISLAND_H = 282;

    protected static final String ISLAND_MODIF_PNG = "island-modif.png";

    protected static final String PNG_MIME_TYPE = "image/png";

    @Inject
    CoreSession coreSession;

    @Test
    public void testDefaultValues() throws Exception {
        File img1 = FileUtils.getResourceFileFromContext(ISLAND_PNG);
        File img2 = FileUtils.getResourceFileFromContext(ISLAND_MODIF_PNG);

        FileBlob blob1 = new FileBlob(img1, PNG_MIME_TYPE);
        FileBlob blob2 = new FileBlob(img2, PNG_MIME_TYPE);

        Blob result;
        DiffPictures dp = new DiffPictures(blob1, blob2);

        result = dp.compare(null, null);
        assertEquals("image/png", result.getMimeType());

        BufferedImage bi = checkIsImage(result);
        assertEquals(bi.getWidth(), ISLAND_W);
        assertEquals(bi.getHeight(), ISLAND_H);

        deleteFile(result);
    }

    @Test
    public void testFuzz() throws Exception {
        File img1 = FileUtils.getResourceFileFromContext(ISLAND_PNG);
        long len1 = img1.length();
        File img2 = FileUtils.getResourceFileFromContext(ISLAND_MODIF_PNG);
        File aFile;

        FileBlob blob1 = new FileBlob(img1, PNG_MIME_TYPE);
        FileBlob blob2 = new FileBlob(img2, PNG_MIME_TYPE);

        Blob result;
        HashMap<String, Serializable> params;
        DiffPictures dp = new DiffPictures(blob1, blob2);

        // With these island.png, a blur of 10% reduce the comparison result (when using the default color values).
        // Reduces it a lot.
        // WARNING: This is a 100% dependence on ImageMagic
        params = new HashMap<>();
        params.put("fuzz", "20%");
        result = dp.compare(null, params);

        BufferedImage bi = checkIsImage(result);
        assertEquals(bi.getWidth(), ISLAND_W);
        assertEquals(bi.getHeight(), ISLAND_H);

        aFile = ((FileBlob) result).getFile();
        assertTrue("Result image with fuzz should be smaller than original", aFile.length() < (len1 / 2));

        deleteFile(result);
    }

    @Test
    public void testSamePicture() throws Exception {
        File img1 = FileUtils.getResourceFileFromContext(ISLAND_PNG);
        File img2 = FileUtils.getResourceFileFromContext(ISLAND_PNG);

        FileBlob blob1 = new FileBlob(img1, PNG_MIME_TYPE);
        FileBlob blob2 = new FileBlob(img2, PNG_MIME_TYPE);

        Blob result;
        HashMap<String, Serializable> params;
        DiffPictures dp = new DiffPictures(blob1, blob2);

        // We make the whole thing red.
        // We need the command line that allows to change the background
        params = new HashMap<>();
        params.put("highlightColor", "Red");
        params.put("lowlightColor", "Red");
        result = dp.compare("diff-pictures-default-with-params", params);

        BufferedImage bi = checkIsImage(result);
        assertEquals(bi.getWidth(), ISLAND_W);
        assertEquals(bi.getHeight(), ISLAND_H);

        // Test a 40x40 rectangle in the middle, where every pixel should be red
        int start_i = (ISLAND_W / 2) - 20;
        int max_i = (ISLAND_W / 2) + 20;
        int start_j = (ISLAND_H / 2) - 20;
        int max_j = (ISLAND_H / 2) + 20;

        for (int i = start_i; i < max_i; i++) {
            for (int j = start_j; j < max_j; j++) {
                int pixel = bi.getRGB(i, j);
                int r = pixel >> 16 & 0xff;
                int g = pixel >> 8 & 0xff;
                int b = pixel & 0xff;

                assertTrue("r should be 255", r == 255);
                assertTrue("g should be 0", g == 0);
                assertTrue("b should be 0", b == 0);
            }
        }

        deleteFile(result);
    }

    protected BufferedImage checkIsImage(Blob inBlob) throws Exception {
        assertTrue(inBlob instanceof FileBlob);
        return checkIsImage((FileBlob) inBlob);
    }

    protected BufferedImage checkIsImage(FileBlob inBlob) throws Exception {
        assertNotNull(inBlob);

        File f = inBlob.getFile();
        assertNotNull(f);
        assertTrue(f.length() > 0);

        BufferedImage bi;
        try {
            ImageIO.setCacheDirectory(Environment.getDefault().getTemp());
            bi = ImageIO.read(f);
            assertNotNull(bi);

        } catch (IOException e) {
            throw new Exception("Error reading the file", e);
        }

        return bi;
    }

    protected void deleteFile(Blob inBlob) {
        if (inBlob instanceof FileBlob) {
            File f = inBlob.getFile();
            if (f != null) {
                f.delete();
            }
        }
    }

}
