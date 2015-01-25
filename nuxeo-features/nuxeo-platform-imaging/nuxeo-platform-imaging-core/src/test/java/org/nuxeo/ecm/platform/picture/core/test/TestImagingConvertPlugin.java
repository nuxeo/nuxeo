/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_CROP_X;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_CROP_Y;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_DEPTH;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_HEIGHT;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_WIDTH;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_ROTATE_ANGLE;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author Laurent Doguin
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.picture.convert" })
public class TestImagingConvertPlugin {

    @Inject
    protected ConversionService conversionService;

    @Test
    public void testResizeConverter() throws Exception {
        String converter = "pictureResize";

        int resizeWidth = 120;
        int resizeHeight = 90;
        int resizeDepth = 8;

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(OPTION_RESIZE_WIDTH, resizeWidth);
        options.put(OPTION_RESIZE_HEIGHT, resizeHeight);
        options.put(OPTION_RESIZE_DEPTH, resizeDepth);

        for (String filename : ImagingResourcesHelper.TEST_IMAGE_FILENAMES) {
            String path = ImagingResourcesHelper.TEST_DATA_FOLDER + filename;
            Blob blob = Blobs.createBlob(ImagingResourcesHelper.getFileFromPath(path));
            blob.setFilename(filename);
            BlobHolder bh = new SimpleBlobHolder(blob);

            BlobHolder result = conversionService.convert(converter, bh, options);
            assertNotNull(result);

            BufferedImage image = ImageIO.read(result.getBlob().getStream());

            assertNotNull("Resized image is null", image);
            assertEquals("Resized image height", resizeHeight, image.getHeight());
        }
    }

    @Test
    public void testRotate() throws Exception {
        String converter = "pictureRotation";

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(OPTION_ROTATE_ANGLE, 90);

        for (String filename : ImagingResourcesHelper.TEST_IMAGE_FILENAMES) {
            String path = ImagingResourcesHelper.TEST_DATA_FOLDER + filename;
            Blob blob = Blobs.createBlob(ImagingResourcesHelper.getFileFromPath(path));
            blob.setFilename(filename);
            BlobHolder bh = new SimpleBlobHolder(blob);

            BlobHolder result = conversionService.convert(converter, bh, options);
            assertNotNull(result);

            BufferedImage image = ImageIO.read(new FileInputStream(FileUtils.getResourceFileFromContext(path)));
            assertNotNull("Original image is null", image);
            int width = image.getWidth();
            int height = image.getHeight();
            assertTrue("Original image size != (0,0)", width > 0 && height > 0);

            image = ImageIO.read(result.getBlob().getStream());
            assertNotNull("Rotated image is null", image);
            assertEquals("Ratated image width", height, image.getWidth());
            assertEquals("Rotated image height", width, image.getHeight());
        }
    }

    @Test
    public void testCrop() throws Exception {
        String converter = "pictureCrop";

        int cropWidth = 400;
        int cropHeight = 200;

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(OPTION_CROP_X, 100);
        options.put(OPTION_CROP_Y, 100);
        options.put(OPTION_RESIZE_WIDTH, cropWidth);
        options.put(OPTION_RESIZE_HEIGHT, cropHeight);

        for (String filename : ImagingResourcesHelper.TEST_IMAGE_FILENAMES) {
            String path = ImagingResourcesHelper.TEST_DATA_FOLDER + filename;
            Blob blob = Blobs.createBlob(ImagingResourcesHelper.getFileFromPath(path));
            blob.setFilename(filename);
            BlobHolder bh = new SimpleBlobHolder(blob);

            BlobHolder result = conversionService.convert(converter, bh, options);
            assertNotNull(result);

            BufferedImage image = ImageIO.read(new FileInputStream(FileUtils.getResourceFileFromContext(path)));
            assertNotNull("Original image is null", image);
            int width = image.getWidth();
            int height = image.getHeight();
            assertTrue("Original image size != (0,0)", width > 0 && height > 0);

            image = ImageIO.read(result.getBlob().getStream());
            assertNotNull("Croped image is null", image);
            assertEquals("Croped image width", cropWidth, image.getWidth());
            assertEquals("Croped image height", cropHeight, image.getHeight());
        }
    }

}
