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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.3
 */
@RunWith(FeaturesRunner.class)
@Features(VideoFeature.class)
public class VideoConvertersTest {

    @Inject
    protected ConversionService cs;

    @Inject
    protected CommandLineExecutorService cles;

    // http://www.elephantsdream.org/
    public static final String ELEPHANTS_DREAM = "/elephantsdream-160-mpeg4-su-ac3.avi";

    protected static BlobHolder getTestBlobFromPath() throws IOException {
        try (InputStream is = VideoConvertersTest.class.getResourceAsStream(ELEPHANTS_DREAM)) {
            assertNotNull(String.format("Failed to load resource: %s", ELEPHANTS_DREAM), is);
            return new SimpleBlobHolder(Blobs.createBlob(is, "video/mp4"));
        }
    }

    protected BlobHolder applyConverter(String converter, Map<String, Serializable> params) throws IOException {
        assertTrue(cs.getRegistredConverters().contains(converter));
        BlobHolder in = getTestBlobFromPath();
        BlobHolder result = cs.convert(converter, in, params);
        assertNotNull(result);
        return result;
    }

    @Test
    public void testStoryboardConverter() throws IOException {
        var blobs = extractStoryboard(Constants.STORYBOARD_CONVERTER);
        assertEquals(9, blobs.size());
        assertEquals("0.00-seconds.jpeg", blobs.get(0).getFilename());
        assertEquals("72.61-seconds.jpeg", blobs.get(1).getFilename());
        assertEquals("580.92-seconds.jpeg", blobs.get(8).getFilename());
    }

    protected List<Blob> extractStoryboard(String converter) throws IOException {
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot-resize");
        Assume.assumeTrue("ffmpeg-screenshot-resize is not available, skipping test", ca.isAvailable());
        return applyConverter(converter, Map.of("duration", 653.53)).getBlobs();
    }

    @Test
    public void testScreenshotConverter() throws IOException {
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot");
        Assume.assumeTrue("ffmpeg-screenshot is not available, skipping test", ca.isAvailable());
        BlobHolder result = applyConverter(Constants.SCREENSHOT_CONVERTER, null);
        List<Blob> blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertEquals("video-screenshot-00000.000.jpeg", blobs.get(0).getFilename());

        result = applyConverter(Constants.SCREENSHOT_CONVERTER, Map.of("position", 10.0));
        blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertEquals("video-screenshot-00010.000.jpeg", blobs.get(0).getFilename());
    }

}
