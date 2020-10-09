/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;

/**
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(VideoFeature.class)
public class VideoConversionTest {

    public static final Log log = LogFactory.getLog(VideoConversionTest.class);

    public static final String DELTA_MP4 = "DELTA.mp4";

    public static final String DELTA_OGV = "DELTA.ogv";

    @Inject
    protected ConversionService cs;

    @Inject
    protected CommandLineExecutorService cles;

    protected static BlobHolder getBlobFromPath(String path, String mimeType) throws IOException {
        try (InputStream is = VideoConvertersTest.class.getResourceAsStream("/" + path)) {
            assertNotNull(String.format("Failed to load resource: " + path), is);
            Blob blob = Blobs.createBlob(is, mimeType);
            blob.setFilename(FilenameUtils.getName(path));
            return new SimpleBlobHolder(blob);
        }
    }

    protected BlobHolder applyConverter(String converter, String fileName, String mimeType, long newHeight)
            throws Exception {
        assertNotNull(cs.getRegistredConverters().contains(converter));
        BlobHolder in = getBlobFromPath(fileName, mimeType);
        Map<String, Serializable> parameters = new HashMap<>();
        Map<String, Serializable> videoInfo = new HashMap<>();
        videoInfo.put(VideoInfo.WIDTH, 768L);
        videoInfo.put(VideoInfo.HEIGHT, 480L);
        parameters.put("videoInfo", VideoInfo.fromMap(videoInfo));
        parameters.put("height", newHeight);
        BlobHolder result = cs.convert(converter, in, parameters);
        assertNotNull(result);
        return result;
    }

    @Test
    public void testWebMConversion() throws Exception {
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-towebm");
        Assume.assumeTrue("ffmpeg-towebm is not available, skipping test", ca.isAvailable());

        BlobHolder result = applyConverter(Constants.TO_WEBM_CONVERTER, DELTA_MP4, "video/x-msvideo", 480);
        List<Blob> blobs = result.getBlobs();
        assertFalse(blobs.isEmpty());
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("DELTA.webm", blob.getFilename());
        assertEquals("video/webm", blob.getMimeType());
    }

    @Test
    public void testOggConversion() throws Exception {
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-toogg");
        Assume.assumeTrue("ffmpeg-toogg is not available, skipping test", ca.isAvailable());

        BlobHolder result = applyConverter(Constants.TO_OGG_CONVERTER, DELTA_MP4, "video/x-msvideo", 480);
        List<Blob> blobs = result.getBlobs();
        assertFalse(blobs.isEmpty());
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("DELTA.ogg", blob.getFilename());
        assertEquals("video/ogg", blob.getMimeType());
    }

    @Test
    public void testMP4Conversion() throws Exception {
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-tomp4");
        Assume.assumeTrue("ffmpeg-tomp4 is not available, skipping test", ca.isAvailable());

        BlobHolder result = applyConverter(Constants.TO_MP4_CONVERTER, DELTA_OGV, "video/ogg", 480);
        List<Blob> blobs = result.getBlobs();
        assertFalse(blobs.isEmpty());
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("DELTA.mp4", blob.getFilename());
        assertEquals("video/mp4", blob.getMimeType());
    }

    @Test
    public void testAVIConversion() throws Exception {
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-toavi");
        Assume.assumeTrue("ffmpeg-toavi is not available, skipping test", ca.isAvailable());

        BlobHolder result = applyConverter(Constants.TO_AVI_CONVERTER, DELTA_MP4, "video/x-msvideo", 120);
        List<Blob> blobs = result.getBlobs();
        assertFalse(blobs.isEmpty());
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("DELTA.avi", blob.getFilename());
        assertEquals("video/x-msvideo", blob.getMimeType());
    }
}
