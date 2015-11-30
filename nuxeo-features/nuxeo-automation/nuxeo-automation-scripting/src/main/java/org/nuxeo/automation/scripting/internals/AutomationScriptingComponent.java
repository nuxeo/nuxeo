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
 *     Stephane Lacoin <slacoin@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import javax.script.ScriptEngineManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.internals.operation.ScriptingOperationDescriptor;
import org.nuxeo.automation.scripting.internals.operation.ScriptingOperationTypeImpl;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.NuxeoException;
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

    protected ScriptingFactory scriptingFactory;

    public AutomationScriptingService scriptingService = new AutomationScriptingServiceImpl();

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        scriptingFactory = new ScriptingFactory();
        scriptingFactory.install();
        if (Boolean.valueOf(Framework.getProperty(AutomationScriptingConstants.AUTOMATION_SCRIPTING_MONITOR,
                Boolean.toString(log.isTraceEnabled())))) {
            scriptingService = MetricInvocationHandler.newProxy(scriptingService, AutomationScriptingService.class);
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
                throw new NuxeoException(e);
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
        if (adapter.isAssignableFrom(ScriptEngineManager.class)) {
            return adapter.cast(scriptingFactory.scriptEngineManager);
        }
        return null;
    }

}
