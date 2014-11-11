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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.config.v1;

import java.util.Map;
import java.util.Properties;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceDescriptor;
import org.nuxeo.runtime.api.ServiceHost;
import org.nuxeo.runtime.api.ServiceManager;
import org.nuxeo.runtime.api.login.LoginService;
import org.nuxeo.runtime.api.login.SecurityDomain;
import org.nuxeo.runtime.config.AutoConfigurationService;
import org.nuxeo.runtime.config.ConfigurationException;
import org.nuxeo.runtime.config.ConfigurationFactory;
import org.nuxeo.runtime.config.ConfigurationHelper;
import org.nuxeo.runtime.config.ServerConfiguration;
import org.nuxeo.runtime.services.streaming.StreamingService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ConfigurationFactory1 extends ConfigurationFactory {

    public static final Version VERSION = new Version(1, 0, 0);

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public ServerConfiguration createConfiguration(InvokerLocator locator, Version version)
            throws ConfigurationException {
        String name = Framework.getProperty("org.nuxeo.runtime.server.name");
        ServerConfiguration1 config = new ServerConfiguration1(locator, name, VERSION);
        config.setProperties(getProperties());
        config.setSecurityDomains(getSecurityDomains());
        // 1. collect streaming information
        StreamingService streaming = (StreamingService) Framework.getRuntime().getComponent(StreamingService.NAME);
        if (streaming.isServer()) {
            // streaming locator is the same as the config locator
            config.setStreamingLocator(locator.getLocatorURI());
        } else {
            String uri = streaming.getServerLocator();
            // normalize streaming locator if needed
            String normalizedUri = ConfigurationHelper.getNormalizedURI(uri, locator.getHost());
            if (normalizedUri != null) {
                uri = normalizedUri;
            }
            // set the streaming locator
            config.setStreamingLocator(uri);
        }

        // 2. collect service bindings information
        config.setServiceBindingsCompat(getServiceBindings());
        config.setServiceHostsCompat(getServiceHosts(locator.getHost()));
        //TODO config.setServiceBindings(bindings);

        // 3. set the client Jndi properties for this server
        //config.setJndiProperties(getJNDIProperties());

        // 4. collect peer information
        //TODO config.setPeers(peers);

        return config;
    }

    public static ServiceDescriptor[] getServiceBindings() {
        ServiceManager sm = Framework.getLocalService(ServiceManager.class);
        return sm.getServiceDescriptors();
    }

    public static ServiceHost[] getServiceHosts(String host) {
        ServiceManager sm = Framework.getLocalService(ServiceManager.class);
        ServiceHost[] serviceHosts = sm.getServers();
        for (ServiceHost shost : serviceHosts) {
            String h = shost.getHost();
            int p = shost.getPort();
            Properties props = shost.getProperties();
            // we need to update jndi props of local server so that the lcient use the right config
            if (h == null) {
                shost.setAddress(host, p); // TODO: 1099?
            } else {
                String newHost = ConfigurationHelper.getNormalizedHost(h, host);
                if (newHost != null) {
                    shost.setAddress(newHost, shost.getPort());
                }
            }
            // if no jndi props specified - use the global ones (the ones in nuxeo.properties)
            if (props == null || props.isEmpty()) {
                updateLocalHostJndiProps(shost);
            }
        }
        return serviceHosts;
    }

    private static void updateLocalHostJndiProps(ServiceHost host) {
        Properties runtimeProps = Framework.getRuntime().getProperties();
        Properties props = AutoConfigurationService.readJndiProperties(runtimeProps);
        if (!props.isEmpty()) {
            host.setProperties(props);
        }
    }

    public static SecurityDomain[] getSecurityDomains() {
        LoginService loginService = Framework.getLocalService(LoginService.class);
        return loginService.getSecurityDomains();
    }

    public static Properties getProperties() {
         Properties props = new Properties();
         Properties rtProps = Framework.getRuntime().getProperties();
         for (Map.Entry<Object, Object> entry : rtProps.entrySet()) {
             String key = entry.getKey().toString();
             String value = Framework.expandVars(entry.getValue().toString());
             props.put(key, value);
         }
         return props;
    }

    public static Properties getJNDIProperties() {
        return AutoConfigurationService.readJndiProperties(Framework.getRuntime().getProperties());
    }

}
