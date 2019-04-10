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
     * Build a {@code Video} from a video {@code blob} and the related
     * {@code videoInfo}.
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
