/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;

/**
 * @since 7.3
 */
@XObject("bundle")
@XRegistry(compatWarnOnMerge = true)
public class ResourceBundleDescriptor implements ResourceBundle {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNodeList(value = "resources/resource", type = LinkedHashSet.class, componentType = String.class)
    @XMerge(value = XMerge.MERGE, fallback = "resources@append")
    protected Set<String> resources;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getResources() {
        return new ArrayList<>(resources);
    }

}
