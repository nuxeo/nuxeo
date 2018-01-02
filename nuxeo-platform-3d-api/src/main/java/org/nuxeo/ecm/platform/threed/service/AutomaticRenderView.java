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
 * Object representing an automatic render view on the {@link ThreeDService}. An {@code AutomaticRenderView} references
 * the {@code RenderView} through its name.
 *
 * @since 8.4
 */
@XObject("automaticRenderView")
public class AutomaticRenderView implements Comparable<AutomaticRenderView> {

    @XNode("@order")
    protected Integer order;

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled;

    public AutomaticRenderView(AutomaticRenderView other) {
        order = other.order;
        name = other.name;
        enabled = other.enabled;
    }

    public AutomaticRenderView() {
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

    public String getId() {
        return String.valueOf(name.hashCode() & 0x7fffffff);
    }

    public boolean isEnabled() {
        return (enabled == null) || enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int compareTo(AutomaticRenderView o) {
        return name.compareTo(o.name);
    }

    public void merge(AutomaticRenderView src) {
        if (src.order != null) {
            order = src.order;
        }
        if (src.name != null) {
            name = src.name;
        }
        if (src.enabled != null) {
            enabled = src.enabled;
        }
    }
}
