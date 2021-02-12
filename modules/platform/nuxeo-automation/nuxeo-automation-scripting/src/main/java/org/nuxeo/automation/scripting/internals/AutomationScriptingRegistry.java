/*
 * (C) Copyright 2016-2021 Nuxeo SA (http://nuxeo.com/) and others.
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

 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.automation.scripting.internals;

import static org.nuxeo.ecm.automation.core.AutomationComponent.COMPONENT_NAME;
import static org.nuxeo.ecm.automation.core.AutomationComponent.XP_OPERATIONS;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.ecm.automation.core.impl.OperationRegistry;
import org.nuxeo.ecm.automation.core.impl.OperationRegistryContribution;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * Scripting registry, forwarding contributions to the {@link OperationRegistry}.
 * <p>
 * Interface modified from 11.5 to implement {@link Registry}.
 */
public class AutomationScriptingRegistry implements Registry {

    protected static OperationRegistry getTargetRegistry() {
        return Framework.getRuntime()
                        .getComponentManager()
                        .<OperationRegistry> getExtensionPointRegistry(COMPONENT_NAME, XP_OPERATIONS)
                        .orElseThrow(() -> new IllegalArgumentException(
                                String.format("Unknown target registry %s--%s", COMPONENT_NAME, XP_OPERATIONS)));
    }

    @Override
    public void tag(String id) {
        getTargetRegistry().tag(id);
    }

    @Override
    public boolean isTagged(String id) {
        return getTargetRegistry().isTagged(id);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        var contrib = (ScriptingOperationDescriptor) xObject.newInstance(ctx, element);
        contrib.setContributingComponent(OperationRegistry.getContributingComponentId(tag));
        // always replace existing operations with the same id
        var registration = new OperationRegistryContribution(ctx, xObject, element, tag, contrib.getId(), true,
                contrib.getAliases(), new ScriptingOperationTypeImpl(contrib));
        getTargetRegistry().register(registration, tag);
    }

    @Override
    public void unregister(String tag) {
        getTargetRegistry().unregister(tag);
    }

}
