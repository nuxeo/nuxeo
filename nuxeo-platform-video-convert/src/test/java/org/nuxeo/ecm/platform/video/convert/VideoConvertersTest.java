/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.video.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class VideoConvertersTest extends NXRuntimeTestCase {

    public static final Log log = LogFactory.getLog(VideoConvertersTest.class);

    // http://www.elephantsdream.org/
    public static final String ELEPHANTS_DREAM = "elephantsdream-160-mpeg4-su-ac3.avi";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.video.convert");
    }

    protected static BlobHolder getBlobFromPath(String path) throws IOException {
        InputStream is = VideoConvertersTest.class.getResourceAsStream("/"
                + path);
        assertNotNull(String.format("Failed to load resource: " + path), is);
        return new SimpleBlobHolder(
                StreamingBlob.createFromStream(is, path).persist());
    }

    protected BlobHolder applyConverter(String converter, String fileName,
            Double position, Double duration) throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);
        assertNotNull(cs.getRegistredConverters().contains(converter));
        BlobHolder in = getBlobFromPath(fileName);
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        if (position != null) {
            params.put("position", position);
        }
        if (duration != null) {
            params.put("duration", duration);
        }
        BlobHolder result = cs.convert(converter, in, params);
        assertNotNull(result);
        return result;
    }

    @Test
    public void testStoryboardConverter() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot-resize");
        if (!ca.isAvailable()) {
            log.warn("ffmpeg is not avalaible, skipping test");
            return;
        }
        BlobHolder result = applyConverter(Constants.STORYBOARD_CONVERTER,
                ELEPHANTS_DREAM, null, 653.53);
        List<Blob> blobs = result.getBlobs();
        assertEquals(9, blobs.size());
        assertEquals("00000.000-seconds.jpeg", blobs.get(0).getFilename());
        assertEquals("00072.000-seconds.jpeg", blobs.get(1).getFilename());
        assertEquals("00580.000-seconds.jpeg", blobs.get(8).getFilename());
    }

    @Test
    @Ignore
    public void testScreenshotConverter() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot");
        if (!ca.isAvailable()) {
            log.warn("ffmpeg is not avalaible, skipping test");
            return;
        }
        BlobHolder result = applyConverter(Constants.SCREENSHOT_CONVERTER,
                ELEPHANTS_DREAM, null, null);
        List<Blob> blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertEquals("video-screenshot-00000.000.jpeg",
                blobs.get(0).getFilename());

        result = applyConverter(Constants.SCREENSHOT_CONVERTER,
                ELEPHANTS_DREAM, 10.0, null);
        blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertEquals("video-screenshot-00010.000.jpeg",
                blobs.get(0).getFilename());
    }

}
