/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.core;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.web.resources.api.Processor;

/**
 * @since 7.3
 */
@XObject("processor")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class ProcessorDescriptor implements Processor {

    @XNode("@name")
    @XRegistryId
    public String name;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected boolean enabled;

    @XNode("class")
    Class<?> klass;

    @XNode("@order")
    protected int order = 0;

    @XNode("@type")
    protected String type;

    @XNode(value = "types@append")
    protected Boolean appendTypes;

    @XNodeList(value = "types/type", type = ArrayList.class, componentType = String.class)
    protected List<String> types;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getTypes() {
        List<String> types = new ArrayList<>();
        if (type != null) {
            types.add(type);
        }
        types.addAll(this.types);
        return types;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Class<?> getTargetProcessorClass() {
        return klass;
    }

    @Override
    public int compareTo(Processor o) {
        int cmp = order - o.getOrder();
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = name.compareTo(o.getName());
        }
        return cmp;
    }

}
