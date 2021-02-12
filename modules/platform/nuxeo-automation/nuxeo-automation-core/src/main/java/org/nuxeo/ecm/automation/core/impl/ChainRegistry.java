/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.impl;

import static org.nuxeo.ecm.automation.core.AutomationComponent.COMPONENT_NAME;
import static org.nuxeo.ecm.automation.core.AutomationComponent.XP_OPERATIONS;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.RegistryContribution;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.OperationChainContribution;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * Registry for {@link OperationChainContribution} contributions, forwarding to {@link OperationRegistry}.
 *
 * @since 11.5
 */
public class ChainRegistry extends MapRegistry {

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
        String id = computeId(ctx, xObject, element);
        try {
            OperationChainContribution occ = getInstance(ctx, xObject, element);
            if (!occ.replace && contributions.containsKey(id)) {
                throw new RuntimeServiceException("An operation is already bound to id '" + id
                        + "': use 'replace=\"true\"' to replace an existing operation");
            }
            OperationChain chain = occ.toOperationChain(ctx);
            ChainTypeImpl contrib = new ChainTypeImpl(chain, occ, OperationRegistry.getContributingComponentId(tag));
            var registration = new OperationRegistryContribution(ctx, xObject, element, tag, contrib.getId(),
                    occ.replace, contrib.getAliases(), contrib);
            getTargetRegistry().register(registration, tag);
        } catch (OperationException e) {
            String extensionId = RegistryContribution.getRuntimeExtensionFromTag(tag);
            String msg = String.format("Failed to register chain with id '%s' in component '%s' (%s)", id, extensionId,
                    e.toString());
            Framework.getRuntime()
                     .getMessageHandler()
                     .addMessage(new RuntimeMessage(Level.ERROR, msg, Source.EXTENSION, extensionId));
        }
    }

    @Override
    public void unregister(String tag) {
        getTargetRegistry().unregister(tag);
    }

}
