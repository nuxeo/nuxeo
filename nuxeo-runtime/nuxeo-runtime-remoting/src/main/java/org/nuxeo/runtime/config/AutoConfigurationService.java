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

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceManager;
import org.nuxeo.runtime.api.login.LoginService;
import org.nuxeo.runtime.remoting.RemotingService;
import org.nuxeo.runtime.remoting.Server;
import org.nuxeo.runtime.remoting.transporter.TransporterClient;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AutoConfigurationService {

    ServerConfiguration config; // last loaded server config

    final ServiceManager serviceMgr;
    final LoginService loginMgr;
    final RemotingService remoting;
    final Version version = new Version(1,0,0);

    public AutoConfigurationService() {
        remoting = Framework.getLocalService(RemotingService.class);
        serviceMgr = Framework.getLocalService(ServiceManager.class);
        loginMgr = Framework.getLocalService(LoginService.class);
    }

    public Version getVersion() {
        return version;
    }

    public void clear() {
        serviceMgr.removeServices();
        serviceMgr.removeGroups();
        serviceMgr.removeServers();
        loginMgr.removeSecurityDomains();
    }

    public void load(String uri) throws Exception {
        load(new InvokerLocator(uri));
    }

    public void load(String protocol, String host, int port) throws Exception {
        Map<String,String> params = new HashMap<String,String>();
        params.put("datatype", "nuxeo");
        load (new InvokerLocator(protocol, host, port, "/", params));
    }

    public void load(String host, int port) throws Exception {
        load("socket", host, port);
    }

    public void load(InvokerLocator locator) throws Exception {
        Server server = null;
        try {
            server = (Server) TransporterClient.createTransporterClient(locator, Server.class);
            try {
                // get the configuration based on the client remoting version and the
                // locator used to access the server
                clear();
                config = server.getConfiguration(locator, version);
                config.install();
            } catch (Exception e) { // compatibility code - for runtime that doesn't supports ServerConfiguration
                loadCompat(server, locator);
            }
        } finally {
            if (server != null) {
                TransporterClient.destroyTransporterClient(server);
            }
        }
    }

    /**
     * Gets the currently connected e server config.
     *
     * @return the server config or null if no server was connected yet
     */
    public ServerConfiguration getServerConfiguration() {
        return config;
    }

    protected static void loadCompat(Server server, InvokerLocator locator) throws Exception {
        new Configuration().load(server, locator.getHost(), locator.getLocatorURI());
    }

    private static final String  JNDI_PREFIX = "nuxeo-client-jndi.";

    public static Properties readJndiProperties(Properties properties) {
        Properties jndiProperties = new Properties();
        int prefixLen = JNDI_PREFIX.length();
        for (Map.Entry<Object,Object> entry :  properties.entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith(JNDI_PREFIX)) {
                jndiProperties.put(key.substring(prefixLen), entry.getValue());
            }
        }
        return jndiProperties;
    }

    @SuppressWarnings("unchecked")
    public static InvokerLocator createLocator(String url) throws MalformedURLException {
        InvokerLocator locator = new InvokerLocator(url);
        return createLocator(locator.getProtocol(), locator.getHost(),
                locator.getPort(), locator.getPath(), locator.getParameters());
    }

    public static InvokerLocator createLocator(String host, int port) {
        return createLocator("socket", host, port, "", null);
    }

    public static InvokerLocator createLocator(String protocol, String host, int port) {
        return createLocator(protocol, host, port, "", null);
    }

    public static InvokerLocator createLocator(String protocol, String host,
            int port, String path) {
        return createLocator(protocol, host, port, path, null);
    }

    public static InvokerLocator createLocator(String protocol, String host,
            int port, String path, Map<String, String> params) {
        if (params == null) {
            params = new HashMap<String, String>();
            params.put(InvokerLocator.DATATYPE, "nuxeo");
        }
        return new InvokerLocator(protocol, host, port, path, params);
    }

}
