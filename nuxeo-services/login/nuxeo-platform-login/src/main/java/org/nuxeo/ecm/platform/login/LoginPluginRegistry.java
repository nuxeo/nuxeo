/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.login;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class LoginPluginRegistry extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.platform.login.LoginPluginRegistry");

    public static final String EP_PLUGIN = "plugin";

    public static final String EP_CBFACTORY = "callbackFactory";

    private static final Log log = LogFactory.getLog(LoginPluginRegistry.class);

    private LoginPlugin currentLoginPlugin;

    private Map<String, LoginPlugin> loginPluginStack;

    private CallbackFactory callbackFactory;

    private Map<String, LoginPluginDescriptor> pluginDescriptorStack;

    public LoginPluginRegistry() {
        currentLoginPlugin = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(EP_PLUGIN)) {
            log.info("registering Login Plugin ... ");
            registerPlugin((LoginPluginDescriptor) contribution);
        } else if (extensionPoint.equals(EP_CBFACTORY)) {
            log.info("registering Callback factory ... ");
            registerCBFactory((CallbackFactoryDescriptor) contribution);
        } else {
            log.error("Extension point " + extensionPoint + " is unknown!");
        }
    }

    private void registerCBFactory(CallbackFactoryDescriptor cbfExtension) {
        try {
            callbackFactory = (CallbackFactory) cbfExtension.getClassName().newInstance();
        } catch (ReflectiveOperationException e) {
            log.error("Unable to create Factory", e);
        }
    }

    private void registerPlugin(LoginPluginDescriptor pluginExtension) {
        Boolean enabled = pluginExtension.getEnabled();
        Class<LoginPlugin> className = pluginExtension.getClassName();
        String pluginName = pluginExtension.getPluginName();

        if (loginPluginStack.containsKey(pluginName)) {
            // merge
            LoginPlugin oldLoginPlugin = loginPluginStack.get(pluginName);
            LoginPluginDescriptor oldLoginPluginDescriptor = pluginDescriptorStack.get(pluginName);

            Map<String, String> mergedParams = oldLoginPluginDescriptor.getParameters();
            mergedParams.putAll(pluginExtension.getParameters());

            oldLoginPlugin.setParameters(mergedParams);
            if (!oldLoginPlugin.initLoginModule()) {
                oldLoginPluginDescriptor.setInitialized(false);
                log.warn("Unable to initialize LoginPlugin for class " + className.getName());
            } else {
                oldLoginPluginDescriptor.setInitialized(true);
            }
            if (enabled != null) {
                oldLoginPluginDescriptor.setEnabled(enabled);
            }
        } else {
            LoginPlugin newLoginPlugin;
            try {
                newLoginPlugin = className.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("Unable to create LoginPlugin for class " + className.getName() + ":" + e.getMessage(), e);
                return;
            }
            newLoginPlugin.setParameters(pluginExtension.getParameters());
            if (newLoginPlugin.initLoginModule()) {
                pluginExtension.setInitialized(true);
                log.info("LoginPlugin initialized for class " + className.getName());
            } else {
                pluginExtension.setInitialized(false);
                log.warn("Unable to initialize LoginPlugin for class " + className.getName());
            }
            pluginDescriptorStack.put(pluginName, pluginExtension);
            loginPluginStack.put(pluginName, newLoginPlugin);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        currentLoginPlugin = null;
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        loginPluginStack = new HashMap<>();
        pluginDescriptorStack = new HashMap<>();
    }

    public CallbackResult handleSpecifcCallbacks(CallbackHandler callbackHandler) {
        if (callbackFactory == null) {
            return null;
        }
        return callbackFactory.handleSpecificCallbacks(callbackHandler);
    }

    public LoginPlugin getPlugin(String pluginName) {
        if (!pluginDescriptorStack.containsKey(pluginName)) {
            log.error("Unable to find needed Login Plugin : " + pluginName);
            return null;
        }

        LoginPlugin loginPlugin = loginPluginStack.get(pluginName);
        LoginPluginDescriptor loginPluginDescriptor = pluginDescriptorStack.get(pluginName);

        if (loginPlugin == null) {
            log.error("Login Plugin : " + pluginName + " is null ");
            return null;
        }

        if (!loginPluginDescriptor.getEnabled()) {
            log.error("Login Plugin : " + pluginName + " is not Enabled ");
            return null;
        }
        return loginPlugin;
    }

    public LoginPluginDescriptor getPluginDescriptor(String pluginName) {
        if (!pluginDescriptorStack.containsKey(pluginName)) {
            log.error("Unable to find needed Login Plugin : " + pluginName);
            return null;
        }

        LoginPlugin loginPlugin = loginPluginStack.get(pluginName);
        LoginPluginDescriptor loginPluginDescriptor = pluginDescriptorStack.get(pluginName);

        if (loginPlugin == null) {
            log.error("Login Plugin : " + pluginName + " is null ");
            return null;
        }

        if (!loginPluginDescriptor.getEnabled()) {
            log.error("Login Plugin : " + pluginName + " is not Enabled ");
            return null;
        }
        return loginPluginDescriptor;
    }

}
