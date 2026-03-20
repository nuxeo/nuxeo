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
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Nuxeo CmisServiceFactory Descriptor.
 */
@XObject(value = "factory")
public class NuxeoCmisServiceFactoryDescriptor implements Descriptor {

    @XNode("@class")
    protected Class<? extends NuxeoCmisServiceFactory> factoryClass;

    @XNodeMap(value = "parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> factoryParameters = new HashMap<>();

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

    public Class<? extends NuxeoCmisServiceFactory> getFactoryClass() {
        return factoryClass == null ? NuxeoCmisServiceFactory.class : factoryClass;
    }

    public Map<String, String> getFactoryParameters() {
        return factoryParameters;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (NuxeoCmisServiceFactoryDescriptor) o;
        var merged = new NuxeoCmisServiceFactoryDescriptor();
        merged.factoryClass = getIfNull(other.factoryClass, factoryClass);
        merged.factoryParameters.putAll(factoryParameters);
        merged.factoryParameters.putAll(other.factoryParameters);
        return merged;
    }
}
