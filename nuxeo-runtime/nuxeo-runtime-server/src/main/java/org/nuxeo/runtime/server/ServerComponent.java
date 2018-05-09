/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.runtime.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.server.WebApplication;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * This component registers and configures an embedded servlet container.
 * <p>
 * The actual implementation is delegated to a specific embedded server by implementing the interface
 * {@link ServerConfigurator}.
 *
 * @since 10.2
 */
public class ServerComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(ServerComponent.class);

    public static final String XP_WEB_APP = "webapp";

    public static final String XP_SERVLET = "servlet";

    public static final String XP_FILTER = "filter";

    public static final String XP_LISTENER = "listener";

    public static final String PORT_SYSTEM_PROP = "nuxeo.servlet-container.port";

    protected static final String CONFIGURATOR_CLASS = "org.nuxeo.runtime.jetty.JettyServerConfigurator";

    protected ServerConfigurator configurator;

    protected int port;

    @Override
    public void activate(ComponentContext context) {
        try {
            configurator = (ServerConfigurator) Class.forName(CONFIGURATOR_CLASS).getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeServiceException(e);
        }
        // config from test features
        int p = -1;
        String configPort = System.getProperty(PORT_SYSTEM_PROP);
        if (StringUtils.isNotBlank(configPort)) {
            try {
                p = Integer.parseInt(configPort);
            } catch (NumberFormatException e) {
                log.error("Invalid port for embedded servlet container: " + configPort);
            }
        }
        port = configurator.initialize(p);
    }

    @Override
    public void deactivate(ComponentContext context) {
        configurator.close();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_WEB_APP.equals(extensionPoint)) {
            configurator.addWepApp((WebApplication) contribution);
        } else if (XP_FILTER.equals(extensionPoint)) {
            configurator.addFilter((FilterDescriptor) contribution);
        } else if (XP_SERVLET.equals(extensionPoint)) {
            configurator.addServlet((ServletDescriptor) contribution);
        } else if (XP_LISTENER.equals(extensionPoint)) {
            configurator.addLifecycleListener((ServletContextListenerDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // we don't do anything special as this is a test component
    }

    @Override
    public int getApplicationStartedOrder() {
        return -100;
    }

    @Override
    public void start(ComponentContext context) {
        configurator.start();
    }

    @Override
    public void stop(ComponentContext context) {
        configurator.stop();
    }

    /**
     * Gets the port which is used by the server.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

}
