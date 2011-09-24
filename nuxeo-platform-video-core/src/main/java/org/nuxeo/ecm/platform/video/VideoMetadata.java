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

import java.io.Serializable;
import java.util.Map;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.3
 */
public final class VideoMetadata {

    private final String name;

    private final double duration;

    private final long width;

    private final long height;

    private final String container;

    private final String videoCodec;

    private final String audioCodec;

    private final double frameRate;

    public static VideoMetadata fromMap(Map<String, Serializable> map) {
        String name = (String) map.get("name");
        if (name == null) {
            name = "";
        }
        Double duration = (Double) map.get("duration");
        if (duration == null) {
            duration = 0d;
        }
        Long width = (Long) map.get("width");
        if (width == null) {
            width = 0l;
        }
        Long height = (Long) map.get("height");
        if (height == null) {
            height = 0l;
        }
        String container = (String) map.get("container");
        if (container == null) {
            container = "";
        }
        String videoCodec = (String) map.get("videoCodec");
        if (videoCodec == null) {
            videoCodec = "";
        }
        String audioCodec = (String) map.get("audioCodec");
        if (audioCodec == null) {
            audioCodec = "";
        }
        Double frameRate = (Double) map.get("frameRate");
        if (frameRate == null) {
            frameRate = 0d;
        }
        return new VideoMetadata(name, duration, width, height, container,
                videoCodec, audioCodec, frameRate);
    }

    public VideoMetadata(String name, double duration, long width, long height,
            String container, String videoCodec, String audioCodec,
            double frameRate) {
        this.name = name;
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.container = container;
        this.videoCodec = videoCodec;
        this.audioCodec = audioCodec;
        this.frameRate = frameRate;
    }

    public String getName() {
        return name;
    }

    public double getDuration() {
        return duration;
    }

    public long getWidth() {
        return width;
    }

    public long getHeight() {
        return height;
    }

    public String getContainer() {
        return container;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public double getFrameRate() {
        return frameRate;
    }
}
