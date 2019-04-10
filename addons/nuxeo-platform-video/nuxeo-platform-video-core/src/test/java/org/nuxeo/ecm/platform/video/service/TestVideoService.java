/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.service;

import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class TestVideoService extends SQLRepositoryTestCase {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(TestVideoService.class);

    public static final String DELTA_MP4 = "DELTA.mp4";

    protected VideoService videoService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.convert");
        deployBundle("org.nuxeo.ecm.platform.video.convert");
        deployBundle("org.nuxeo.ecm.platform.video.core");

        openSession();

        EventServiceAdmin eventServiceAdmin = Framework.getLocalService(EventServiceAdmin.class);
        eventServiceAdmin.setListenerEnabledFlag("videoAutomaticConversions",
                false);
        eventServiceAdmin.setListenerEnabledFlag("sql-storage-binary-text",
                false);

        videoService = Framework.getLocalService(VideoService.class);
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testVideoConversion() throws IOException, ClientException {
        Video video = getTestVideo();
        TranscodedVideo transcodedVideo = videoService.convert(video,
                "WebM 480p");
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
        InputStream is = TestVideoService.class.getResourceAsStream("/"
                + DELTA_MP4);
        assertNotNull(String.format("Failed to load resource: " + DELTA_MP4),
                is);
        Blob blob = StreamingBlob.createFromStream(is, "video/mp4");
        blob.setFilename(FilenameUtils.getName(DELTA_MP4));
        blob = blob.persist();
        VideoInfo videoInfo = VideoInfo.fromFFmpegOutput(getTestVideoInfoOutput());
        return Video.fromBlobAndInfo(blob, videoInfo);
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
    public void testAsynchronousVideoConversion() throws IOException,
            ClientException, InterruptedException {
        Video video = getTestVideo();
        DocumentModel doc = session.createDocumentModel("/", "video",
                VIDEO_TYPE);
        doc.setPropertyValue("file:content", (Serializable) video.getBlob());
        doc = session.createDocument(doc);
        session.save();

        videoService.launchConversion(doc, "WebM 480p");

        while (videoService.getProgressStatus(doc.getRepositoryName(),
                doc.getId(), "WebM 480p") != null) {
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
