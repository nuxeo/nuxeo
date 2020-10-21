/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed;

import org.nuxeo.ecm.core.api.Blob;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Object wraping a 3D render view {@code Blob}, spherical coordinates and title.
 *
 * @since 8.4
 */
public class ThreeDRenderView {

    public static final String TITLE = "title";

    public static final String CONTENT = "content";

    public static final String THUMBNAIL = "thumbnail";

    public static final String AZIMUTH = "azimuth";

    public static final String ZENITH = "zenith";

    protected final Blob content;

    protected final Blob thumbnail;

    protected final String title;

    protected final int azimuth;

    protected final int zenith;

    public ThreeDRenderView(String title, Blob content, Blob thumbnail, int azimuth, int zenith) {
        this.title = title;
        this.content = content;
        this.thumbnail = thumbnail;
        this.azimuth = azimuth;
        this.zenith = zenith;
    }

    public ThreeDRenderView(Map<String, Serializable> map) {
        title = (String) map.get(TITLE);
        content = (Blob) map.get(CONTENT);
        thumbnail = (Blob) map.get(THUMBNAIL);
        Long azimuthLong = (Long) map.get(AZIMUTH);
        azimuth = (azimuthLong != null) ? azimuthLong.intValue() : 0;
        Long zenithLong = (Long) map.get(ZENITH);
        zenith = (zenithLong != null) ? zenithLong.intValue() : 0;
    }

    public Blob getContent() {
        return content;
    }

    public Blob getThumbnail() {
        return thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public int getAzimuth() {
        return azimuth;
    }

    public int getZenith() {
        return zenith;
    }

    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<>();
        map.put(TITLE, title);
        map.put(CONTENT, (Serializable) content);
        map.put(THUMBNAIL, (Serializable) thumbnail);
        map.put(AZIMUTH, azimuth);
        map.put(ZENITH, zenith);
        return map;
    }
}
