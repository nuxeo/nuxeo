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
package org.nuxeo.runtime.jetty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletContextListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.FilterMapping;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.xml.XmlConfiguration;
import org.nuxeo.common.Environment;
import org.nuxeo.common.server.WebApplication;
import org.nuxeo.runtime.server.FilterDescriptor;
import org.nuxeo.runtime.server.FilterMappingDescriptor;
import org.nuxeo.runtime.server.ServerConfigurator;
import org.nuxeo.runtime.server.ServletContextListenerDescriptor;
import org.nuxeo.runtime.server.ServletDescriptor;
import org.xml.sax.SAXException;

/**
 * Configurator for an embedded Jetty server.
 *
 * @since 10.2
 */
public class JettyServerConfigurator implements ServerConfigurator {

    private static final Log log = LogFactory.getLog(JettyServerConfigurator.class);

    public static final String JETTY_XML = "jetty/jetty.xml";

    public static final String EMPTY_WEB_XML = "jetty/empty-web.xml";

    public static final String WEB_INF = "WEB-INF";

    public static final String WEB_XML = "web.xml";

    public static final int DEFAULT_PORT = 8080;

    protected Server server;

    // non-root war contexts, which we register before the root war context
    protected ContextHandlerCollection warContexts;

    @Override
    public int initialize(int port) {
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        // this system property is mentioned in jetty.xml to configure the server port
        System.setProperty("jetty.port", String.valueOf(port));

        // apply bundled configuration
        URL cfg = getResource(JETTY_XML);
        XmlConfiguration configuration;
        try {
            configuration = new XmlConfiguration(cfg);
        } catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }
        try {
            server = (Server) configuration.configure();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) { // stupid Jetty API throws Exception
            throw new RuntimeException(e);
        }

        // create the war context handler if needed
        HandlerCollection hc = (HandlerCollection) server.getHandler();
        warContexts = (ContextHandlerCollection) hc.getChildHandlerByClass(ContextHandlerCollection.class);
        if (warContexts == null) {
            // create the war context
            warContexts = new ContextHandlerCollection();
            server.addHandler(warContexts);
        }

        return port;
    }

    @Override
    public void close() {
        server = null;
    }

    @Override
    public void addWepApp(WebApplication app) {
        File home = Environment.getDefault().getHome();

        WebAppContext ctx = new WebAppContext();
        ctx.setContextPath(app.getContextPath());
        String root = app.getWebRoot();
        if (root != null) {
            File file = new File(home, root);
            file.mkdirs(); // make sure the WAR root exists
            ctx.setWar(file.getAbsolutePath());
        }
        String webXml = app.getConfigurationFile();
        if (webXml != null) {
            File file = new File(home, root);
            ctx.setDescriptor(file.getAbsolutePath());
        } else {
            // create an empty web.xml so that the Jetty WebAppContext initializes correctly
            // when scanning for taglibs
            Path webInf = home.toPath().resolve(root).resolve(WEB_INF);
            try {
                Files.createDirectories(webInf);
                try (InputStream is = getResourceAsStream(EMPTY_WEB_XML)) {
                    FileUtils.copyInputStreamToFile(is, webInf.resolve(WEB_XML).toFile());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if ("/".equals(app.getContextPath())) {
            server.addHandler(ctx);
        } else {
            warContexts.addHandler(ctx);
        }
        org.mortbay.log.Log.setLog(new Log4JLogger(log));

        if (ctx.isFailed()) {
            log.error("Error in war deployment");
        }
    }

    protected static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

    protected static InputStream getResourceAsStream(String resource) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }

    @Override
    public void addFilter(FilterDescriptor descriptor) {
        String name = descriptor.getName();
        Context ctx = getContextForPath(descriptor.getContext());
        ServletHandler servletHandler = ctx.getServletHandler();
        FilterHolder filter = new FilterHolder(descriptor.getClazz());
        filter.setName(name);
        filter.setDisplayName(descriptor.getDisplayName());
        filter.setInitParameters(descriptor.getInitParams());
        servletHandler.addFilter(filter);
        for (FilterMappingDescriptor fmd : descriptor.getFilterMappings()) {
            FilterMapping mapping = new FilterMapping();
            mapping.setFilterName(name);
            mapping.setPathSpec(fmd.getUrlPattern());
            int dispatches = Handler.DEFAULT;
            for (String dispatch : fmd.getDispatchers()) {
                dispatches |= FilterHolder.dispatch(dispatch);
            }
            mapping.setDispatches(dispatches);
            servletHandler.addFilterMapping(mapping);
        }
    }

    @Override
    public void addServlet(ServletDescriptor descriptor) {
        String name = descriptor.getName();
        Context ctx = getContextForPath(descriptor.getContext());
        ServletHandler servletHandler = ctx.getServletHandler();
        ServletHolder servlet = new ServletHolder(descriptor.getClazz());
        servlet.setName(name);
        servlet.setDisplayName(descriptor.getDisplayName());
        servlet.setInitParameters(descriptor.getInitParams());
        servletHandler.addServlet(servlet);
        ServletMapping mapping = new ServletMapping();
        mapping.setServletName(name);
        mapping.setPathSpecs(descriptor.getUrlPatterns().toArray(new String[0]));
        servletHandler.addServletMapping(mapping);
    }

    @Override
    public void addLifecycleListener(ServletContextListenerDescriptor descriptor) {
        Context ctx = getContextForPath(descriptor.getContext());
        ServletContextListener listener;
        try {
            listener = descriptor.getClazz().getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot add life cycle listener " + descriptor.getName(), e);
        }
        ctx.addEventListener(listener);
    }

    protected Context getContextForPath(String path) {
        if (path.equals("")) {
            path = "/";
        }
        Context ctx = null;
        // find if there is an existing servlet handler for this path
        for (Handler handler : server.getChildHandlersByClass(Context.class)) {
            Context context = (Context) handler;
            if (path.equals(context.getContextPath())) {
                ctx = context;
                break;
            }
        }
        if (ctx == null) {
            ctx = new Context(server, path, Context.SESSIONS | Context.NO_SECURITY);
        }
        return ctx;
    }

    @Override
    public void start() {
        if (server == null) {
            return;
        }
        Thread t = Thread.currentThread();
        ClassLoader oldcl = t.getContextClassLoader();
        t.setContextClassLoader(getClass().getClassLoader());
        try {
            server.start();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) { // stupid Jetty API throws Exception
            throw new RuntimeException(e);
        } finally {
            t.setContextClassLoader(oldcl);
        }
    }

    @Override
    public void stop() {
        try {
            server.stop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) { // stupid Jetty API throws Exception
            throw new RuntimeException(e);
        }
    }

}
