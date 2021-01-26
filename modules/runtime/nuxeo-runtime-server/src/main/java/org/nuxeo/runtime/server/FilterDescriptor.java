/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.runtime.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor for a servlet filter. For convenience, we follow the official syntax of web.xml.
 */
@XObject("filter")
@XRegistry(merge = false)
public class FilterDescriptor {

    @XNode("@context")
    protected String context;

    /**
     * @since 10.2
     */
    @XRegistryId
    @XNode(value = "filter-name", fallback = "@name")
    protected String name;

    /**
     * @since 10.2
     */
    @XNode(value = "filter-class", fallback = "@class")
    protected Class<?> clazz;

    /**
     * @since 10.2
     */
    @XNode(value = "display-name", fallback = "description")
    protected String displayName;

    protected Map<String, String> initParams = new LinkedHashMap<>();

    /**
     * @since 10.2
     */
    @XNodeList(value = "init-param", type = ArrayList.class, componentType = InitParamDescriptor.class)
    public void setInitParams(List<InitParamDescriptor> descriptors) {
        for (InitParamDescriptor d : descriptors) {
            initParams.put(d.getName(), d.getValue());
        }
    }

    // compat
    @XNodeMap(value = "init-params/param", key = "@name", type = HashMap.class, componentType = String.class, trim = true, nullByDefault = true)
    public void setInitParams(Map<String, String> initParams) {
        this.initParams.putAll(initParams);
    }

    /**
     * @since 10.2
     */
    @XNodeList(value = "filter-mapping", type = ArrayList.class, componentType = FilterMappingDescriptor.class)
    protected List<FilterMappingDescriptor> filterMappings;

    public String getContext() {
        return context;
    }

    public String getName() {
        return name;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Map<String, String> getInitParams() {
        return initParams;
    }

    public List<FilterMappingDescriptor> getFilterMappings() {
        return filterMappings;
    }

}
