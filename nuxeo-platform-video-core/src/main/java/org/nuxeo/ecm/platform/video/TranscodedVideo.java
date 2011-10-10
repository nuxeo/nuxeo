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
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.3
 */
public final class TranscodedVideo {

    private static final String NAME = "name";

    private static final String CONTENT = "content";

    private static final String METADATA = "metadata";

    private final String name;

    private final VideoMetadata metadata;

    private final Blob blob;

    private final int position;

    public static TranscodedVideo fromMap(Map<String, Serializable> map,
            int position) {
        return new TranscodedVideo(map, position);
    }

    public static TranscodedVideo fromBlobAndMetadata(String name, Blob blob,
            VideoMetadata videoMetadata) {
        return new TranscodedVideo(name, blob, videoMetadata);
    }

    private TranscodedVideo(Map<String, Serializable> map, int position) {
        this.position = position;
        name = (String) map.get(NAME);
        blob = (Blob) map.get(CONTENT);
        Map<String, Serializable> metadata = (Map<String, Serializable>) map.get(METADATA);
        this.metadata = VideoMetadata.fromMap(metadata);
    }

    private TranscodedVideo(String name, Blob blob, VideoMetadata videoMetadata) {
        this.name = name;
        this.blob = blob;
        metadata = videoMetadata;
        position = -1;
    }

    public String getName() {
        return name;
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
        return metadata.getFormat();
    }

    public double getFrameRate() {
        return metadata.getFrameRate();
    }

    public Blob getVideoBlob() {
        return blob;
    }

    public String getBlobPropertyName() {
        if (position == -1) {
            throw new IllegalStateException(
                    "This transcoded video is not yet persisted, cannot generate property name.");
        }
        return "vid:transcodedVideos/" + position + "/content";
    }

    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put(NAME, name);
        map.put(CONTENT, (Serializable) blob);
        map.put(METADATA, (Serializable) metadata.toMap());
        return map;
    }

}
