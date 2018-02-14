/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.video.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.video.convert")
@Deploy("org.nuxeo.ecm.platform.video.core")
public class TestVideoService {

    public static final String DELTA_MP4 = "DELTA.mp4";

    @Inject
    protected CoreSession session;

    @Inject
    protected VideoService videoService;

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    @Before
    public void setUp() throws Exception {
        eventServiceAdmin.setListenerEnabledFlag("videoAutomaticConversions", false);
        eventServiceAdmin.setListenerEnabledFlag("sql-storage-binary-text", false);
    }

    @Test
    public void testVideoConversion() throws IOException {
        Video video = getTestVideo();
        TranscodedVideo transcodedVideo = videoService.convert(video, "WebM 480p");
        assertNotNull(transcodedVideo);
        assertEquals("video/webm", transcodedVideo.getBlob().getMimeType());
        assertTrue(transcodedVideo.getBlob().getFilename().endsWith("webm"));
        assertEquals("WebM 480p", transcodedVideo.getName());
        assertEquals(8.38, transcodedVideo.getDuration(), 0.1);
        assertEquals(768, transcodedVideo.getWidth());
        assertEquals(480, transcodedVideo.getHeight());
        assertEquals(23.98, transcodedVideo.getFrameRate(), 0.1);
        assertEquals("matroska,webm", transcodedVideo.getFormat());
    }

    protected static Video getTestVideo() throws IOException {
        try (InputStream is = TestVideoService.class.getResourceAsStream("/" + DELTA_MP4)) {
            assertNotNull(String.format("Failed to load resource: " + DELTA_MP4), is);
            Blob blob = Blobs.createBlob(is, "video/mp4");
            blob.setFilename(FilenameUtils.getName(DELTA_MP4));
            VideoInfo videoInfo = VideoInfo.fromFFmpegOutput(getTestVideoInfoOutput());
            return Video.fromBlobAndInfo(blob, videoInfo);
        }
    }

    protected static List<String> getTestVideoInfoOutput() {
        List<String> output = new ArrayList<String>();
        output.add("Input #0, mov,mp4,m4a,3gp,3g2,mj2, from 'DELTA.mp4':");
        output.add("Duration: 00:00:08.38, start: 0.000000, bitrate: 930 kb/s");
        output.add("Stream #0.0(und): Video: h264 (High), yuv420p, 768x480 [PAR 1:1 DAR 8:5], 927 kb/s, 23.98 fps, 23.98 tbr, 10k tbn, 47.96 tbc");
        return output;
    }

    @Test
    // temporary ignore
    @Ignore
    public void testAsynchronousVideoConversion() throws IOException, InterruptedException {
        Video video = getTestVideo();
        DocumentModel doc = session.createDocumentModel("/", "video", VIDEO_TYPE);
        doc.setPropertyValue("file:content", (Serializable) video.getBlob());
        doc = session.createDocument(doc);
        session.save();

        videoService.launchConversion(doc, "WebM 480p");

        while (videoService.getProgressStatus(doc.getRepositoryName(), doc.getId(), "WebM 480p") != null) {
            // wait for the conversion to complete
            Thread.sleep(2000);
        }

        session.save();
        doc = session.getDocument(doc.getRef());
        assertNotNull(doc);
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue("vid:transcodedVideos");
        assertNotNull(transcodedVideos);
        assertEquals(1, transcodedVideos.size());
    }

}
