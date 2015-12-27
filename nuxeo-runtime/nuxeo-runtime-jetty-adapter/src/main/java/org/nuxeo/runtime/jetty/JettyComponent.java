/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.jetty;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.webapp.WebInfConfiguration;
import org.mortbay.jetty.webapp.WebXmlConfiguration;
import org.mortbay.xml.XmlConfiguration;
import org.nuxeo.common.Environment;
import org.nuxeo.common.server.WebApplication;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.xml.sax.SAXException;

/**
 * This component registers and configures an embedded Jetty server.
 * <p>
 * Contexts are registered like this:
 * <p>
 * First, if there is a {@code jetty.xml} config file, the contexts defined there are registered first; if there is no
 * {@code jetty.xml}, a log context will be create programatically and registered first.
 * <p>
 * Second an empty collection context is registered. Here will be registered all regular war contexts.
 * <p>
 * Third, the root collection is registered. This way all requests not handled by regular wars are directed to the root
 * war, which usually is the webengine war in a nxserver application.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JettyComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.runtime.server");

    public static final String XP_WEB_APP = "webapp";

    public static final String XP_SERVLET = "servlet";

    public static final String XP_FILTER = "filter";

    public static final String XP_LISTENERS = "listeners";

    public static final String P_SCAN_WEBDIR = "org.nuxeo.runtime.jetty.scanWebDir";

    protected Server server;

    protected ContextManager ctxMgr;

    // here we are putting all regular war contexts
    // the root context will be appended after this context collection to be
    // sure the regular contexts are checked first
    // This is because the root context is bound to / so if it is checked first
    // it will consume
    // all requests even if there is a context that is the target of the request
    protected ContextHandlerCollection warContexts;

    protected File config;

    protected File log;

    private static final Log logger = LogFactory.getLog(JettyComponent.class);

    public Server getServer() {
        return server;
    }

    @Override
    public void activate(ComponentContext context) {

        // apply bundled configuration
        URL cfg = null;

        String cfgName = Framework.getProperty("org.nuxeo.jetty.config");
        if (cfgName != null) {
            if (cfgName.contains(":/")) {
                try {
                    cfg = new URL(cfgName);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            } else { // assume a file
                File file = new File(cfgName);
                if (file.isFile()) {
                    try {
                        cfg = file.toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            File file = new File(Environment.getDefault().getConfig(), "jetty.xml");
            if (file.isFile()) {
                try {
                    cfg = file.toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        boolean hasConfigFile = false;
        if (cfg != null) {
            hasConfigFile = true;
            XmlConfiguration configuration;
            try {
                configuration = new XmlConfiguration(cfg);
            } catch (SAXException | IOException e) {
                throw new RuntimeException(e);
            }
            try {
                server = (Server) configuration.configure();
            } catch (Exception e) { // stupid Jetty API throws Exception
                throw ExceptionUtils.runtimeException(e);
            }
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

        // if a jetty.xml is present we don't configure logging - this should be
        // done in that file.
        if (!hasConfigFile) {
            RequestLogHandler requestLogHandler = new RequestLogHandler();
            File logDir = Environment.getDefault().getLog();
            logDir.mkdirs();
            File logFile = new File(logDir, "jetty.log");
            NCSARequestLog requestLog = new NCSARequestLog(logFile.getAbsolutePath());
            requestLogHandler.setRequestLog(requestLog);
            // handlers = new Handler[] {contexts, new DefaultHandler(),
            // requestLogHandler};
            server.addHandler(requestLogHandler);
            server.setSendServerVersion(true);
            server.setStopAtShutdown(true);

        }

        // create the war context handler if needed
        HandlerCollection hc = (HandlerCollection) server.getHandler();
        warContexts = (ContextHandlerCollection) hc.getChildHandlerByClass(ContextHandlerCollection.class);
        if (warContexts == null) {
            // create the war context
            warContexts = new ContextHandlerCollection();
            server.addHandler(warContexts);
        }

        // scan for WAR files
        // deploy any war found in web directory
        String scanWebDir = Framework.getProperty(P_SCAN_WEBDIR);
        if (scanWebDir != null && scanWebDir.equals("true")) {
            logger.info("Scanning for WARs in web directory");
            File web = Environment.getDefault().getWeb();
            scanForWars(web);
        }

        ctxMgr = new ContextManager(server);

        // start the server
        // server.start(); -> server will be start after frameworks starts to be
        // sure that all services
        // used by web.xml filters are registered.
    }

    @Override
    public void deactivate(ComponentContext context) {
        ctxMgr = null;
        try {
            server.stop();
        } catch (Exception e) { // stupid Jetty API throws Exception
            throw ExceptionUtils.runtimeException(e);
        }
        server = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_WEB_APP.equals(extensionPoint)) {
            File home = Environment.getDefault().getHome();
            WebApplication app = (WebApplication) contribution;
            // TODO preprocessing was removed from this component -
            // preprocessing should be done in another bundle
            // if still required (on equinox distribution)
            // if (app.needsWarPreprocessing()) {
            // logger.info("Starting deployment preprocessing");
            // DeploymentPreprocessor dp = new DeploymentPreprocessor(home);
            // dp.init();
            // dp.predeploy();
            // logger.info("Deployment preprocessing terminated");
            // }

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
            if ("/".equals(app.getContextPath())) { // the root context must be
                                                    // put at the end
                server.addHandler(ctx);
            } else {
                warContexts.addHandler(ctx);
            }
            org.mortbay.log.Log.setLog(new Log4JLogger(logger));
            // ctx.start();
            // HandlerWrapper wrapper = (HandlerWrapper)ctx.getHandler();
            // wrapper = (HandlerWrapper)wrapper.getHandler();
            // wrapper.setHandler(new NuxeoServletHandler());

            if (ctx.isFailed()) {
                logger.error("Error in war deployment");
            }

        } else if (XP_FILTER.equals(extensionPoint)) {
            ctxMgr.addFilter((FilterDescriptor) contribution);
        } else if (XP_SERVLET.equals(extensionPoint)) {
            ctxMgr.addServlet((ServletDescriptor) contribution);
        } else if (XP_LISTENERS.equals(extensionPoint)) {
            ctxMgr.addLifecycleListener((ServletContextListenerDescriptor) contribution);
        }
    }

    public ContextManager getContextManager() {
        return ctxMgr;
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_WEB_APP.equals(extensionPoint)) {

        } else if (XP_FILTER.equals(extensionPoint)) {
            ctxMgr.removeFilter((FilterDescriptor) contribution);
        } else if (XP_SERVLET.equals(extensionPoint)) {
            ctxMgr.removeServlet((ServletDescriptor) contribution);
        } else if (XP_LISTENERS.equals(extensionPoint)) {
            ctxMgr.removeLifecycleListener((ServletContextListenerDescriptor) contribution);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == org.mortbay.jetty.Server.class) {
            return adapter.cast(server);
        }
        return null;
    }

    // let's nuxeo runtime get access to the JNDI context
    // injected through the JettyTransactionalListener
    protected ClassLoader nuxeoCL;

    public void setNuxeoClassLoader(ClassLoader cl) {
        nuxeoCL = cl;
    }

    protected ClassLoader getClassLoader(ClassLoader cl) {
        if (!Boolean.valueOf(System.getProperty("org.nuxeo.jetty.propagateNaming"))) {
            return cl;
        }
        if (nuxeoCL == null) {
            return cl;
        }
        return nuxeoCL;
    }

    @Override
    public int getApplicationStartedOrder() {
        return -100;
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        if (server == null) {
            return;
        }
        ctxMgr.applyLifecycleListeners();
        Thread t = Thread.currentThread();
        ClassLoader oldcl = t.getContextClassLoader();
        t.setContextClassLoader(getClass().getClassLoader());
        try {
            server.start();
        } catch (Exception e) { // stupid Jetty API throws Exception
            throw ExceptionUtils.runtimeException(e);
        } finally {
            t.setContextClassLoader(getClassLoader(oldcl));
        }
    }

    private void scanForWars(File dir) {
        scanForWars(dir, "");
    }

    private void scanForWars(File dir, String basePath) {
        File[] roots = dir.listFiles();
        if (roots != null) {
            for (File root : roots) {
                String name = root.getName();
                if (name.endsWith(".war")) {
                    logger.info("Found war: " + name);
                    name = name.substring(0, name.length() - 4);
                    boolean isRoot = "root".equals(name);
                    String ctxPath = isRoot ? "/" : basePath + "/" + name;
                    WebAppContext ctx = new WebAppContext(root.getAbsolutePath(), ctxPath);
                    ctx.setConfigurations(new Configuration[] { new WebInfConfiguration(), new WebXmlConfiguration() });
                    if (isRoot) {
                        server.addHandler(ctx);
                    } else {
                        warContexts.addHandler(ctx);
                    }
                } else if (root.isDirectory()) {
                    scanForWars(root, basePath + "/" + name);
                }
            }
        }
    }

}
