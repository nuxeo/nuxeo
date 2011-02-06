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

import java.util.Iterator;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.config.AutoConfigurationService;
import org.nuxeo.runtime.config.ConfigurationFactory;
import org.nuxeo.runtime.config.v1.ConfigurationFactory1;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.remoting.transporter.NuxeoUnMarshaller;
import org.nuxeo.runtime.remoting.transporter.TransporterClient;
import org.nuxeo.runtime.remoting.transporter.TransporterServer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RemotingService extends DefaultComponent {

    public static final String INVOKER_NAME = "nx:service=invoker,name=remoting";

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.runtime.remoting.RemotingService");

    public static final String DEFAULT_LOCATOR = "socket://0.0.0.0:62474/?datatype=nuxeo";

    private TransporterServer transporterServer;

    private Server server;

    private boolean isServer;

    private InvokerLocator serverLocator;

    public static Server connect(String locatorURI) throws Exception {
        return (Server) TransporterClient.createTransporterClient(
                new InvokerLocator(locatorURI), Server.class);
    }

    /**
     * Helper method to connect to a remote nuxeo runtime server.
     *
     * @param host the remote host
     * @param port the remote port
     * @return the server object
     */
    public static Server connect(String host, int port) throws Exception {
        return connect(getServerURI(host, port));
    }

    /**
     * Helper method to disconnect from a remote server.
     */
    public static void disconnect(Server server) {
        TransporterClient.destroyTransporterClient(server);
    }

    /**
     * @deprecated must be removed since from runtime 1.5.1 the invoker protocol
     *             may be configurable
     */
    @Deprecated
    public static String getServerURI(String host, int port) {
        return "socket://" + host + ':' + port + "/?datatype=nuxeo";
    }

    /**
     * Tests the connection with a remote server.
     *
     * @return the product info if successful, null otherwise
     * @deprecated should no more be used - use instead
     *             {@link AutoConfigurationService}
     */
    @Deprecated
    public static String ping(String host, int port) {
        try {
            Server server = connect(host, port);
            try {
                return server.getProductInfo();
            } finally {
                TransporterClient.destroyTransporterClient(server);
            }
        } catch (Throwable t) {
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        // register the configuration handlers
        ConfigurationFactory.registerFactory(new ConfigurationFactory1());
        // register the marshaller
        MarshalFactory.addMarshaller("nuxeo", new SerializableMarshaller(),
                new NuxeoUnMarshaller());
        // startup server if needed
        String val = Framework.getProperty("org.nuxeo.runtime.server.enabled",
                "true");
        isServer = val.equalsIgnoreCase("true");
        if (isServer) {
            String locator = Framework.getProperty(
                    "org.nuxeo.runtime.server.locator", DEFAULT_LOCATOR);
            RuntimeContext runtimeContext = context.getRuntimeContext();
            server = new ServerImpl(this, runtimeContext.getRuntime());
            serverLocator = new InvokerLocator(locator);
            transporterServer = TransporterServer.createTransporterServer(
                    serverLocator, server, Server.class.getName());

            // TODO: the current version of jboss remoting doesn't support
            // locatorUrl on the servlet impl. - see docs
            // when this will be supported ignore registering the mbean and use
            // locatorUrl to retrieve the invoker
            // (this approach is more portable)
            Iterator<?> it = MBeanServerFactory.findMBeanServer(null).iterator();
            if (it.hasNext()) {
                MBeanServer mb = (MBeanServer) it.next();
                if (mb != null) {
                    mb.registerMBean(
                            transporterServer.getConnector().getServerInvoker(),
                            new ObjectName(INVOKER_NAME));
                }
            }
        }
    }

    public InvokerLocator getServerLocator() {
        return serverLocator;
    }

    public TransporterServer getTransporterServer() {
        return transporterServer;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        if (transporterServer != null) {
            transporterServer.stop();
            transporterServer = null;
        }
        serverLocator = null;
        isServer = false;
        server = null;
    }

    public Server getServer() {
        return server;
    }

    public boolean isServer() {
        return isServer;
    }

    public static void main(String[] args) {
        try {
            Server server = connect("servlet://localhost:8080/nuxeo/ServerInvokerServlet");
            Properties props = server.getProperties();
            System.out.println(props);
        } catch (Exception e) {
        }
    }

}
