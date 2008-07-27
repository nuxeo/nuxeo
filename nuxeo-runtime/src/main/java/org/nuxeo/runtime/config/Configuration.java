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

package org.nuxeo.runtime.config;

import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceDescriptor;
import org.nuxeo.runtime.api.ServiceHost;
import org.nuxeo.runtime.api.ServiceLocator;
import org.nuxeo.runtime.api.ServiceManager;
import org.nuxeo.runtime.api.login.LoginService;
import org.nuxeo.runtime.api.login.SecurityDomain;
import org.nuxeo.runtime.remoting.RemotingService;
import org.nuxeo.runtime.remoting.Server;
import org.nuxeo.runtime.remoting.transporter.TransporterClient;
import org.nuxeo.runtime.services.streaming.StreamingService;

/**
 * Nuxeo Runtime Configurator.
 * <p>
 * Loads configuration from a remote server and configure the local runtime instance.
 * Configuration includes:
 * <ul>
 * <li> service bindings
 * <li> login configuration
 * <li> streaming configuration
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Configuration {

    //TODO for now REPLACE is implemented only for login config
    public static final int IGNORE  = 0;
    public static final int APPEND  = 1;
    public static final int REPLACE = 2;
    public static final int RESET   = 3;

    private final ServiceManager serviceMgr;
    private final LoginService loginMgr;

    private int updateBindingsOption = RESET;
    private int updateServersOption = RESET;
    private int updateLoginOption = RESET;
    private int updateStreamingOption = RESET;

    // remote server properties
    private Properties properties;


    public Configuration() {
        serviceMgr = Framework.getLocalService(ServiceManager.class);
        loginMgr = Framework.getLocalService(LoginService.class);
    }

    /**
     * Gets the name of the remote server.
     * <p>
     * This information is available only after
     * {@link Configuration#load(String, int)} method was called
     *
     * @return the name.
     */
    public String getName() {
        if (properties == null) {
            throw new IllegalStateException("Configuration was not fetched from the server");
        }
        return properties.getProperty("org.nuxeo.ecm.instance.name");
    }

    /**
     * Gets the description of the remote server.
     * <p>
     * This information is available only after
     * {@link Configuration#load(String, int)} method was called.
     *
     * @return the description.
     */
    public String getDescription() {
        if (properties == null) {
            throw new IllegalStateException("Configuration was not fetched from the server");
        }
        return properties.getProperty("org.nuxeo.ecm.instance.description");
    }

    /**
     * Gets the remote server properties.
     * <p>
     * This information is available only after
     * {@link Configuration#load(String, int)} method was called.
     *
     * @return the properties.
     */
    public Properties getProperties() {
        if (properties == null) {
            throw new IllegalStateException("Configuration was not fetched from the server");
        }
        return properties;
    }

    public void setBindingsUpdateOption(int updateBindingsOption) {
        this.updateBindingsOption = updateBindingsOption;
    }

    public void setLoginUpdateOption(int updateLoginOption) {
        this.updateLoginOption = updateLoginOption;
    }

    public void setServersUpdateOption(int updateServersOption) {
        this.updateServersOption = updateServersOption;
    }

    public void setUpdateStreamingOption(int updateStreamingOption) {
        this.updateStreamingOption = updateStreamingOption;
    }

    public int getUpdateBindingsOption() {
        return updateBindingsOption;
    }

    public int getUpdateLoginOption() {
        return updateLoginOption;
    }

    public int getUpdateServersOption() {
        return updateServersOption;
    }

    public int getUpdateStreamingOption() {
        return updateStreamingOption;
    }

    /**
     * Loads the configuration from a remote server given its address as an URI.
     * <p>
     * URIs are in JBoss remoting format. Example:
     * socket://localhost:62474/nxruntime
     *
     * @param uri the URI
     * @throws Exception
     */
    public void load(String uri) throws Exception {
        InvokerLocator locator = new InvokerLocator(uri);
        Server server = (Server) TransporterClient.createTransporterClient(locator, Server.class);
        try {
            load(server, locator.getHost(), uri);
        } finally {
            TransporterClient.destroyTransporterClient(server);
        }
    }

    /**
     * Loads the configuration from a remote server given its host and port.
     *
     * @param host the host
     * @param port the port
     * @throws Exception
     */
    public void load(String host, int port) throws Exception {
        String serverLocator = RemotingService.getServerURI(host, port);
        Server server = RemotingService.connect(serverLocator);
        try {
            load(server, host, serverLocator);
        } finally {
            TransporterClient.destroyTransporterClient(server);
        }
    }

    void load(Server server, String host, String serverLocator)
            throws Exception {

        if (updateBindingsOption == RESET) {
            serviceMgr.removeServices();
            serviceMgr.removeGroups();
        }
        if (updateServersOption == RESET) {
            serviceMgr.removeServers();
        }
        if (updateLoginOption == RESET) {
            loginMgr.removeSecurityDomains();
        }

        properties = server.getProperties();
        // set up the streaming service
        if (updateStreamingOption != IGNORE) {
            loadStreamingConfig(serverLocator);
        }

        // get service bindings
        if (updateBindingsOption != IGNORE) {
            loadServiceBindings(server);
        }

        // get service locators
        if (updateServersOption != IGNORE) {
            loadServiceHosts(server, host);
        }

        // get login info
        if (updateLoginOption != IGNORE) {
            loadLoginConfig(server);
        }
    }

    private static void loadStreamingConfig(String serverLocator) throws Exception {
        StreamingService streamingService = (StreamingService) Framework.getRuntime().getComponent(
                StreamingService.NAME);
        if (!streamingService.isServer()) { // if this host ignores updating
                                            // streaming config
            String oldLocator = streamingService.getServerLocator();
            if (!serverLocator.equals(oldLocator)) {
                streamingService.stopManager();
                streamingService.setServerLocator(serverLocator);
                streamingService.setServer(false);
                streamingService.startManager();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadLoginConfig(Server server) throws Exception {
        Map<String, Object[][]> domains = server.getSecurityDomains();
        for (Map.Entry<String, Object[][]> entry : domains.entrySet()) {
            String name = entry.getKey();
            SecurityDomain domain = new SecurityDomain(name);
            Object[][] values = entry.getValue();
            AppConfigurationEntry[] appEntries = new AppConfigurationEntry[values.length];
            for (int i = 0; i < values.length; i++) {
                String loginModuleName = values[i][0].toString();
                LoginModuleControlFlag flag = SecurityDomain.controlFlagFromString(values[i][1].toString());
                Map<String, ?> options = (Map<String, ?>) values[i][2];
                appEntries[i] = new AppConfigurationEntry(loginModuleName,
                        flag, options);
            }
            domain.setAppConfigurationEntries(appEntries);
            if (updateLoginOption == REPLACE) {
                loginMgr.addSecurityDomain(domain);
            } else if (null == loginMgr.getSecurityDomain(name)) {
                loginMgr.addSecurityDomain(domain);
            }
        }
    }

    private void loadServiceBindings(Server server) {
        String[] bindings = server.getServiceBindings();
        for (int i = 0; i < bindings.length; i += 4) {
            String group = bindings[i];
            String className = bindings[i + 1];
            String name = bindings[i + 2];
            String locator = bindings[i + 3];
            ServiceDescriptor sd = new ServiceDescriptor(className, group, name);
            sd.setLocator(locator);
            serviceMgr.registerService(sd);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadServiceHosts(Server server, String host) throws Exception {
        for (Properties props : server.getServiceHosts()) {
            String className = (String) props.remove("@class");
            Class<? extends ServiceLocator> klass
                = (Class<? extends ServiceLocator>) Thread.currentThread()
                    .getContextClassLoader().loadClass(className);

            ServiceHost serviceHost = new ServiceHost(klass);
            String[] groups = (String[]) props.remove("@groups");
            if (groups != null) {
                serviceHost.setGroups(groups);
            }
            String addr = (String) props.remove("@host");
            if (addr != null) {
                if (addr.equals("localhost") || addr.equals("127.0.0.1")
                        || addr.equals("0.0.0.0")) {
                    // FIXME: addr value is not used!
                    addr = host; // replace with remote host
                }
                Integer port = (Integer) props.remove("@port");
                serviceHost.setAddress(host, port != null ? port : 0);
            }
            serviceHost.setProperties(props);
            serviceMgr.registerServer(serviceHost);
        }
    }

}
