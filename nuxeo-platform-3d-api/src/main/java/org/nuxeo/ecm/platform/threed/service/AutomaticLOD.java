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

    @XNode("@percentage")
    private float percentage = 100.0f;

    @XNode("@enabled")
    private boolean enabled = true;

    public AutomaticLOD(AutomaticLOD anAutomaticLOD) {
        percentage = anAutomaticLOD.getPercentage();
        enabled = anAutomaticLOD.isEnabled();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getPercentage() {
        return percentage;
    }

    public void setPercentage(float percentage) {
        this.percentage = percentage;
    }

    @Override
    public int compareTo(AutomaticLOD o) {
        return Float.valueOf(o.percentage).compareTo(percentage);
    }

    public void merge(AutomaticLOD src) {
        enabled = src.isEnabled();
    }
}
