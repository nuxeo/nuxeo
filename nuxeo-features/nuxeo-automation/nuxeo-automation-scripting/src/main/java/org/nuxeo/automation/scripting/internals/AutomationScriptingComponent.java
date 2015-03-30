/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import jdk.nashorn.api.scripting.ClassFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.automation.scripting.api.AutomationScriptingException;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.internals.operation
        .ScriptingOperationDescriptor;
import org.nuxeo.automation.scripting.internals.operation
        .ScriptingOperationTypeImpl;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.metrics.MetricInvocationHandler;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 7.2
 */
public class AutomationScriptingComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(AutomationScriptingComponent.class);

    public AutomationScriptingService scriptingService = new AutomationScriptingServiceImpl();

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        if (Boolean.valueOf(Framework.getProperty(AutomationScriptingConstants.AUTOMATION_SCRIPTING_MONITOR,
                Boolean.toString(log.isTraceEnabled())))) {
            scriptingService = MetricInvocationHandler.newProxy(scriptingService, AutomationScriptingService.class);
        }
        String version = System.getProperty("java.version");
        if (version.contains(AutomationScriptingConstants.NASHORN_JAVA_VERSION)) {
            if (version.compareTo(AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CACHE) < 0) {
                log.warn(AutomationScriptingConstants.NASHORN_WARN_CACHE);
                scriptingService.setCacheActivation(false);
                scriptingService.setClassFilterActivation(false);
            } else if (version.compareTo(AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CACHE) > 0
                    && version.compareTo(AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CLASS_FILTER) < 0) {
                log.warn(AutomationScriptingConstants.NASHORN_WARN_CLASS_FILTER);
                scriptingService.setClassFilterActivation(false);
            } else {
                // Double security check to find if class filter exists or not (jdk8u40)
                try {
                    Class<?> klass = Class.forName(ClassFilter.class.getName());
                } catch (ClassNotFoundException e) {
                    log.warn(AutomationScriptingConstants.NASHORN_WARN_CLASS_FILTER);
                    scriptingService.setClassFilterActivation(false);
                }
            }
        }else{
            log.warn(AutomationScriptingConstants.NASHORN_WARN_VERSION);
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (AutomationScriptingConstants.XP_OPERATION.equals(extensionPoint)) {
            AutomationService automationService = Framework.getLocalService(AutomationService.class);
            ScriptingOperationDescriptor desc = (ScriptingOperationDescriptor) contribution;
            ScriptingOperationTypeImpl type = new ScriptingOperationTypeImpl(automationService, desc);
            try {
                automationService.putOperation(type, true);
            } catch (OperationException e) {
                throw new AutomationScriptingException(e);
            }
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (AutomationScriptingConstants.XP_OPERATION.equals(extensionPoint)) {
            AutomationService automationService = Framework.getLocalService(AutomationService.class);
            ScriptingOperationDescriptor desc = (ScriptingOperationDescriptor) contribution;
            ScriptingOperationTypeImpl type = new ScriptingOperationTypeImpl(automationService, desc);
            automationService.removeOperation(type);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        super.applicationStarted(context);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(AutomationScriptingService.class)) {
            return adapter.cast(scriptingService);
        }
        return null;
    }

}
