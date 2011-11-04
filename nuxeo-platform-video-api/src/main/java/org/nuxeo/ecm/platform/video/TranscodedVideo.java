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
 * @since 5.5
 */
public final class TranscodedVideo extends Video {

    private static final String NAME = "name";

    private static final String CONTENT = "content";

    private static final String INFO = "info";

    private final String name;

    private final int position;

    public static TranscodedVideo fromMapAndPosition(
            Map<String, Serializable> map, int position) {
        Blob blob = (Blob) map.get(CONTENT);
        Map<String, Serializable> info = (Map<String, Serializable>) map.get(INFO);
        VideoInfo videoInfo = VideoInfo.fromMap(info);
        String name = (String) map.get(NAME);
        return new TranscodedVideo(blob, videoInfo, name, position);
    }

    public static TranscodedVideo fromBlobAndInfo(String name, Blob blob,
            VideoInfo videoInfo) {
        return new TranscodedVideo(blob, videoInfo, name, -1);
    }

    private TranscodedVideo(Blob blob, VideoInfo videoInfo, String name,
            int position) {
        super(blob, videoInfo);
        this.name = name;
        this.position = position;
    }

    public String getName() {
        return name;
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
        map.put(INFO, (Serializable) videoInfo.toMap());
        return map;
    }

}
