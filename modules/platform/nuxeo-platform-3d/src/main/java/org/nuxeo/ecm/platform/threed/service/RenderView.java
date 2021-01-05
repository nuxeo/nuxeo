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
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Object representing a registered render view conversion on the {@link ThreeDService}. An {@code RenderView}
 * references the spherical coordinates and if it should be a Rendition.
 *
 * @since 8.4
 */
@XObject("renderView")
@XRegistry(enable = false)
public class RenderView implements Comparable<RenderView> {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@zenith")
    protected Integer zenith;

    @XNode("@azimuth")
    protected Integer azimuth;

    @XNode("@width")
    protected Integer width;

    @XNode("@height")
    protected Integer height;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    protected boolean enabled;

    @XNode("@rendition")
    protected Boolean rendition;

    @XNode("@renditionVisible")
    protected Boolean renditionVisible;

    public String getName() {
        return name;
    }

    public Integer getZenith() {
        return zenith;
    }

    public Integer getAzimuth() {
        return azimuth;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRendition() {
        return (rendition == null) || rendition;
    }

    public boolean isRenditionVisible() {
        return (renditionVisible == null) || renditionVisible;
    }

    @Override
    public int compareTo(RenderView o) {
        return name.compareTo(o.name);
    }
}
