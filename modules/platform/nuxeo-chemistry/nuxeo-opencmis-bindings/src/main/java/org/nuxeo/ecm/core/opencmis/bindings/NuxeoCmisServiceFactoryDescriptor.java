/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;

/**
 * Nuxeo CmisServiceFactory Descriptor.
 */
@XObject(value = "factory")
@XRegistry
public class NuxeoCmisServiceFactoryDescriptor {

    @XNode("@class")
    public Class<? extends NuxeoCmisServiceFactory> factoryClass;

    @XNodeMap(value = "parameter", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> factoryParameters;

    public Class<? extends NuxeoCmisServiceFactory> getFactoryClass() {
        return factoryClass == null ? NuxeoCmisServiceFactory.class : factoryClass;
    }

}
