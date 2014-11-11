/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core;

import java.util.ArrayList;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationAdmin;
import org.nuxeo.ecm.automation.AutomationFilter;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.ChainException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.events.EventHandler;
import org.nuxeo.ecm.automation.core.events.EventHandlerRegistry;
import org.nuxeo.ecm.automation.core.exception.ChainExceptionFilter;
import org.nuxeo.ecm.automation.core.exception.ChainExceptionImpl;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.OperationServiceImpl;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetDescriptor;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Nuxeo component that provide an implementation of the
 * {@link AutomationService} and handle extensions registrations.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
public class AutomationComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(AutomationComponent.class);

    public static final String XP_OPERATIONS = "operations";

    public static final String XP_ADAPTERS = "adapters";

    public static final String XP_CHAINS = "chains";

    public static final String XP_EVENT_HANDLERS = "event-handlers";

    public static final String XP_CHAIN_EXCEPTION = "chainException";

    public static final String XP_AUTOMATION_FILTER = "automationFilter";

    protected OperationServiceImpl service;

    protected EventHandlerRegistry handlers;

    protected TracerFactory tracerFactory;

    @Override
    public void activate(ComponentContext context) throws Exception {
        service = new OperationServiceImpl();
        tracerFactory = new TracerFactory();
        handlers = new EventHandlerRegistry(service);
    }

    protected void bindManagement() throws MalformedObjectNameException,
            NotCompliantMBeanException, InstanceAlreadyExistsException,
            MBeanRegistrationException {
        final ObjectName objectName = new ObjectName(
                "org.nuxeo.automation:name=tracerfactory");
        MBeanServer mBeanServer = Framework.getLocalService(ServerLocator.class).lookupServer();
        mBeanServer.registerMBean(tracerFactory, objectName);
    }

    protected void unBindManagement() throws MalformedObjectNameException,
            NotCompliantMBeanException, InstanceAlreadyExistsException,
            MBeanRegistrationException, InstanceNotFoundException {
        final ObjectName on = new ObjectName(
                "org.nuxeo.automation:name=tracerfactory");
        final ServerLocator locator = Framework.getLocalService(ServerLocator.class);
        if (locator != null) {
            MBeanServer mBeanServer = locator.lookupServer();
            mBeanServer.unregisterMBean(on);
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        service = null;
        handlers = null;
        tracerFactory = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_OPERATIONS.equals(extensionPoint)) {
            OperationContribution opc = (OperationContribution) contribution;
            List<WidgetDefinition> widgetDefinitionList = new ArrayList<WidgetDefinition>();
            if (opc.widgets != null) {
                for (WidgetDescriptor widgetDescriptor : opc.widgets) {
                    widgetDefinitionList.add(widgetDescriptor.getWidgetDefinition());
                }
            }
            service.putOperation(opc.type, opc.replace,
                    contributor.getName().toString(), widgetDefinitionList);
        } else if (XP_CHAINS.equals(extensionPoint)) {
            OperationChainContribution occ = (OperationChainContribution) contribution;
            // Register the chain
            OperationType docChainType = new ChainTypeImpl(service,
                    occ.toOperationChain(contributor.getContext().getBundle()),
                    occ);
            service.putOperation(docChainType, occ.replace);
        } else if (XP_CHAIN_EXCEPTION.equals(extensionPoint)) {
            ChainExceptionDescriptor chainExceptionDescriptor = (ChainExceptionDescriptor) contribution;
            ChainException chainException = new ChainExceptionImpl(
                    chainExceptionDescriptor);
            service.putChainException(chainException);
        } else if (XP_AUTOMATION_FILTER.equals(extensionPoint)) {
            AutomationFilterDescriptor automationFilterDescriptor = (AutomationFilterDescriptor) contribution;
            ChainExceptionFilter chainExceptionFilter = new ChainExceptionFilter(
                    automationFilterDescriptor);
            service.putAutomationFilter(chainExceptionFilter);
        } else if (XP_ADAPTERS.equals(extensionPoint)) {
            TypeAdapterContribution tac = (TypeAdapterContribution) contribution;
            service.putTypeAdapter(tac.accept, tac.produce,
                    tac.clazz.newInstance());
        } else if (XP_EVENT_HANDLERS.equals(extensionPoint)) {
            EventHandler eh = (EventHandler) contribution;
            if (eh.isPostCommit()) {
                handlers.putPostCommitEventHandler(eh);
            } else {
                handlers.putEventHandler(eh);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_OPERATIONS.equals(extensionPoint)) {
            service.removeOperation(((OperationContribution) contribution).type);
        } else if (XP_CHAINS.equals(extensionPoint)) {
            OperationChainContribution occ = (OperationChainContribution) contribution;
            service.removeOperationChain(occ.getId());
        } else if (XP_CHAIN_EXCEPTION.equals(extensionPoint)) {
            ChainExceptionDescriptor chainExceptionDescriptor = (ChainExceptionDescriptor) contribution;
            ChainException chainException = new ChainExceptionImpl(
                    chainExceptionDescriptor);
            service.removeExceptionChain(chainException);
        } else if (XP_AUTOMATION_FILTER.equals(extensionPoint)) {
            AutomationFilterDescriptor automationFilterDescriptor = (AutomationFilterDescriptor) contribution;
            AutomationFilter automationFilter = new ChainExceptionFilter(
                    automationFilterDescriptor);
            service.removeAutomationFilter(automationFilter);
        } else if (XP_ADAPTERS.equals(extensionPoint)) {
            TypeAdapterContribution tac = (TypeAdapterContribution) contribution;
            service.removeTypeAdapter(tac.accept, tac.produce);
        } else if (XP_EVENT_HANDLERS.equals(extensionPoint)) {
            EventHandler eh = (EventHandler) contribution;
            if (eh.isPostCommit()) {
                handlers.removePostCommitEventHandler(eh);
            } else {
                handlers.removeEventHandler(eh);
            }
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == AutomationService.class || adapter == AutomationAdmin
                .class) {
            return adapter.cast(service);
        }
        if (adapter == EventHandlerRegistry.class) {
            return adapter.cast(handlers);
        }
        if (adapter == TracerFactory.class) {
            return adapter.cast(tracerFactory);
        }
        return null;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        super.applicationStarted(context);
        if (!tracerFactory.getRecordingState()) {
            log.info("You can activate automation trace mode to get more informations on automation executions");
        }
        bindManagement();
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP) {
                    return;
                }
                Framework.removeListener(this);
                try {
                    unBindManagement();
                } catch (MalformedObjectNameException
                        | NotCompliantMBeanException
                        | InstanceAlreadyExistsException
                        | MBeanRegistrationException
                        | InstanceNotFoundException cause) {
                    log.error("Cannot unbind management", cause);
                }
            }
        });
    }
}
