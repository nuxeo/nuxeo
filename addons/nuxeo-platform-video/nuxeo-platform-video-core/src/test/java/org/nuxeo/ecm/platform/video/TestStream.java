package org.nuxeo.ecm.platform.video;

import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.nuxeo.ecm.platform.video.Stream.BIT_RATE_ATTRIBUTE;
import static org.nuxeo.ecm.platform.video.Stream.TYPE_ATTRIBUTE;

class TestStream {

    @Test
    void fromMap() {
        Map<String, Serializable> map = new HashMap<>() {
            {
                put(TYPE_ATTRIBUTE, Stream.VIDEO_TYPE);
                put(Stream.CODEC_ATTRIBUTE, "h264 (High)");
                put(Stream.STREAM_INFO_ATTRIBUTE, "Stream #0.0(eng): Video: h264 (High), yuv420p, 1280x720, PAR 1:1 DAR 16:9, 23.98 fps, 23.98 tbr, 1k tbn, 47.95 tbc");
                put(BIT_RATE_ATTRIBUTE, 9);
            }
        };
        Stream result = Stream.fromMap(map);
    }

    @Test
    void toMap() {
    }
}