/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.cluster;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;

/**
 * Descriptor of a cluster node.
 *
 * @since 11.1
 */
@XObject("clusterNode")
@XRegistry
public class ClusterNodeDescriptor {

    @XNode("@id")
    public String name;

    @XNode(value = "@enabled", fallbackValue = XEnable.ENABLE)
    @XEnable
    public Boolean enabled;

    /**
     * Gets the name (id) of the cluster node.
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if cluster is enabled for this node.
     */
    public Boolean getEnabled() {
        return enabled;
    }

}
