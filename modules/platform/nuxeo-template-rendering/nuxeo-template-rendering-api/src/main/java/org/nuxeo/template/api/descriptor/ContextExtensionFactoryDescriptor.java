/*
 * (C) Copyright 2012-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.template.api.context.ContextExtensionFactory;

@XObject("contextFactory")
public class ContextExtensionFactoryDescriptor implements Descriptor {

    private static final Logger log = LogManager.getLogger(ContextExtensionFactoryDescriptor.class);

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected Class<? extends ContextExtensionFactory> factoryClass;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNodeList(value = "aliasName", type = ArrayList.class, componentType = String.class)
    protected List<String> aliasNames = new ArrayList<>();

    protected ContextExtensionFactory factory;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getAliases() {
        return aliasNames;
    }

    public ContextExtensionFactory getExtensionFactory() {
        if (factory == null) {
            if (factoryClass != null) {
                try {
                    factory = factoryClass.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    log.error("Unable to instantiate Processor", e);
                }
            }
        }
        return factory;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (ContextExtensionFactoryDescriptor) o;
        var merged = new ContextExtensionFactoryDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.factoryClass = getIfNull(other.factoryClass, factoryClass);
        merged.enabled = other.enabled;
        merged.aliasNames = new ArrayList<>(aliasNames);
        merged.aliasNames.addAll(other.aliasNames);
        return merged;
    }
}
