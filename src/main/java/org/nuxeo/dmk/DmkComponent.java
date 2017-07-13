/*
 * (C) Copyright 2013-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     slacoin
 */
package org.nuxeo.dmk;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.sun.jdmk.comm.AuthInfo;
import com.sun.jdmk.comm.GenericHttpConnectorServer;
import com.sun.jdmk.comm.HtmlAdaptorServer;
import com.sun.jdmk.comm.internal.JDMKServerConnector;

public class DmkComponent extends DefaultComponent {

    protected final Map<String, DmkProtocol> configs = new HashMap<>();

    protected HtmlAdaptorServer htmlAdaptor;

    protected JDMKServerConnector httpConnector;

    protected JDMKServerConnector httpsConnector;

    protected final Log log = LogFactory.getLog(DmkComponent.class);

    protected final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    protected HtmlAdaptorServer newAdaptor(DmkProtocol config) {
        HtmlAdaptorServer adaptor = new HtmlAdaptorServer();
        adaptor.addUserAuthenticationInfo(new AuthInfo(config.user, config.password));
        adaptor.setPort(config.port);
        try {
            ObjectName name = new ObjectName("org.nuxeo:type=jmx-adaptor,format=html");
            mbs.registerMBean(adaptor, name);
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
        return adaptor;
    }

    protected void destroyAdaptor(HtmlAdaptorServer adaptor) {
        try {
            ObjectName name = new ObjectName("org.nuxeo:type=jmx-adaptor,format=html");
            mbs.unregisterMBean(name);
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
        if (!adaptor.isActive()) {
            return;
        }
        adaptor.stop();
    }

    protected JDMKServerConnector newConnector(DmkProtocol config) {
        try {
            String protocol = "jdmk-".concat(config.name);
            JMXServiceURL httpURL = new JMXServiceURL(protocol, null, config.port);
            JDMKServerConnector connector = (JDMKServerConnector) JMXConnectorServerFactory.newJMXConnectorServer(
                    httpURL, null, mbs);
            GenericHttpConnectorServer server = (GenericHttpConnectorServer) connector.getWrapped();
            server.addUserAuthenticationInfo(new AuthInfo(config.user, config.password));
            ObjectName name = new ObjectName("org.nuxeo:type=jmx-connector,protocol=".concat(protocol));
            mbs.registerMBean(connector, name);
            return connector;
        } catch (JMException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void destroyConnector(JDMKServerConnector connector) {
        String protocol = connector.getAddress().getProtocol();
        try {
            ObjectName name = new ObjectName("org.nuxeo:type=jmx-connector,protocol=".concat(protocol));
            mbs.unregisterMBean(name);
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
        if (!connector.isActive()) {
            return;
        }
        try {
            connector.stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(ComponentContext context) {
        if (configs.containsKey("html")) {
            htmlAdaptor = newAdaptor(configs.get("html"));
            log.info("JMX HTML adaptor available at port 8081 (not active, to be started in JMX console)");
        }
        if (configs.containsKey("http")) {
            httpConnector = newConnector(configs.get("http"));
            log.info("JMX HTTP connector available at " + httpConnector.getAddress()
                    + " (not active, to be started in JMX console)");
        }
        if (configs.containsKey("https")) {
            httpsConnector = newConnector(configs.get("https"));
            log.info("JMX HTTPS connector available at " + httpConnector.getAddress()
                    + " (not active, to be started in JMX console)");
        }
    }

    @Override
    public void stop(ComponentContext arg0) {
        if (htmlAdaptor != null) {
            try {
                destroyAdaptor(htmlAdaptor);
            } finally {
                htmlAdaptor = null;
            }
        }

        if (httpConnector != null) {
            try {
                destroyConnector(httpConnector);
            } finally {
                httpConnector = null;
            }
        }

        if (httpsConnector != null) {
            try {
                destroyConnector(httpsConnector);
            } finally {
                httpsConnector = null;
            }
        }

    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("protocols".equals(extensionPoint)) {
            DmkProtocol protocol = (DmkProtocol) contribution;
            configs.put(protocol.name, protocol);
        }
    }

}
