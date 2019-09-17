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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class TestVideoInfo {

    private static final String[] FFMPEG_OUTPUT = { "Input #0, matroska,webm, from 'test.mkv':",
            "  Duration: 00:21:09.02, start: 0.000000, bitrate: 448 kb/s",
            "    Stream #0.0(eng): Video: h264 (High), yuv420p, 1280x720, PAR 1:1 DAR 16:9, 23.98 fps, 23.98 tbr, 1k tbn, 47.95 tbc",
            "    Stream #0.1: Audio: ac3, 48000 Hz, 5.1, s16, 448 kb/s (default)" };

    private static final String[] FFMPEG_OUTPUT2 = { "Input #0, mov,mp4,m4a,3gp,3g2,mj2, from 'ccdemo.mov':",
            "  Metadata:", "    major_brand     : qt  ", "    minor_version   : 537199360",
            "    compatible_brands: qt  ", "    creation_time   : 2010-06-23T17:51:49.000000Z",
            "    encoder         : CoreMediaAuthoring 700, CoreMedia 484.11, i386",
            "  Duration: 00:01:45.90, start: 0.000000, bitrate: 940 kb/s",
            "    Stream #0:0: Audio: aac (LC) (mp4a / 0x6134706D), 44100 Hz, stereo, fltp, 131 kb/s (default)",
            "    Metadata:", "      creation_time   : 2010-06-23T17:51:49.000000Z",
            "      handler_name    : Apple Sound Media Handler",
            "    Stream #0:1: Video: h264 (Constrained Baseline) (avc1 / 0x31637661), yuv420p(tv, smpte170m/smpte170m/bt709), 480x270 [SAR 1:1 DAR 16:9], 800 kb/s, 23.98 fps, 23.98 tbr, 23976 tbn, 47952 tbc (default)",
            "    Metadata:", "      creation_time   : 2010-06-23T17:51:49.000000Z",
            "      handler_name    : Apple Video Media Handler",
            "    Stream #0:2(eng): Subtitle: eia_608 (c608 / 0x38303663), 480x270, 4 kb/s (default)", "    Metadata:",
            "      creation_time   : 2010-06-23T17:51:49.000000Z",
            "      handler_name    : Apple Closed Caption Media Handler" };

    @Test
    public void testFFmpegOutputParsing() {
        VideoInfo videoInfo = VideoInfo.fromFFmpegOutput(Arrays.asList(FFMPEG_OUTPUT));
        assertNotNull(videoInfo);

        assertEquals("matroska,webm", videoInfo.getFormat());
        assertEquals(21 * 60 + 9 + 2d / 100, videoInfo.getDuration(), 0.1);
        assertEquals(1280, videoInfo.getWidth());
        assertEquals(720, videoInfo.getHeight());
        assertEquals(23.98, videoInfo.getFrameRate(), 0.1);

        List<Stream> streams = videoInfo.getStreams();
        assertEquals(2, streams.size());
        Stream stream = streams.get(0);
        assertEquals(Stream.VIDEO_TYPE, stream.getType());
        assertThat(stream.getBitRate()).isEqualTo(0.0d);
        assertEquals("h264 (High)", stream.getCodec());
        assertEquals(
                "Stream #0.0(eng): Video: h264 (High), yuv420p, 1280x720, PAR 1:1 DAR 16:9, 23.98 fps, 23.98 tbr, 1k tbn, 47.95 tbc",
                stream.getStreamInfo());

        stream = streams.get(1);
        assertEquals(Stream.AUDIO_TYPE, stream.getType());
        assertEquals("ac3", stream.getCodec());
        assertThat(stream.getBitRate()).isEqualTo(448.0d);
        assertEquals("Stream #0.1: Audio: ac3, 48000 Hz, 5.1, s16, 448 kb/s (default)", stream.getStreamInfo());
    }

    @Test
    public void testFFmpegOutput2Parsing() {
        VideoInfo videoInfo = VideoInfo.fromFFmpegOutput(Arrays.asList(FFMPEG_OUTPUT2));
        assertNotNull(videoInfo);
        assertThat(videoInfo).isEqualToComparingFieldByField(VideoInfo.fromMap(videoInfo.toMap()));
    }

    @Test
    public void emptyVideoBeEqualEvenToMapAndBack() {
        assertThat(VideoInfo.EMPTY_INFO).isEqualToComparingFieldByField(
                VideoInfo.fromMap(VideoInfo.EMPTY_INFO.toMap()));
    }

    @Test
    public void shouldEmptyVideoBeEqualBetweenThem() {
        assertThat(VideoInfo.EMPTY_INFO).isEqualTo(VideoInfo.EMPTY_INFO);

    }

}
