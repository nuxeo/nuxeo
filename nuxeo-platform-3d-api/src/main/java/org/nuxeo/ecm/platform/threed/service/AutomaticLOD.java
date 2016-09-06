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
 * Object representing a registered automatic level of detail conversion on the {@link ThreeDService}. An
 * {@code AutomaticLOD} references the percentage of mesh polygons through its percentage.
 *
 * @since 8.4
 */
@XObject("automaticLOD")
public class AutomaticLOD implements Comparable<AutomaticLOD> {

    @XNode("@order")
    protected  Integer order;

    @XNode("@name")
    protected String name;

    @XNode("@percentage")
    protected Integer percentage;

    @XNode("@maxPoly")
    protected Long maxPoly;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@rendition")
    protected Boolean rendition;

    @XNode("@renditionVisible")
    protected Boolean renditionVisible;

    public AutomaticLOD(AutomaticLOD other) {
        order = other.order;
        name = other.name;
        percentage = other.percentage;
        maxPoly = other.maxPoly;
        enabled = other.enabled;
        rendition = other.rendition;
        renditionVisible = other.renditionVisible;
    }

    public AutomaticLOD() {
        super();
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return (enabled == null) || enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPercentage() {
        return percentage;
    }

    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    public Long getMaxPoly() {
        return maxPoly;
    }

    public void setMaxPoly(Long maxPoly) {
        this.maxPoly = maxPoly;
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

    public String getId() {
        return String.valueOf(name.hashCode());
    }

    @Override
    public int compareTo(AutomaticLOD o) {
        return o.percentage.compareTo(percentage);
    }

    public void merge(AutomaticLOD src) {
        if (src.enabled != null) {
            enabled = src.enabled;
        }
        if (src.rendition != null) {
            rendition = src.rendition;
        }
        if (src.renditionVisible != null) {
            renditionVisible = src.renditionVisible;
        }
    }
}
