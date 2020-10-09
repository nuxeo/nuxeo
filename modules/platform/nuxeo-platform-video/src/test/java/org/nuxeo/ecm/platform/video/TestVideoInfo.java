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

package org.nuxeo.ecm.platform.video;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class TestVideoInfo {

    @Test
    public void testFFmpegOutputParsing() throws IOException {
        File file = FileUtils.getResourceFileFromContext("ffmpeg/ffmpeg_output_1.txt");
        List<String> lines = org.apache.commons.io.FileUtils.readLines(file, "UTF-8");
        VideoInfo videoInfo = VideoInfo.fromFFmpegOutput(lines);
        assertCommonVideoInfo(videoInfo);
        assertEquals(1280, videoInfo.getWidth());
        assertEquals(720, videoInfo.getHeight());
    }

    /**
     * NXP-28856
     */
    @Test
    public void testFFmpegOutputRotateParsing() throws IOException {
        File file = FileUtils.getResourceFileFromContext("ffmpeg/ffmpeg_output_portrait_1.txt");
        List<String> lines = org.apache.commons.io.FileUtils.readLines(file, "UTF-8");
        VideoInfo videoInfo = VideoInfo.fromFFmpegOutput(lines);
        assertCommonVideoInfo(videoInfo);
        assertEquals(720, videoInfo.getWidth());
        assertEquals(1280, videoInfo.getHeight());

        file = FileUtils.getResourceFileFromContext("ffmpeg/ffmpeg_output_portrait_2.txt");
        lines = org.apache.commons.io.FileUtils.readLines(file, "UTF-8");
        videoInfo = VideoInfo.fromFFmpegOutput(lines);
        assertCommonVideoInfo(videoInfo);
        assertEquals(720, videoInfo.getWidth());
        assertEquals(1280, videoInfo.getHeight());

        file = FileUtils.getResourceFileFromContext("ffmpeg/ffmpeg_output_landscape_1.txt");
        lines = org.apache.commons.io.FileUtils.readLines(file, "UTF-8");
        videoInfo = VideoInfo.fromFFmpegOutput(lines);
        assertCommonVideoInfo(videoInfo);
        assertEquals(1280, videoInfo.getWidth());
        assertEquals(720, videoInfo.getHeight());

        file = FileUtils.getResourceFileFromContext("ffmpeg/ffmpeg_output_landscape_2.txt");
        lines = org.apache.commons.io.FileUtils.readLines(file, "UTF-8");
        videoInfo = VideoInfo.fromFFmpegOutput(lines);
        assertCommonVideoInfo(videoInfo);
        assertEquals(1280, videoInfo.getWidth());
        assertEquals(720, videoInfo.getHeight());

        file = FileUtils.getResourceFileFromContext("ffmpeg/ffmpeg_output_wrong_rotate_1.txt");
        lines = org.apache.commons.io.FileUtils.readLines(file, "UTF-8");
        videoInfo = VideoInfo.fromFFmpegOutput(lines);
        assertCommonVideoInfo(videoInfo);
        assertEquals(1280, videoInfo.getWidth());
        assertEquals(720, videoInfo.getHeight());

        file = FileUtils.getResourceFileFromContext("ffmpeg/ffmpeg_output_wrong_rotate_2.txt");
        lines = org.apache.commons.io.FileUtils.readLines(file, "UTF-8");
        videoInfo = VideoInfo.fromFFmpegOutput(lines);
        assertCommonVideoInfo(videoInfo);
        assertEquals(1280, videoInfo.getWidth());
        assertEquals(720, videoInfo.getHeight());
    }

    protected void assertCommonVideoInfo(VideoInfo videoInfo) {
        assertNotNull(videoInfo);

        assertEquals("matroska,webm", videoInfo.getFormat());
        assertEquals(21 * 60 + 9 + 2d / 100, videoInfo.getDuration(), 0.1);
        assertEquals(23.98, videoInfo.getFrameRate(), 0.1);

        List<Stream> streams = videoInfo.getStreams();
        assertEquals(2, streams.size());
        Stream stream = streams.get(0);
        assertEquals(Stream.VIDEO_TYPE, stream.getType());
        assertEquals("h264 (High)", stream.getCodec());
        assertEquals(
                "Stream #0.0(eng): Video: h264 (High), yuv420p, 1280x720, PAR 1:1 DAR 16:9, 23.98 fps, 23.98 tbr, 1k tbn, 47.95 tbc",
                stream.getStreamInfo());

        stream = streams.get(1);
        assertEquals(Stream.AUDIO_TYPE, stream.getType());
        assertEquals("ac3", stream.getCodec());
        assertEquals("Stream #0.1: Audio: ac3, 48000 Hz, 5.1, s16, 448 kb/s (default)", stream.getStreamInfo());
    }

}
