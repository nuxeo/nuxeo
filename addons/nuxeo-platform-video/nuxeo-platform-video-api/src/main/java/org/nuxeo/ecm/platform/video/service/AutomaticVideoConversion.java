/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Object representing a registered automatic video conversion on the {@link VideoService}.
 * <p>
 * An {@code AutomaticVideoConversion} references the {@code VideoConversion} through its name.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("automaticVideoConversion")
public class AutomaticVideoConversion implements Cloneable, Comparable<AutomaticVideoConversion> {

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    private boolean enabled = true;

    @XNode("@order")
    private int order = 0;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public AutomaticVideoConversion clone() throws CloneNotSupportedException {
        return (AutomaticVideoConversion) super.clone();
    }

    @Override
    public int compareTo(AutomaticVideoConversion o) {
        int cmp = order - o.order;
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = name.compareTo(o.name);
        }
        return cmp;
    }

}
