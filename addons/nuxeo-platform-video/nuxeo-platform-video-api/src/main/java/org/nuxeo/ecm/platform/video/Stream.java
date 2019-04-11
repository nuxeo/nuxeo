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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Object representing a Stream of a video.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class Stream {

    public static final String VIDEO_TYPE = "Video";

    public static final String AUDIO_TYPE = "Audio";

    public static final String TYPE_ATTRIBUTE = "type";

    public static final String CODEC_ATTRIBUTE = "codec";

    public static final String STREAM_INFO_ATTRIBUTE = "streamInfo";

    public static final String BIT_RATE_ATTRIBUTE = "bitRate";

    private final Map<String, Serializable> attributes;

    /**
     * Build a {@code Stream} from a {@code Map} of attributes.
     * <p>
     * Used when creating a {@code Stream} from a {@code DocumentModel} property.
     */
    public static Stream fromMap(Map<String, Serializable> m) {
        return new Stream(m);
    }

    private Stream(Map<String, Serializable> m) {
        attributes = new HashMap<>(m);
    }

    /**
     * Returns this {@code Stream}'s type.
     * <p>
     * Can be one of the following:
     * <ul>
     * <li>Video</li>
     * <li>Audio</li>
     * </ul>
     */
    public String getType() {
        return (String) attributes.get(TYPE_ATTRIBUTE);
    }

    /**
     * Returns this {@code Stream}'s codec.
     */
    public String getCodec() {
        return (String) attributes.get(CODEC_ATTRIBUTE);
    }

    /**
     * Returns this {@code Stream} whole info as returned by FFmpeg.
     */
    public String getStreamInfo() {
        return (String) attributes.get(STREAM_INFO_ATTRIBUTE);
    }

    /**
     * Returns this {@code Stream}'s bit rate.
     */
    public double getBitRate() {
        return (Double) attributes.get(BIT_RATE_ATTRIBUTE);
    }

    /**
     * Returns a {@code Map} of attributes for this {@code Stream}.
     * <p>
     * Used when saving this {@code Stream} to a {@code DocumentModel} property.
     */
    public Map<String, Serializable> toMap() {
        return attributes;
    }

}
