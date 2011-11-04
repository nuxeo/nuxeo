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
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class Video {

    protected final VideoInfo videoInfo;

    protected final Blob blob;

    public static Video fromBlobAndInfo(Blob blob, VideoInfo videoInfo) {
        return new Video(blob, videoInfo);
    }

    protected Video(Blob blob, VideoInfo videoInfo) {
        this.blob = blob;
        this.videoInfo = videoInfo;
    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public double getDuration() {
        return videoInfo.getDuration();
    }

    public long getWidth() {
        return videoInfo.getWidth();
    }

    public long getHeight() {
        return videoInfo.getHeight();
    }

    public String getFormat() {
        return videoInfo.getFormat();
    }

    public double getFrameRate() {
        return videoInfo.getFrameRate();
    }

    public List<Stream> getStreams() {
        return videoInfo.getStreams();
    }

    public Blob getBlob() {
        return blob;
    }

}
