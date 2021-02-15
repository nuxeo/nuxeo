/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.audit.service.extension;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Really simple auditable event descriptor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("event")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class EventDescriptor {

    @XNode("@name")
    @XRegistryId
    private String name;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    private boolean enabled;

    @XNodeList(value = "extendedInfos/extendedInfo", type = ArrayList.class, componentType = ExtendedInfoDescriptor.class)
    protected List<ExtendedInfoDescriptor> extendedInfoDescriptors;

    public boolean getEnabled() {
        return enabled;
    }

    /**
     * @since 7.4
     */
    public List<ExtendedInfoDescriptor> getExtendedInfoDescriptors() {
        return extendedInfoDescriptors;
    }

    public String getName() {
        return name;
    }

}
