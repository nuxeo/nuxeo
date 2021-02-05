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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.relations.descriptors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;

/**
 * Graph descriptor.
 */
@XObject("graph")
@XRegistry(compatWarnOnMerge = true)
public class GraphDescriptor implements GraphDescription {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@type")
    protected String graphType;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> options = new HashMap<>();

    @XNodeMap(value = "namespaces/namespace", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> namespaces = new HashMap<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGraphType() {
        return graphType;
    }

    @Override
    public Map<String, String> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    @Override
    public Map<String, String> getNamespaces() {
        return Collections.unmodifiableMap(namespaces);
    }

}
