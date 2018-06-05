/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @since 7.2
 */
public class AutomationScriptingComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(AutomationScriptingComponent.class);

    protected static final String XP_OPERATION = "operation";

    protected static final String XP_CLASSFILTER = "classFilter";

    protected final AutomationScriptingServiceImpl service = new AutomationScriptingServiceImpl();

    protected final AutomationScriptingRegistry registry = new AutomationScriptingRegistry();

    protected final List<ClassFilterDescriptor> classFilterDescriptors = new ArrayList<>();

    @Override
    public void activate(ComponentContext context) {
        registry.automation = Framework.getService(AutomationService.class);
        registry.scripting = service;
    }

    @Override
    public void start(ComponentContext context) {
        boolean inlinedContext = Framework.getService(ConfigurationService.class)
                                          .isBooleanPropertyTrue("nuxeo.automation.scripting.inline-context-in-params");

        service.paramsInjector = AutomationScriptingParamsInjector.newInstance(inlinedContext);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_OPERATION.equals(extensionPoint)) {
            registry.addContribution((ScriptingOperationDescriptor) contribution);
        } else if (XP_CLASSFILTER.equals(extensionPoint)) {
            registerClassFilter((ClassFilterDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_OPERATION.equals(extensionPoint)) {
            registry.removeContribution((ScriptingOperationDescriptor) contribution);
        } else if (XP_CLASSFILTER.equals(extensionPoint)) {
            unregisterClassFilter((ClassFilterDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    protected void registerClassFilter(ClassFilterDescriptor desc) {
        classFilterDescriptors.add(desc);
        recomputeClassFilters();
    }

    protected void unregisterClassFilter(ClassFilterDescriptor desc) {
        classFilterDescriptors.remove(desc);
        recomputeClassFilters();
    }

    protected void recomputeClassFilters() {
        Set<String> allowedClassNames = new HashSet<>();
        for (ClassFilterDescriptor desc : classFilterDescriptors) {
            if (desc.deny.contains("*")) {
                allowedClassNames.clear();
                allowedClassNames.addAll(desc.allow);
            } else {
                allowedClassNames.addAll(desc.allow);
                allowedClassNames.removeAll(desc.deny);
            }
        }
        // we don't care about update atomicity, as nothing executes concurrently with XML config
        service.allowedClassNames.clear();
        service.allowedClassNames.addAll(allowedClassNames);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(AutomationScriptingService.class)) {
            return adapter.cast(service);
        }
        return super.getAdapter(adapter);
    }

}
