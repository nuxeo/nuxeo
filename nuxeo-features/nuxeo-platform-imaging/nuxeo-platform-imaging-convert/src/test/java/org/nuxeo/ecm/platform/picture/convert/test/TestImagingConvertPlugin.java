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
package org.nuxeo.ecm.platform.picture.convert.test;

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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Laurent Doguin
 *
 */
public class TestImagingConvertPlugin extends NXRuntimeTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.picture.convert");
    }

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

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        for (String filename : ImagingRessourcesHelper.TEST_IMAGE_FILENAMES) {
            String path = ImagingRessourcesHelper.TEST_DATA_FOLDER + filename;
            Blob blob = new FileBlob(ImagingRessourcesHelper.getFileFromPath(path));
            blob.setFilename(filename);
            BlobHolder bh = new SimpleBlobHolder(blob);

            BlobHolder result = cs.convert(converter, bh, options);
            assertNotNull(result);

            BufferedImage image = ImageIO.read(result.getBlob().getStream());
            assertNotNull("Resized image is null", image);
            assertEquals("Resized image width", resizeWidth, image.getWidth());
            assertEquals("Resized image height", resizeHeight,
                    image.getHeight());
        }
    }

    @Test
    public void testRotate() throws Exception {

        String converter = "pictureRotation";

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(OPTION_ROTATE_ANGLE, 90);

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        for (String filename : ImagingRessourcesHelper.TEST_IMAGE_FILENAMES) {
            String path = ImagingRessourcesHelper.TEST_DATA_FOLDER + filename;
            Blob blob = new FileBlob(ImagingRessourcesHelper.getFileFromPath(path));
            blob.setFilename(filename);
            BlobHolder bh = new SimpleBlobHolder(blob);

            BlobHolder result = cs.convert(converter, bh, options);
            assertNotNull(result);

            BufferedImage image = ImageIO.read(new FileInputStream(
                    FileUtils.getResourceFileFromContext(path)));
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

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        for (String filename : ImagingRessourcesHelper.TEST_IMAGE_FILENAMES) {
            String path = ImagingRessourcesHelper.TEST_DATA_FOLDER + filename;
            Blob blob = new FileBlob(ImagingRessourcesHelper.getFileFromPath(path));
            blob.setFilename(filename);
            BlobHolder bh = new SimpleBlobHolder(blob);

            BlobHolder result = cs.convert(converter, bh, options);
            assertNotNull(result);

            BufferedImage image = ImageIO.read(new FileInputStream(
                    FileUtils.getResourceFileFromContext(path)));
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
