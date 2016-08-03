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
    protected Float zenith = 0f;

    @XNode("@azimuth")
    protected Float azimuth = 0f;

    @XNode("@enabled")
    protected Boolean enabled = true;

    @XNode("@rendition")
    protected Boolean rendition;

    @XNode("@renditionVisible")
    protected Boolean renditionVisible;

    public RenderView(RenderView aRenderView) {
        name = aRenderView.getName();
        zenith = aRenderView.getZenith();
        azimuth = aRenderView.getAzimuth();
        enabled = aRenderView.isEnabled();
        rendition = aRenderView.isRendition();
        renditionVisible = aRenderView.isRenditionVisible();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getZenith() {
        return zenith;
    }

    public void setZenith(Float zenith) {
        this.zenith = zenith;
    }

    public Float getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(Float azimuth) {
        this.azimuth = azimuth;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isRendition() {
        return rendition;
    }

    public void setRendition(Boolean rendition) {
        this.rendition = rendition;
    }

    public Boolean isRenditionVisible() {
        return renditionVisible;
    }

    public void setRenditionVisible(Boolean renditionVisible) {
        this.renditionVisible = renditionVisible;
    }

    public void merge(RenderView src) {
        enabled = src.isEnabled();
    }

    @Override
    public int compareTo(RenderView o) {
        return name.compareTo(o.getName());
    }
}
