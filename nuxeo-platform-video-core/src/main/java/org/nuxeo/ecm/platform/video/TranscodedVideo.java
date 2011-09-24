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

import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.3
 */
public final class TranscodedVideo {

    private final VideoMetadata metadata;

    private final Blob blob;

    private final int position;

    public static TranscodedVideo fromMap(Map<String, Serializable> map,
            int position) {
        return new TranscodedVideo(map, position);
    }

    private TranscodedVideo(Map<String, Serializable> map, int position) {
        this.position = position;
        blob = (Blob) map.get("content");
        Map<String, Serializable> metadata = (Map<String, Serializable>) map.get("metadata");
        this.metadata = VideoMetadata.fromMap(metadata);
    }

    public String getName() {
        return metadata.getName();
    }

    public double getDuration() {
        return metadata.getDuration();
    }

    public long getWidth() {
        return metadata.getWidth();
    }

    public long getHeight() {
        return metadata.getHeight();
    }

    public String getContainer() {
        return metadata.getContainer();
    }

    public String getAudioCodec() {
        return metadata.getAudioCodec();
    }

    public String getVideoCodec() {
        return metadata.getVideoCodec();
    }

    public double getFrameRate() {
        return metadata.getFrameRate();
    }

    public Blob getVideoBlob() {
        return blob;
    }

    public String getBlobPropertyName() {
        return "vid:transcodedVideos/" + position + "/content";
    }

}
