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
 * $Id$
 */

package org.nuxeo.runtime.remoting;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.loading.ClassUtil;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceDescriptor;
import org.nuxeo.runtime.api.ServiceGroup;
import org.nuxeo.runtime.api.ServiceHost;
import org.nuxeo.runtime.api.ServiceManager;
import org.nuxeo.runtime.api.login.LoginService;
import org.nuxeo.runtime.api.login.SecurityDomain;
import org.nuxeo.runtime.config.ConfigurationException;
import org.nuxeo.runtime.config.ConfigurationFactory;
import org.nuxeo.runtime.config.ServerConfiguration;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServerImpl implements Server {

    private static final Log log = LogFactory.getLog(ServerImpl.class);

    private final RuntimeService runtime;
    private final RemotingService remoting;


    public ServerImpl(RemotingService service, RuntimeService runtime) {
        remoting = service;
        this.runtime = runtime;
    }

    @Override
    public String getName() {
        String name = Framework.getRuntime().getProperty("org.nuxeo.ecm.instance.name");
        if (name == null) {
            name = "Nuxeo Runtime Server";
        }
        return name;
    }

    @Override
    public String getProductInfo() {
        String name = Framework.getRuntime().getProperty("org.nuxeo.ecm.product.name");
        String version = Framework.getRuntime().getProperty("org.nuxeo.ecm.product.version");
        if (name == null) {
            name = "Nuxeo Runtime Server";
            version = runtime.getVersion().toString();
        } else if (version == null) {
            version = "0.0.0";
        }
        return name + ' ' + version;
    }

    @Override
    public String[] getServiceBindings() {
        ServiceManager sm = Framework.getLocalService(ServiceManager.class);
        ServiceDescriptor[] services = sm.getServiceDescriptors();
        List<String> result = new ArrayList<String>();
        for (ServiceDescriptor sd : services) {
            result.add(sd.getGroup().getName());
            result.add(sd.getServiceClassName());
            result.add(sd.getName());
            result.add(sd.getLocator());
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public Properties[] getServiceHosts() throws Exception {
        ServiceManager sm = Framework.getLocalService(ServiceManager.class);
        ServiceHost[] servers = sm.getServers();
        List<Properties> result = new ArrayList<Properties>(servers.length);
        for (ServiceHost server : servers) {
            Properties value = server.getProperties();
            if (value == null) {
                value = new Properties();
            }
            String host = server.getHost();
            if (host != null) {
                value.put("@host", host);
                value.put("@port", server.getPort());
            }
            ServiceGroup[] groups = server.getGroups();
            if (groups.length > 0) {
                String[] names = new String[groups.length];
                for (int i = 0; i < groups.length; i++) {
                    names[i] = groups[i].getName();
                }
                value.put("@groups", names);
            }
            value.put("@class", server.getServiceLocator().getClass().getName());
            result.add(value);
        }
        return result.toArray(new Properties[result.size()]);
    }

    @Override
    public Map<String, Object[][]> getSecurityDomains() throws Exception {
        LoginService loginService = Framework.getLocalService(LoginService.class);
        Map<String, Object[][]> result = new HashMap<String, Object[][]>();
        for (SecurityDomain domain :  loginService.getSecurityDomains()) {
            AppConfigurationEntry[] entries = domain.getAppConfigurationEntries();
            if (entries != null && entries.length > 0) {
                String key = domain.getName();
                Object[][] value = new Object[entries.length][3];
                for (int i = 0; i < entries.length; i++) {
                    value[i][0] = entries[i].getLoginModuleName();
                    value[i][1] = SecurityDomain.controlFlagToString(entries[i].getControlFlag());
                    value[i][2] = entries[i].getOptions();
                }
                result.put(key, value);
            }
        }
        return result;
    }

    @Override
    public Properties getProperties() {
         Properties props = new Properties();
         Properties rtProps = Framework.getRuntime().getProperties();
         for (Map.Entry<Object, Object> entry : rtProps.entrySet()) {
             String key = entry.getKey().toString();
             String value = Framework.expandVars(entry.getValue().toString());
             props.put(key, value);
         }
         return props;
    }

    @Override
    public ComponentName[] getComponents() {
        Collection<RegistrationInfo> regs = Framework.getRuntime().getComponentManager().getRegistrations();
        List<ComponentName> comps = new ArrayList<ComponentName>();
        for (RegistrationInfo ri : regs) {
            comps.add(ri.getName());
        }
        return comps.toArray(new ComponentName[comps.size()]);
    }

    @Override
    public boolean hasComponent(ComponentName name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Collection<ComponentInstance> getActiveComponents() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ComponentInstance getComponent(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ComponentInstance getComponent(ComponentName name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription() {
        return runtime.getDescription();
    }

    @Override
    public Collection<RegistrationInfo> getRegistrations() {
        return runtime.getComponentManager().getRegistrations();
    }

    @Override
    public String getServerAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServerConfiguration getConfiguration(InvokerLocator locator, Version version)
            throws ConfigurationException {
        ConfigurationFactory factory = ConfigurationFactory.getFactory(version);
        return factory.createConfiguration(locator, version);
    }

    public void contributeExtension(Extension extension, String xmlContent) throws Exception {
//        ComponentManagerImpl mgr = (ComponentManagerImpl)runtime.getComponentManager();
//        mgr.getRegistrationInfo(extension.getTargetComponent());
//        mgr.loadContributions(ri, xt);
//        extension.
    }

    // ------------------- resource loader handler ------------

    @Override
    public byte[] getLocalResource(ComponentName component, String name) {
        return getResource(component, name);
    }

    @Override
    public byte[] getResource(ComponentName component, String name) {
        log.info("Loading resource: " + name + " using " + component
                + " context");
        ComponentInstance ci = Framework.getRuntime().getComponentInstance(component);
        if (ci == null) {
            return null;
        }
        URL url = ci.getContext().getResource(name);
        if (url != null) {
            try {
                return FileUtils.readBytes(url);
            } catch (IOException e) {
                log.error("Failed to load resource: " + name, e);
            }
        }
        return null;
    }

    @Override
    public byte[] getClass(ComponentName component, String name) {
        log.info("Loading class: " + name + " using " + component + " context");
        ComponentInstance ci = Framework.getRuntime().getComponentInstance(component);
        if (ci == null) {
            return null;
        }
        String resourceName = getClassResource(name);
        URL classUrl = ci.getContext().getResource(resourceName);
        if (classUrl != null) {
            try {
                return FileUtils.readBytes(classUrl);
            } catch (IOException e) {
                log.error("Failed to load class " + name, e);
            }
        }
        return null;
    }

    private static String getClassResource(String className) {
        String cn;
        if (ClassUtil.isArrayClass(className)) {
            /*
             * if requesting an array, of course, that would be found in our
             * class path, so we need to strip the class data and just return
             * the class part, the other side will properly load the class as an
             * array.
             */
            cn = ClassUtil.getArrayClassPart(className).replace('.', '/')
                    + ".class";
        } else {
            cn = className.replace('.', '/') + ".class";
        }
        return cn;
    }

}
