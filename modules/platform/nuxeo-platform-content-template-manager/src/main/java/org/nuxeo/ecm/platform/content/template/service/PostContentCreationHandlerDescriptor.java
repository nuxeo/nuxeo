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

package org.nuxeo.ecm.platform.content.template.service;

import java.util.Comparator;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor of a registered {@link PostContentCreationHandler}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("postContentCreationHandler")
@XRegistry(enable = false)
public class PostContentCreationHandlerDescriptor implements Comparable<PostContentCreationHandlerDescriptor> {

    /** @since 11.5 **/
    public static final Comparator<PostContentCreationHandlerDescriptor> COMPARATOR = //
            Comparator.comparing(PostContentCreationHandlerDescriptor::getOrder)
                      .thenComparing(PostContentCreationHandlerDescriptor::getName);

    @XNode("@name")
    @XRegistryId
    private String name;

    @XNode("@class")
    private Class<PostContentCreationHandler> clazz;

    @XNode("@order")
    private int order = 0;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    private boolean enabled;

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

    @Override
    public int compareTo(PostContentCreationHandlerDescriptor o) {
        return COMPARATOR.compare(this, o);
    }

}
