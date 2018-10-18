/*
 * (C) Copyright 2007-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
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

/**
 * @author Laurent Doguin
 */
@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.tag")
public class TestImagingConvertPlugin {

    @Inject
    protected ConversionService conversionService;

    @Before
    public void setup() {
        ImageIO.setCacheDirectory(Environment.getDefault().getTemp());
    }

    @Test
    public void testResizeConverter() throws Exception {
        String converter = "pictureResize";

        int resizeWidth = 120;
        int resizeHeight = 90;
        int resizeDepth = 8;

        Map<String, Serializable> options = new HashMap<>();
        options.put(OPTION_RESIZE_WIDTH, resizeWidth);
        options.put(OPTION_RESIZE_HEIGHT, resizeHeight);
        options.put(OPTION_RESIZE_DEPTH, resizeDepth);

        doOnTestImages((bh, path) -> {
            BlobHolder result = conversionService.convert(converter, bh, options);
            assertNotNull(result);

            BufferedImage image = readImage(result.getBlob());

            assertNotNull("Resized image is null", image);
            assertEquals("Resized image height", resizeHeight, image.getHeight());
        });
    }

    @Test
    public void testRotate() throws Exception {
        String converter = "pictureRotation";

        Map<String, Serializable> options = new HashMap<>();
        options.put(OPTION_ROTATE_ANGLE, 90);

        doOnTestImages((bh, path) -> {
            BlobHolder result = conversionService.convert(converter, bh, options);
            assertNotNull(result);

            BufferedImage image = readImage(path);
            assertNotNull("Original image is null", image);
            int width = image.getWidth();
            int height = image.getHeight();
            assertTrue("Original image size != (0,0)", width > 0 && height > 0);

            image = readImage(result.getBlob());
            assertNotNull("Rotated image is null", image);
            assertEquals("Ratated image width", height, image.getWidth());
            assertEquals("Rotated image height", width, image.getHeight());
        });
    }

    @Test
    public void testCrop() throws Exception {
        String converter = "pictureCrop";

        int cropWidth = 400;
        int cropHeight = 200;

        Map<String, Serializable> options = new HashMap<>();
        options.put(OPTION_CROP_X, 100);
        options.put(OPTION_CROP_Y, 100);
        options.put(OPTION_RESIZE_WIDTH, cropWidth);
        options.put(OPTION_RESIZE_HEIGHT, cropHeight);

        doOnTestImages((bh, path) -> {
            BlobHolder result = conversionService.convert(converter, bh, options);
            assertNotNull(result);

            BufferedImage image = readImage(path);
            assertNotNull("Original image is null", image);
            int width = image.getWidth();
            int height = image.getHeight();
            assertTrue("Original image size != (0,0)", width > 0 && height > 0);

            image = readImage(result.getBlob());
            assertNotNull("Croped image is null", image);
            assertEquals("Croped image width", cropWidth, image.getWidth());
            assertEquals("Croped image height", cropHeight, image.getHeight());
        });
    }

    @Test
    public void testConvertToPDF() throws Exception {
        String converter = "pictureConvertToPDF";
        doOnTestImages((bh, path) -> {
            BlobHolder result = conversionService.convert(converter, bh, null);
            assertNotNull(result);
        });
    }

    protected void doOnTestImages(BiConsumer<BlobHolder, String> consumer) throws IOException {
        for (String filename : ImagingResourcesHelper.TEST_IMAGE_FILENAMES) {
            String path = ImagingResourcesHelper.TEST_DATA_FOLDER + filename;
            BlobHolder bh = getBlobHolder(path);
            consumer.accept(bh, path);
        }
    }

    protected BlobHolder getBlobHolder(String path) throws IOException {
        Blob blob = Blobs.createBlob(ImagingResourcesHelper.getFileFromPath(path));
        return new SimpleBlobHolder(blob);
    }

    protected BufferedImage readImage(String path) {
        try {
            return readImage(new FileInputStream(FileUtils.getResourceFileFromContext(path)));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected BufferedImage readImage(Blob blob) {
        try {
            return readImage(blob.getStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected BufferedImage readImage(InputStream inputStream) {
        try {
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
