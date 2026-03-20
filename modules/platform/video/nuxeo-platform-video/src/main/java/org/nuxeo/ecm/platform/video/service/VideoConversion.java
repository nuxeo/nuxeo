/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.video.service;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Object representing a registered video conversion on the {@link VideoService} .
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("videoConversion")
public class VideoConversion implements Cloneable, Descriptor {

    @XNode("@name")
    private String name;

    @XNode("@converter")
    private String converter;

    @XNode("@height")
    private long height;

    @XNode("@enabled")
    private boolean enabled = true;

    /**
     * @since 7.2
     */
    @XNode("@rendition")
    private Boolean rendition;

    /**
     * @since 7.2
     */
    @XNode("@renditionVisible")
    private Boolean renditionVisible;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getConverter() {
        return converter;
    }

    public long getHeight() {
        return height;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setConverter(String converter) {
        this.converter = converter;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public boolean isRenditionVisible() {
        return renditionVisible == null || renditionVisible;
    }

    /**
     * @deprecated since 2025.0 seems unused
     */
    @Deprecated(since = "2025.0")
    public boolean isRenditionVisibleSet() {
        return renditionVisible != null;
    }

    public boolean isRendition() {
        return rendition == null || rendition;
    }

    /**
     * @deprecated since 2025.0 seems unused
     */
    @Deprecated(since = "2025.0")
    public boolean isRenditionSet() {
        return rendition != null;
    }

    /**
     * @deprecated since 2025.0 seems unused
     */
    @Deprecated(since = "2025.0")
    public void setRendition(Boolean rendition) {
        this.rendition = rendition;
    }

    /**
     * @deprecated since 2025.0 seems unused
     */
    @Deprecated(since = "2025.0")
    public void setRenditionVisible(Boolean renditionVisible) {
        this.renditionVisible = renditionVisible;
    }

    @Override
    public VideoConversion clone() throws CloneNotSupportedException {
        return (VideoConversion) super.clone();
    }

    @Override
    public VideoConversion merge(Descriptor o) {
        var other = (VideoConversion) o;
        var merged = new VideoConversion();
        merged.name = name; // we merged based on name, so no need for merging name
        merged.converter = getIfNull(other.converter, converter);
        merged.height = getIfNull(other.height, height);
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.rendition = getIfNull(other.rendition, rendition);
        merged.renditionVisible = getIfNull(other.renditionVisible, renditionVisible);

        return merged;
    }
}
