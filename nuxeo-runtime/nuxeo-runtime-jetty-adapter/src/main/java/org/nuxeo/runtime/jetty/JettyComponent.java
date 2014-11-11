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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.webapp.WebInfConfiguration;
import org.mortbay.jetty.webapp.WebXmlConfiguration;
import org.mortbay.xml.XmlConfiguration;
import org.nuxeo.common.Environment;
import org.nuxeo.common.server.WebApplication;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Contexts are registered like this:
 * If there is a jetty.config the contexts defined there are registered first.
 * If there is no jetty.config a log context will be create programatically and registered first
 * Second an empty collection context is registered. Here will be registered all regular war contexts.
 * Third a the root collection is registered. This way all requests not handled by regular wars are directed to the root war
 * which usually is the webengine war in a nxserver application.  
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JettyComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.runtime.server");
    public static final String XP_WEB_APP = "webapp";
    public static final String XP_DATA_SOURCE = "datasource";

    protected Server server;
    // here we are putting all regular war contexts
    // the root context will be appended after this context collection to be sure the regular contexts are checked first
    // This is because the root context is bound to / so if it is checked first it will consume 
    // all requests even if there is a context that is the target of the request  
    protected ContextHandlerCollection warContexts;
    protected File config;
    protected File log;

    private static final Log logger = LogFactory.getLog(JettyComponent.class);

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
        boolean hasConfigFile = false;
        if (cfg != null) {
            hasConfigFile = true;
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
        
        // if a jetty.xml is present we don't configure logging - this should be done in that file.
        if (!hasConfigFile) {
            RequestLogHandler requestLogHandler = new RequestLogHandler();
            File logDir = Environment.getDefault().getLog();
            logDir.mkdirs();
            File logFile = new File(logDir, "jetty.log");
            NCSARequestLog requestLog = new NCSARequestLog(logFile.getAbsolutePath());
            requestLogHandler.setRequestLog(requestLog);
            //handlers = new Handler[] {contexts, new DefaultHandler(), requestLogHandler};
            server.addHandler(requestLogHandler);
            server.setSendServerVersion(true);
            server.setStopAtShutdown(true);            
        }
        // create the war context
        warContexts = new ContextHandlerCollection();
        server.addHandler(warContexts);
        
        // scan for WAR files        
        // deploy any war found in web directory
        File web = Environment.getDefault().getWeb();
        File[] roots = web.listFiles();
        if (roots != null) {
            for (File root : roots) {
                String name = root.getName();                
                if (name.endsWith(".war")) {
                    name = name.substring(0, name.length()-4);
                    boolean isRoot = "root".equals(name);
                    String ctxPath = isRoot ? "/" : "/"+name;
                    WebAppContext ctx = new WebAppContext(root.getAbsolutePath(), ctxPath);
//                    File defWebXml = new File(Environment.getDefault().getConfig(), "default-web.xml");
//                    if (defWebXml.isFile()) {
//                      ctx.setDefaultsDescriptor(defWebXml.getAbsolutePath());
//                    }
                    ctx.setConfigurations(new Configuration[] {new WebInfConfiguration(), new WebXmlConfiguration()});
                    if (isRoot) {
                        server.addHandler(ctx);
                    } else {
                        warContexts.addHandler(ctx);
                    }
                }
            }
        }
        // start the server
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

            if (app.needsWarPreprocessing()) {
                logger.info("Starting deployment preprocessing");
                DeploymentPreprocessor dp = new DeploymentPreprocessor(home);
                dp.init();
                dp.predeploy();
                logger.info("Deployment preprocessing terminated");
            }

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
            if ("/".equals(app.getContextPath())) { // the root context must be put at the end               
                server.addHandler(ctx);
            } else {
                warContexts.addHandler(ctx);
            }
            org.mortbay.log.Log.setLog(new Log4JLogger(logger));
            ctx.start();
            //HandlerWrapper wrapper = (HandlerWrapper)ctx.getHandler();
            //wrapper = (HandlerWrapper)wrapper.getHandler();
            //wrapper.setHandler(new NuxeoServletHandler());

            if (ctx.isFailed()) {
                logger.error("Error in war deployment");
            }

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
