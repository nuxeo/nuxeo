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
package org.nuxeo.ecm.platform.content.template.service;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor of a registered {@link PostContentCreationHandler}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("postContentCreationHandler")
public class PostContentCreationHandlerDescriptor
        implements Descriptor, Comparable<PostContentCreationHandlerDescriptor> {

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected Class<PostContentCreationHandler> clazz;

    @XNode("@order")
    protected int order = 0;

    @XNode("@enabled")
    protected boolean enabled = true;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Class<PostContentCreationHandler> getClazz() {
        return clazz;
    }

    public int getOrder() {
        return order;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setClazz(Class<PostContentCreationHandler> clazz) {
        this.clazz = clazz;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (PostContentCreationHandlerDescriptor) o;
        var merged = new PostContentCreationHandlerDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.clazz = getIfNull(other.clazz, clazz);
        merged.order = other.order > 0 ? other.order : order;
        merged.enabled = other.enabled;
        return merged;
    }

    @Override
    public int compareTo(PostContentCreationHandlerDescriptor o) {
        int cmp = order - o.order;
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = name.compareTo(o.name);
        }
        return cmp;
    }
}
