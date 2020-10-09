/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.video.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.video.VideoFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.3
 */
@RunWith(FeaturesRunner.class)
@Features(VideoFeature.class)
public class VideoConvertersTest {

    // http://www.elephantsdream.org/
    public static final String ELEPHANTS_DREAM = "elephantsdream-160-mpeg4-su-ac3.avi";

    protected static BlobHolder getTestBlobFromPath() throws IOException {
        String path = "/" + ELEPHANTS_DREAM;
        try (InputStream is = VideoConvertersTest.class.getResourceAsStream(path)) {
            assertNotNull(String.format("Failed to load resource: %s", path), is);
            return new SimpleBlobHolder(Blobs.createBlob(is, "video/mp4"));
        }
    }

    protected BlobHolder applyConverter(String converter, Double position, Double duration) throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);
        assertTrue(cs.getRegistredConverters().contains(converter));
        BlobHolder in = getTestBlobFromPath();
        Map<String, Serializable> params = new HashMap<>();
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
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        assertNotNull(cles);
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot-resize");
        Assume.assumeTrue("ffmpeg-screenshot-resize is not available, skipping test", ca.isAvailable());
        BlobHolder result = applyConverter(Constants.STORYBOARD_CONVERTER, null, 653.53);
        List<Blob> blobs = result.getBlobs();
        assertEquals(9, blobs.size());
        assertEquals("0.00-seconds.jpeg", blobs.get(0).getFilename());
        assertEquals("72.61-seconds.jpeg", blobs.get(1).getFilename());
        assertEquals("580.92-seconds.jpeg", blobs.get(8).getFilename());
    }

    @Test
    public void testScreenshotConverter() throws Exception {
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        assertNotNull(cles);
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot");
        Assume.assumeTrue("ffmpeg-screenshot is not available, skipping test", ca.isAvailable());
        BlobHolder result = applyConverter(Constants.SCREENSHOT_CONVERTER, null, null);
        List<Blob> blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertEquals("video-screenshot-00000.000.jpeg", blobs.get(0).getFilename());

        result = applyConverter(Constants.SCREENSHOT_CONVERTER, 10.0, null);
        blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertEquals("video-screenshot-00010.000.jpeg", blobs.get(0).getFilename());
    }

}
