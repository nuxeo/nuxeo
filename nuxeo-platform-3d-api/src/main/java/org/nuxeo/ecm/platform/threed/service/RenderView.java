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
package org.nuxeo.ecm.platform.threed.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Object representing a registered render view conversion on the {@link ThreeDService}. An {@code RenderView}
 * references the spherical coordinates and if it should be a Rendition.
 *
 * @since 8.4
 */
@XObject("renderView")
public class RenderView implements Comparable<RenderView> {

    @XNode("@name")
    protected String name;

    @XNode("@zenith")
    protected Integer zenith;

    @XNode("@azimuth")
    protected Integer azimuth;

    @XNode("@width")
    protected Integer width;

    @XNode("@height")
    protected Integer height;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@rendition")
    protected Boolean rendition;

    @XNode("@renditionVisible")
    protected Boolean renditionVisible;

    public RenderView(RenderView other) {
        name = other.name;
        zenith = other.zenith;
        azimuth = other.azimuth;
        width = other.width;
        height = other.height;
        enabled = other.enabled;
        rendition = other.rendition;
        renditionVisible = other.renditionVisible;
    }

    public RenderView() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getZenith() {
        return zenith;
    }

    public void setZenith(Integer zenith) {
        this.zenith = zenith;
    }

    public Integer getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(Integer azimuth) {
        this.azimuth = azimuth;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public boolean isEnabled() {
        return (enabled == null) || enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRendition() {
        return (rendition == null) || rendition;
    }

    public void setRendition(boolean rendition) {
        this.rendition = rendition;
    }

    public boolean isRenditionVisible() {
        return (renditionVisible == null) || renditionVisible;
    }

    public void setRenditionVisible(boolean renditionVisible) {
        this.renditionVisible = renditionVisible;
    }

    public void merge(RenderView src) {
        if (src.enabled != null) {
            enabled = src.enabled;
        }
        if (src.rendition != null) {
            rendition = src.rendition;
        }
        if (src.renditionVisible != null) {
            renditionVisible = src.renditionVisible;
        }
        if (src.zenith != null) {
            zenith = src.zenith;
        }
        if (src.azimuth != null) {
            azimuth = src.azimuth;
        }
        if (src.width != null) {
            width = src.width;
        }
        if (src.height != null) {
            height = src.height;
        }
    }

    public String getId() {
        return String.valueOf(name.hashCode() & 0x7fffffff);
    }

    @Override
    public int compareTo(RenderView o) {
        return getId().compareTo(o.getId());
    }
}
