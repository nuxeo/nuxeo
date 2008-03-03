/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.login;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class LoginPluginRegistry extends DefaultComponent {

    public static final ComponentName NAME =
        new ComponentName("org.nuxeo.ecm.platform.login.LoginPluginRegistry");

    private static final Log log = LogFactory.getLog(LoginPluginRegistry.class);

    private LoginPlugin currentLoginPlugin;

    private Map<String, LoginPlugin> loginPluginStack;

    private Map<String, LoginPluginDescriptor> pluginDescriptorStack;

    public LoginPluginRegistry() {
        currentLoginPlugin = null;
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            log.info("registering Login Plugin ... ");
            register((LoginPluginDescriptor) contrib);
        }
    }

    private void register(LoginPluginDescriptor pluginExtension) {
        Boolean enabled = pluginExtension.getEnabled();
        //String loginPage = pluginExtension.getLoginPage();
        Class className = pluginExtension.getClassName();
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
                log.warn("Unable to initialize LoginPlugin for class "
                        + className.getName());
            } else {
                oldLoginPluginDescriptor.setInitialized(true);
            }
            if (enabled != null) {
                oldLoginPluginDescriptor.setEnabled(enabled);
            }
        } else {
            LoginPlugin newLoginPlugin = null;
            try {
                newLoginPlugin = (LoginPlugin) className.newInstance();
            } catch (InstantiationException e) {
                log.error("Unable to create LoginPlugin for class "
                        + className.getName() + ":" + e.getMessage());
                e.printStackTrace();
                return;
            } catch (IllegalAccessException e) {
                log.error("Unable to create LoginPlugin for class "
                        + className.getName() + ":" + e.getMessage());
                e.printStackTrace();
                return;
            }
            newLoginPlugin.setParameters(pluginExtension.getParameters());
            if (newLoginPlugin.initLoginModule()) {
                pluginExtension.setInitialized(true);
                log.info("LoginPlugin initialized for class "
                        + className.getName());
            } else {
                pluginExtension.setInitialized(false);
                log.warn("Unable to initialize LoginPlugin for class "
                        + className.getName());
            }
            pluginDescriptorStack.put(pluginName, pluginExtension);
            loginPluginStack.put(pluginName, newLoginPlugin);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        currentLoginPlugin = null;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        loginPluginStack = new HashMap<String, LoginPlugin>();
        pluginDescriptorStack = new HashMap<String, LoginPluginDescriptor>();
    }

/*    @Override
    public void deactivate(RuntimeContext context) throws Exception {
        super.deactivate(context);
    }
  */
    @Deprecated
    public LoginPlugin getPlugin() {
        return currentLoginPlugin;
    }

    @Deprecated
    public Boolean useCustomLoginPlugin() {
        return currentLoginPlugin != null;
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
