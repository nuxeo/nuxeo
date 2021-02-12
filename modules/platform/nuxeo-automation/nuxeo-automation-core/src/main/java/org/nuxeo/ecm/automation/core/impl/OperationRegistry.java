/*
 * (C) Copyright 2012-2021 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.OperationContribution;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetDescriptor;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * Registry for {@link OperationContribution} descriptors as well as scripting operations.
 * <p>
 * This registry is thread safe and optimized for lookups at runtime.
 *
 * @since 11.5
 */
public class OperationRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(OperationRegistry.class);

    // specific API to handle the chain and scripting operations
    public void register(OperationRegistryContribution registryContrib, String tag) {
        tag(tag);
        registrations.add(registryContrib);
        setInitialized(false);
    }

    @Override
    public void initialize() {
        registrations.forEach(rc -> {
            if (rc instanceof OperationRegistryContribution) {
                doRegisterInstance((OperationRegistryContribution) rc);
            } else {
                doRegister(rc.getContext(), rc.getObject(), rc.getElement(), rc.getRuntimeExtensionFromTag());
            }
        });
        setInitialized(true);
    }

    protected void checkReplace(String id, boolean replace) {
        if (!replace && contributions.containsKey(id)) {
            throw new RuntimeServiceException("An operation is already bound to id '" + id
                    + "': use 'replace=\"true\"' to replace an existing operation");
        }
    }

    protected void doRegisterInstance(OperationRegistryContribution rc) {
        String id = rc.getId();
        checkReplace(id, rc.isReplace());
        Object contrib = rc.getInstance();
        contributions.put(id, contrib);
        String[] aliases = rc.getAliases();
        doRegisterAliases(id, contrib, aliases);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        OperationContribution opc = getInstance(ctx, xObject, element);
        try {
            Class<?> type = Class.forName(opc.type);

            List<WidgetDefinition> widgets = null;
            if (opc.widgets != null) {
                widgets = opc.widgets.stream().map(WidgetDescriptor::getWidgetDefinition).collect(Collectors.toList());
            }
            OperationTypeImpl contrib = new OperationTypeImpl(type, getContributingComponentId(extensionId), widgets);
            String id = contrib.getId();
            checkReplace(id, opc.replace);

            contributions.put(id, contrib);
            doRegisterAliases(id, contrib, contrib.getAliases());
            return (T) contrib;
        } catch (ClassNotFoundException | RuntimeServiceException | IllegalArgumentException e) {
            String msg = String.format("Failed to register operation in component '%s' with class '%s' (%s)",
                    extensionId, opc.type, e.toString());
            Framework.getRuntime()
                     .getMessageHandler()
                     .addMessage(new RuntimeMessage(Level.ERROR, msg, Source.EXTENSION, extensionId));
            return null;
        }
    }

    protected void doRegisterAliases(String id, Object contrib, String[] aliases) {
        for (String alias : aliases) {
            if (id.equals(alias)) {
                log.warn("Useless alias for operation with id '{}'", id);
                continue;
            }
            if (contributions.containsKey(alias)) {
                log.warn("Overriding operation with id '{}' with alias from operation '{}'", alias, id);
            }
            contributions.put(alias, contrib);
        }
    }

    // API

    public static final String getContributingComponentId(String tagOrExtensionId) {
        return StringUtils.substringBeforeLast(tagOrExtensionId, "#");
    }

    public OperationType getOperationType(Class<?> key) {
        return (OperationType) contributions.get(key.getAnnotation(Operation.class).id());
    }

}
