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
package org.nuxeo.ecm.platform.imaging.transform;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.imaging.api.ImagingService;
import org.nuxeo.ecm.platform.imaging.transform.api.ImagingTransformPlugin;
import org.nuxeo.ecm.platform.transform.NXTransform;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.service.TransformService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Max Stepanov
 *
 */
public class TestImagingTransformPlugin extends NXRuntimeTestCase {

    private Plugin plugin;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("nximaging-bundle.xml");
        deployContrib("nxmimetype-bundle.xml");
        deployContrib("nxtransform-bundle.xml");
        deployContrib("nxtransform-plugins-bundle.xml");

        TransformService service = NXTransform.getTransformService();
        assertNotNull(service);
        plugin = service.getPluginByName("imaging");
        assertNotNull(plugin);

        assertNotNull(Framework.getService(ImagingService.class));
    }

    @Override
    protected void tearDown() throws Exception {
        plugin = null;
        super.tearDown();
    }

    private static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return file;
    }

    public void testResize() throws Exception {
        String path = "test-data/sample.jpeg";
        int resizeWidth = 120;
        int resizeHeight = 120;

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(ImagingTransformPlugin.OPTION_OPERATION,
                ImagingTransformPlugin.OPERATION_RESIZE);
        options.put(ImagingTransformPlugin.OPTION_RESIZE_WIDTH,
                Integer.toString(resizeWidth));
        options.put(ImagingTransformPlugin.OPTION_RESIZE_HEIGHT,
                Integer.toString(resizeHeight));

        FileBlob blob = new FileBlob(getFileFromPath(path));
        List<TransformDocument> results = plugin.transform(options,
                new TransformDocumentImpl(blob, "image/jpeg"));
        assertEquals(1, results.size());
        InputStream result = results.get(0).getBlob().getStream();
        assertNotNull(result);

        BufferedImage image = ImageIO.read(new FileInputStream(
                FileUtils.getResourceFileFromContext(path)));
        assertNotNull("Original image is null", image);
        int width = image.getWidth();
        int height = image.getHeight();
        assertTrue("Original image size != (0,0)", width > 0 && height > 0);

        if (width * resizeHeight >= height * resizeWidth) {
            /* scale by width */
            resizeHeight = (int) (height * ((double) resizeWidth / (double) width));
        } else {
            /* scale by height */
            resizeWidth = (int) (width * ((double) resizeHeight / (double) height));
        }

        image = ImageIO.read(result);
        assertNotNull("Resized image is null", image);
        assertEquals("Resized image width", resizeWidth, image.getWidth());
        assertEquals("Resized image height", resizeHeight, image.getHeight());
    }

    public void testRotate() throws Exception {
        String path = "test-data/sample.jpeg";

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(ImagingTransformPlugin.OPTION_OPERATION,
                ImagingTransformPlugin.OPERATION_ROTATE);
        options.put(ImagingTransformPlugin.OPTION_ROTATE_ANGLE, "90");

        FileBlob blob = new FileBlob(getFileFromPath(path));
        List<TransformDocument> results = plugin.transform(options,
                new TransformDocumentImpl(blob, "image/jpeg"));
        assertEquals(1, results.size());
        InputStream result = results.get(0).getBlob().getStream();
        assertNotNull(result);

        BufferedImage image = ImageIO.read(new FileInputStream(
                FileUtils.getResourceFileFromContext(path)));
        assertNotNull("Original image is null", image);
        int width = image.getWidth();
        int height = image.getHeight();
        assertTrue("Original image size != (0,0)", width > 0 && height > 0);

        image = ImageIO.read(result);
        assertNotNull("Rotated image is null", image);
        assertEquals("Ratated image width", height, image.getWidth());
        assertEquals("Rotated image height", width, image.getHeight());
    }

    public void testCrop(String[] Args) throws Exception {
        String path = "test-data/sample.jpeg";
        String path2 = "test-data/sample2.jpeg";
        File file = File.createTempFile("test-data/sample2", "jpg");

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(ImagingTransformPlugin.OPTION_OPERATION,
                ImagingTransformPlugin.OPERATION_CROP);

        options.put(ImagingTransformPlugin.OPTION_CROP_X, "5");
        options.put(ImagingTransformPlugin.OPTION_CROP_Y, "25");
        options.put(ImagingTransformPlugin.OPTION_RESIZE_HEIGHT, "10");
        options.put(ImagingTransformPlugin.OPTION_RESIZE_WIDTH, "10");

        FileBlob blob = new FileBlob(getFileFromPath(path));
        List<TransformDocument> results = plugin.transform(options,
                new TransformDocumentImpl(blob, "image/jpeg"));
        assertEquals(1, results.size());
        InputStream result = results.get(0).getBlob().getStream();
        assertNotNull(result);

        BufferedImage image = ImageIO.read(new FileInputStream(
                FileUtils.getResourceFileFromContext(path)));
        assertNotNull("Original image is null", image);
        int width = image.getWidth();
        int height = image.getHeight();
        assertTrue("Original image size != (0,0)", width > 0 && height > 0);
        ImageIcon im= new ImageIcon(image);
        image = ImageIO.read(result);



        ImageIcon im2= new ImageIcon(image);
        FileBlob blob2 = new FileBlob(getFileFromPath(path2));
        blob2.transferTo(file);

        assertNotSame("Ratated image width", height, image.getWidth());
        assertNotSame("Rotated image height", width, image.getHeight());
    }

}
