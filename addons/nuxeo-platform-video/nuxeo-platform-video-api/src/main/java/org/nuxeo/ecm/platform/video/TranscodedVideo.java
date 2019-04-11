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

import org.nuxeo.ecm.core.api.Blob;

/**
 * Object wrapping a transcoded video and related {@link VideoInfo}.
 * <p>
 * The {@code TranscodedVideo} is identified by its name and a position, if any, in the list of {@code TranscodedVideo}s
 * for a given @{link VideoDocument}.
 * <p>
 * If this {@code TranscodedVideo} is not part
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public final class TranscodedVideo extends Video {

    private static final String NAME = "name";

    private static final String CONTENT = "content";

    private static final String INFO = "info";

    private final String name;

    private final int position;

    /**
     * Build a {@code TranscodedVideo} from a {@code Map} of attributes and a {@code position}
     */
    public static TranscodedVideo fromMapAndPosition(Map<String, Serializable> map, int position) {
        Blob blob = (Blob) map.get(CONTENT);
        @SuppressWarnings("unchecked")
        Map<String, Serializable> info = (Map<String, Serializable>) map.get(INFO);
        VideoInfo videoInfo = VideoInfo.fromMap(info);
        String name = (String) map.get(NAME);
        return new TranscodedVideo(blob, videoInfo, name, position);
    }

    /**
     * Build a {@code TranscodedVideo} from a {@code name}, video {@code blob} and related {@code videoInfo}.
     */
    public static TranscodedVideo fromBlobAndInfo(String name, Blob blob, VideoInfo videoInfo) {
        return new TranscodedVideo(blob, videoInfo, name, -1);
    }

    private TranscodedVideo(Blob blob, VideoInfo videoInfo, String name, int position) {
        super(blob, videoInfo);
        this.name = name;
        this.position = position;
    }

    /**
     * Returns the name of this {@code TranscodedVideo}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the video {@code Blob} property name of this {@code TranscodedVideo}.
     */
    public String getBlobPropertyName() {
        if (position == -1) {
            throw new IllegalStateException(
                    "This transcoded video is not yet persisted, cannot generate property name.");
        }
        return "vid:transcodedVideos/" + position + "/content";
    }

    /**
     * Returns a {@code Map} of attributes for this {@code TranscodedVideo}.
     * <p>
     * Used when saving this {@code TranscodedVideo} to a {@code DocumentModel} property.
     */
    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<>();
        map.put(NAME, name);
        map.put(CONTENT, (Serializable) blob);
        map.put(INFO, (Serializable) videoInfo.toMap());
        return map;
    }

}
