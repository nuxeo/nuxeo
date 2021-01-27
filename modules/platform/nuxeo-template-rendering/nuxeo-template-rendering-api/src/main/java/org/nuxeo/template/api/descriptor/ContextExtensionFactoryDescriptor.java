/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.api.descriptor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.template.api.context.ContextExtensionFactory;

@XObject("contextFactory")
@XRegistry(enable = false)
public class ContextExtensionFactoryDescriptor {

    protected static final Log log = LogFactory.getLog(ContextExtensionFactoryDescriptor.class);

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@class")
    protected Class<? extends ContextExtensionFactory> factoryClass;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected boolean enabled;

    @XNodeList(value = "aliasName", type = ArrayList.class, componentType = String.class)
    protected List<String> aliasNames = new ArrayList<>();

    protected ContextExtensionFactory factory;

    public ContextExtensionFactory getExtensionFactory() {
        if (factory == null) {
            if (factoryClass != null) {
                try {
                    factory = factoryClass.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    log.error("Unable to instanciate Processor", e);
                }
            }
        }
        return factory;
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliasNames;
    }

}
