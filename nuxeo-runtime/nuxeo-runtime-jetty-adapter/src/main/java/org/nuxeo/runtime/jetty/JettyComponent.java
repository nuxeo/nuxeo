/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.jetty;

import java.io.File;
import java.net.URL;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.xml.XmlConfiguration;
import org.nuxeo.common.Environment;
import org.nuxeo.common.server.WebApplication;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JettyComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.runtime.server");
    public static final String XP_WEB_APP = "webapp";
    public static final String XP_DATA_SOURCE = "datasource";

    protected Server server;
    protected ContextHandlerCollection contexts = new ContextHandlerCollection();
    protected File config;
    protected File log;

    public Server getServer() {
        return server;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        // apply bundled configuration
        URL cfg = null;

        String cfgName = Framework.getProperty("org.nuxeo.jetty.config");
        if (cfgName != null) {
            if (cfgName.contains(":/")) {
                cfg = new URL(cfgName);
            } else { // assume a file
                File file = new File(cfgName);
                if (file.isFile()) {
                    cfg = file.toURI().toURL();
                }
            }
        } else {
            File file = new File(Environment.getDefault().getConfig(), "jetty.xml");
            if (file.isFile()) {
                cfg = file.toURI().toURL();
            }
        }
        if (cfg != null) {
            XmlConfiguration configuration = new XmlConfiguration(cfg);
            server = (Server)configuration.configure();
        } else {
            int p = 8080;
            String port = Environment.getDefault().getProperty("http_port");
            if (port != null) {
                try {
                p = Integer.parseInt(port);
                } catch (NumberFormatException e) {
                    // do noting
                }
            }
            server = new Server(p);
        }
        Handler[] handlers = server.getHandlers();
        if (handlers != null && handlers.length > 0 && handlers[0] instanceof ContextHandlerCollection) {
            contexts = (ContextHandlerCollection)handlers[0];
        } else if (handlers == null || handlers.length == 0) {
            // dynamic configuration
            contexts = new ContextHandlerCollection();
            RequestLogHandler requestLogHandler = new RequestLogHandler();
            File logDir = Environment.getDefault().getLog();
            logDir.mkdirs();
            File logFile = new File(logDir, "jetty.log");
            NCSARequestLog requestLog = new NCSARequestLog(logFile.getAbsolutePath());
            requestLogHandler.setRequestLog(requestLog);
            //handlers = new Handler[] {contexts, new DefaultHandler(), requestLogHandler};
            handlers = new Handler[] {contexts, requestLogHandler};
            server.setHandlers(handlers);
            server.setSendServerVersion(true);
            server.setStopAtShutdown(true);
        } else { // add only the contexts handler
            contexts = new ContextHandlerCollection();
            Handler[] newHandlers = new Handler[handlers.length+1];
            newHandlers[0] = contexts;
            System.arraycopy(handlers, 0, newHandlers, 0, handlers.length);
            server.setHandlers(newHandlers);
        }
        server.start();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        server.stop();
        server = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_WEB_APP.equals(extensionPoint)) {
            File home = Environment.getDefault().getHome();
            WebApplication app = (WebApplication)contribution;
            WebAppContext ctx = new WebAppContext();
            ctx.setContextPath(app.getContextPath());
            String root = app.getWebRoot();
            if (root != null) {
                File file = new File(home, root);
                ctx.setWar(file.getAbsolutePath());
            }
            String webXml = app.getConfigurationFile();
            if (webXml != null) {
                File file = new File(home, root);
                ctx.setDescriptor(file.getAbsolutePath());
            }
            File defWebXml = new File(Environment.getDefault().getConfig(), "default-web.xml");
            if (defWebXml.isFile()) {
              ctx.setDefaultsDescriptor(defWebXml.getAbsolutePath());
            }
            contexts.addHandler(ctx);
            ctx.start();
        } else if (XP_DATA_SOURCE.equals(extensionPoint)) {

        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_WEB_APP.equals(extensionPoint)) {

        } else if (XP_DATA_SOURCE.equals(extensionPoint)) {

        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == org.mortbay.jetty.Server.class) {
            return adapter.cast(server);
        }
        return null;
    }

}
