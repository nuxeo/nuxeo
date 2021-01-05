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
 * Object representing a registered automatic level of detail conversion on the {@link ThreeDService}. An
 * {@code AutomaticLOD} references the percentage of mesh polygons through its percentage.
 *
 * @since 8.4
 */
@XObject("automaticLOD")
@XRegistry(enable = false)
public class AutomaticLOD implements Comparable<AutomaticLOD> {

    @XNode("@order")
    protected Integer order;

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@percPoly")
    protected Integer percPoly;

    @XNode("@maxPoly")
    protected Long maxPoly;

    @XNode("@percTex")
    protected Integer percTex;

    @XNode("@maxTex")
    protected String maxTex;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    protected boolean enabled;

    @XNode("@rendition")
    protected Boolean rendition;

    @XNode("@renditionVisible")
    protected Boolean renditionVisible;

    public Integer getOrder() {
        return order;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getPercPoly() {
        return percPoly;
    }

    public Long getMaxPoly() {
        return maxPoly;
    }

    public Integer getPercTex() {
        return percTex;
    }

    public String getMaxTex() {
        return maxTex;
    }

    public boolean isRendition() {
        return (rendition == null) || rendition;
    }

    public boolean isRenditionVisible() {
        return (renditionVisible == null) || renditionVisible;
    }

    @Override
    public int compareTo(AutomaticLOD o) {
        return o.percPoly.compareTo(percPoly);
    }

}
