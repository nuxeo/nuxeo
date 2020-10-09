/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 */
package org.nuxeo.ecm.platform.video.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.video.VideoFeature;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(VideoFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestVideoToolsService extends BaseVideoToolsTest {

    @Inject
    protected VideoToolsService service;

    @Test
    public void testExtractClosedCaptions() throws IOException {
        Blob videowithCC = getTestVideo(TEST_VIDEO_WITH_CC);

        Blob closedCaptions = service.extractClosedCaptions(videowithCC, "ttxt", "", "");
        assertNotNull(closedCaptions);

        assertTrue(closedCaptions instanceof FileBlob);
        String cc = fileBlobToString((FileBlob) closedCaptions);
        assertNotNull(cc);
        assertNotEquals("", cc);
    }

    @Test
    public void testExtractClosedCaptionsFromSlice() throws IOException {
        Blob videowithCC = getTestVideo(TEST_VIDEO_WITH_CC);

        Blob closedCaptions = service.extractClosedCaptions(videowithCC, "ttxt", "00:10", "00:20");
        assertNotNull(closedCaptions);

        assertTrue(closedCaptions instanceof FileBlob);
        String cc = fileBlobToString((FileBlob) closedCaptions);
        assertNotNull(cc);
        assertNotEquals("", cc);
    }

    @Test
    public void testConcat() throws IOException {
        Blob oneVideo = getTestVideo(TEST_VIDEO_SMALL);
        Blob otherVideo = getTestVideo(TEST_VIDEO_SMALL);

        BlobList blobs = new BlobList();
        blobs.add(oneVideo);
        blobs.add(otherVideo);
        Blob concatVideo = service.concat(blobs);
        assertNotNull(concatVideo);
        assertTrue(concatVideo.getLength() > 0);

        VideoInfo videoInfo = VideoHelper.getVideoInfo(concatVideo);
        assertEquals(videoInfo.getDuration(), 16.0, 1.0);
    }

    @Test
    public void testSlice() throws IOException {
        Blob video = getTestVideo(TEST_VIDEO_WITH_CC);
        List<Blob> slices = service.slice(video, "00:04", "00:04", false);

        assertNotNull(slices);
        assertNotNull(slices.size() == 1);
        Blob videoSlice = slices.get(0);
        assertTrue(videoSlice.getLength() > 0);

        VideoInfo videoInfo = VideoHelper.getVideoInfo(videoSlice);
        assertEquals(videoInfo.getDuration(), 4.0, 1.0);
    }

    @Test
    public void testSliceEqualParts() throws IOException {
        Blob video = getTestVideo(TEST_VIDEO_WITH_CC);
        List<Blob> slicedVideos = service.slice(video, "", "30", false);

        assertNotNull(slicedVideos);
        assertTrue(slicedVideos.size() == 4);

        for (Blob blob : slicedVideos) {
            VideoInfo videoInfo = VideoHelper.getVideoInfo(blob);
            assertEquals(videoInfo.getDuration(), 25.0, 12.0);
        }
    }

    @Test
    public void testSliceStartAt() throws IOException {
        Blob video = getTestVideo(TEST_VIDEO_WITH_CC);
        List<Blob> slicedVideos = service.slice(video, "00:30", "", false);

        assertNotNull(slicedVideos);
        assertTrue(slicedVideos.size() == 1);

        for (Blob blob : slicedVideos) {
            VideoInfo videoInfo = VideoHelper.getVideoInfo(blob);
            assertEquals(videoInfo.getDuration(), 75.0, 1.0);
        }
    }

    @Test
    public void testAddWatermark() throws IOException {
        Blob video = getTestVideo(TEST_VIDEO_SMALL);
        Blob watermark = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-data/logo.jpeg"));

        Blob videoWithWatermark = service.watermark(video, watermark, "5", "5");
        assertNotNull(videoWithWatermark);
        assertTrue(videoWithWatermark.getLength() > 0);
    }
}
