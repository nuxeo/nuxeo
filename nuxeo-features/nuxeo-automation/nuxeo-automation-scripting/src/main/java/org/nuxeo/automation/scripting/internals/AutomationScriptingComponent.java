/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Stephane Lacoin <slacoin@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.internals.operation.ScriptingOperationDescriptor;
import org.nuxeo.automation.scripting.internals.operation.ScriptingOperationTypeImpl;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 7.2
 */
public class AutomationScriptingComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(AutomationScriptingComponent.class);

    protected final AutomationScriptingRegistry registry = new AutomationScriptingRegistry();

    protected AutomationScriptingServiceImpl service;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof ScriptingOperationDescriptor) {
            registry.addContribution((ScriptingOperationDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof ScriptingOperationDescriptor) {
            registry.removeContribution((ScriptingOperationDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        super.applicationStarted(context);
        service = new AutomationScriptingServiceImpl();
        AutomationService automation = Framework.getService(AutomationService.class);
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP) {
                    return;
                }
                Framework.removeListener(this);
                registry.stream().forEach(contrib -> {
                    try {
                        ScriptingOperationTypeImpl type = new ScriptingOperationTypeImpl(service,
                                automation, contrib);
                        automation.removeOperation(type);
                    } catch (OperationException e) {
                        LogFactory.getLog(AutomationScriptingRegistry.class)
                                .error("Cannot contribute scripting operation " + contrib.getId());
                    }
                });
            }
        });
        {
            registry.stream().forEach(contrib -> {
                try {
                    ScriptingOperationTypeImpl type = new ScriptingOperationTypeImpl(service,
                            automation, contrib);
                    automation.putOperation(type, true);
                } catch (OperationException e) {
                    LogFactory.getLog(AutomationScriptingRegistry.class)
                            .error("Cannot contribute scripting operation " + contrib.getId());
                }
            });
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(AutomationScriptingService.class)) {
            return adapter.cast(service);
        }
        return super.getAdapter(adapter);
    }

}
