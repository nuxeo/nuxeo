package org.nuxeo.ecm.platform.pictures.tiles.service.test;

import java.io.File;

import junit.framework.TestCase;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.pictures.tiles.magick.utils.ImageConverter;
import org.nuxeo.ecm.platform.pictures.tiles.magick.utils.ImageCropper;
import org.nuxeo.ecm.platform.pictures.tiles.magick.utils.ImageCropperAndResizer;
import org.nuxeo.ecm.platform.pictures.tiles.magick.utils.ImageIdentifier;
import org.nuxeo.ecm.platform.pictures.tiles.magick.utils.ImageInfo;
import org.nuxeo.ecm.platform.pictures.tiles.magick.utils.JpegSimplifier;
import org.nuxeo.ecm.platform.pictures.tiles.magick.utils.MultiTiler;

public class TestMagickExecutors extends TestCase {

    public void testIdentify() throws Exception {

        File file = FileUtils.getResourceFileFromContext("test.jpg");

        ImageInfo info = ImageIdentifier.getInfo(file.getAbsolutePath());

        assertNotNull(info);
        assertEquals("JPEG", info.getFormat());
        assertFalse(info.getWidth() == 0);
        assertFalse(info.getHeight() == 0);

        System.out.print(info);
    }

    public void testJpegSimplier() throws Exception {

        String outputFile = System.getProperty("java.io.tmpdir")
                + "/test_small.jpg";

        File file = FileUtils.getResourceFileFromContext("test.jpg");

        ImageInfo info = JpegSimplifier.simplify(file.getAbsolutePath(),
                outputFile, 20, 20);
        assertNotNull(info);

        File out = new File(outputFile);
        assertTrue(out.exists());
        assertTrue(out.length() < file.length());

    }

    public void testCropper() throws Exception {

        String outputFilePath = System.getProperty("java.io.tmpdir")
                + "/test_crop.jpg";

        File file = FileUtils.getResourceFileFromContext("test.jpg");

        ImageCropper.crop(file.getAbsolutePath(), outputFilePath, 255, 255, 10,
                10);

        File out = new File(outputFilePath);
        assertTrue(out.exists());
        assertTrue(out.length() < file.length());

        ImageInfo info = ImageIdentifier.getInfo(outputFilePath);
        assertNotNull(info);
        assertEquals(255, info.getWidth());
        assertEquals(255, info.getHeight());

    }

    public void testCropperAndResize() throws Exception {

        String outputFilePath = System.getProperty("java.io.tmpdir")
                + "/test_crop_resized.jpg";

        File file = FileUtils.getResourceFileFromContext("test.jpg");

        ImageCropperAndResizer.cropAndResize(file.getAbsolutePath(),
                outputFilePath, 255, 255, 10, 10, 200, 200);

        File out = new File(outputFilePath);
        assertTrue(out.exists());
        assertTrue(out.length() < file.length());

        ImageInfo info = ImageIdentifier.getInfo(outputFilePath);
        assertNotNull(info);
        assertEquals(200, info.getWidth());
        assertEquals(200, info.getHeight());

    }

    public void testTiler() throws Exception {

        String outputPath = System.getProperty("java.io.tmpdir")
                + "/test_tiles/";
        new File(outputPath).mkdir();
        File file = FileUtils.getResourceFileFromContext("test.jpg");

        MultiTiler.tile(file.getAbsolutePath(), outputPath, 255, 255);

        File outDir = new File(outputPath);
        String[] tiles = outDir.list();
        assertTrue(tiles.length > 0);
    }

    public void testConverterWithBmp() throws Exception {
        File file = FileUtils.getResourceFileFromContext("andy.bmp");

        String outputFilePath = System.getProperty("java.io.tmpdir")
                + "/andy.jpg";

        ImageConverter.convert(file.getAbsolutePath(), outputFilePath);

        ImageInfo info = ImageIdentifier.getInfo(outputFilePath);
        assertNotNull(info);
        assertEquals("JPEG", info.getFormat());
    }

    public void testConverterWithGif() throws Exception {
        File file = FileUtils.getResourceFileFromContext("cat.gif");

        String outputFilePath = System.getProperty("java.io.tmpdir")
                + "/cat.jpg";

        ImageConverter.convert(file.getAbsolutePath(), outputFilePath);

        ImageInfo info = ImageIdentifier.getInfo(outputFilePath);
        assertNotNull(info);
        assertEquals("JPEG", info.getFormat());
    }

}
