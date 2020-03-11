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

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Object wrapping a video {@code Blob} and related {@link VideoInfo}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class Video {

    protected final VideoInfo videoInfo;

    protected final Blob blob;

    /**
     * Build a {@code Video} from a video {@code blob} and the related {@code videoInfo}.
     */
    public static Video fromBlobAndInfo(Blob blob, VideoInfo videoInfo) {
        return new Video(blob, videoInfo);
    }

    protected Video(Blob blob, VideoInfo videoInfo) {
        this.blob = blob;
        this.videoInfo = videoInfo;
    }

    /**
     * Returns the {@link VideoInfo} for this {@code Video}.
     */
    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    /**
     * Returns the duration of this {@code Video}.
     */
    public double getDuration() {
        return videoInfo.getDuration();
    }

    /**
     * Returns the width of this {@code Video}.
     */
    public long getWidth() {
        return videoInfo.getWidth();
    }

    /**
     * Returns the height of this {@code Video}.
     */
    public long getHeight() {
        return videoInfo.getHeight();
    }

    /**
     * Returns the format of this {@code Video}.
     */
    public String getFormat() {
        return videoInfo.getFormat();
    }

    /**
     * Returns the frame rate of this {@code Video}.
     */
    public double getFrameRate() {
        return videoInfo.getFrameRate();
    }

    /**
     * Returns all the {@link Stream}s of this {@code Video}.
     */
    public List<Stream> getStreams() {
        return videoInfo.getStreams();
    }

    /**
     * Returns the video {@code Blob}.
     */
    public Blob getBlob() {
        return blob;
    }

}
