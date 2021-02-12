/*
 * (C) Copyright 2015-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Guillaume Renard
 */
package org.nuxeo.ecm.automation.core;

import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.AutomationAdmin;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.context.ContextService;
import org.nuxeo.ecm.automation.context.ContextServiceImpl;
import org.nuxeo.ecm.automation.core.events.EventHandlerRegistry;
import org.nuxeo.ecm.automation.core.impl.OperationServiceImpl;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Nuxeo component that provide an implementation of the {@link AutomationService} and handle extensions registrations.
 */
public class AutomationComponent extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(AutomationComponent.class);

    /** @since 11.5 */
    public static final String COMPONENT_NAME = "org.nuxeo.ecm.automation.core.AutomationComponent";

    public static final String XP_OPERATIONS = "operations";

    public static final String XP_ADAPTERS = "adapters";

    public static final String XP_CHAINS = "chains";

    public static final String XP_EVENT_HANDLERS = "event-handlers";

    public static final String XP_CHAIN_EXCEPTION = "chainException";

    public static final String XP_AUTOMATION_FILTER = "automationFilter";

    public static final String XP_CONTEXT_HELPER = "contextHelpers";

    protected OperationServiceImpl service;

    protected ContextService contextService;

    protected TracerFactory tracerFactory;

    protected void bindManagement() throws JMException {
        ObjectName objectName = new ObjectName("org.nuxeo.automation:name=tracerfactory");
        MBeanServer mBeanServer = Framework.getService(ServerLocator.class).lookupServer();
        mBeanServer.registerMBean(tracerFactory, objectName);
    }

    protected void unBindManagement()
            throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        final ObjectName on = new ObjectName("org.nuxeo.automation:name=tracerfactory");
        final ServerLocator locator = Framework.getService(ServerLocator.class);
        if (locator != null) {
            MBeanServer mBeanServer = locator.lookupServer();
            mBeanServer.unregisterMBean(on);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == AutomationService.class || adapter == AutomationAdmin.class) {
            return adapter.cast(service);
        }
        if (adapter == EventHandlerRegistry.class) {
            return adapter.cast(getExtensionPointRegistry(XP_EVENT_HANDLERS));
        }
        if (adapter == TracerFactory.class) {
            return adapter.cast(tracerFactory);
        }
        if (adapter == ContextService.class) {
            return adapter.cast(contextService);
        }
        return null;
    }

    @Override
    public void start(ComponentContext context) {
        service = new OperationServiceImpl(getExtensionPointRegistry(XP_OPERATIONS),
                getExtensionPointRegistry(XP_CHAINS), getExtensionPointRegistry(XP_CHAIN_EXCEPTION),
                getExtensionPointRegistry(XP_AUTOMATION_FILTER), getExtensionPointRegistry(XP_ADAPTERS));
        checkOperationChains();
        contextService = new ContextServiceImpl(getExtensionPointRegistry(XP_CONTEXT_HELPER));

        tracerFactory = new TracerFactory();
        if (!tracerFactory.getRecordingState()) {
            log.info("You can activate automation trace mode to get more informations on automation executions");
        }
        try {
            bindManagement();
        } catch (JMException e) {
            throw new RuntimeException("Cannot bind management", e);
        }
    }

    /**
     * Checks operation references in chains
     *
     * @since 11.3
     */
    protected void checkOperationChains() {
        List<OperationChain> chains = service.getOperationChains();
        for (OperationChain chain : chains) {
            List<OperationParameters> opps = chain.getOperations();
            for (OperationParameters opp : opps) {
                if (!service.hasOperation(opp.id())) {
                    String msg = String.format("Operation chain with id '%s' references unknown operation with id '%s'",
                            chain.getId(), opp.id());
                    log.error(msg);
                    addRuntimeMessage(Level.ERROR, msg);
                }
            }
        }
    }

    @Override
    public void stop(ComponentContext context) {
        service.flushCompiledChains();
        service = null;
        contextService = null;

        try {
            unBindManagement();
        } catch (JMException e) {
            throw new RuntimeException("Cannot unbind management", e);
        }
        tracerFactory = null;
    }
}
