/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine.gwt.dev;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.dev.NuxeoApp;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.webengine.gwt.GwtBundleActivator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoLauncher extends NuxeoAuthenticationFilter {

    private static final Log log = LogFactory.getLog(NuxeoLauncher.class);

    protected static NuxeoApp app;

    protected static RedirectService redirect;

    /**
     * You can overwrite this to add your own pom artifacts in the graph.
     */
    protected void initializeGraph(NuxeoApp app) {
        // app.addPom("org.my", "my-artifact", "1.0", 1);
    }

    protected void buildDone(NuxeoApp app) {
    }

    protected void aboutToStartFramework(NuxeoApp app) {
    }

    /**
     * You can overwrite this add custom initialization after nuxeo started.
     */
    protected void frameworkStarted(NuxeoApp app) {
        // do nothing
    }

    /**
     * Gets a custom configuration for the Nuxeo to build. By default no custom
     * configuration is used - but built-in configuration selected through
     * profiles.
     *
     * @return null if no custom configuration is wanted.
     */
    protected URL getConfiguration() {
        return null;
    }

    /**
     * Override this if you don't want to cache the nuxeo build.
     */
    protected boolean useCache() {
        return true;
    }

    @Override
    public synchronized void init(FilterConfig config) throws ServletException {
        if (app != null) {
            return;
        }
        System.setProperty(GwtBundleActivator.GWT_DEV_MODE_PROP, "true");
        app = createApplication(config);

        Runtime.getRuntime().addShutdownHook(
                new Thread("Nuxeo Server Shutdown") {
                    @Override
                    public void run() {
                        try {
                            if (app != null) {
                                app.shutdown();
                            }
                        } catch (Exception e) {
                            log.error(e, e);
                        }
                    }
                });

        super.init(config);
    }

    protected NuxeoApp createApplication(FilterConfig config)
            throws ServletException {
        URL cfg = getConfiguration();
        String homeParam = config.getInitParameter("home");
        String hostParam = config.getInitParameter("host");
        String portParam = config.getInitParameter("port");
        String profileParam = config.getInitParameter("profile");
        String updatePolicy = config.getInitParameter("updatePolicy");
        String offline = config.getInitParameter("offline");
        String isolated = config.getInitParameter("isolated");
        boolean isIsolated = Boolean.parseBoolean(isolated);

        String redirectPrefix = config.getInitParameter("redirectPrefix");
        String redirectTrace = config.getInitParameter("redirectTrace");
        String redirectTraceContent = config.getInitParameter("redirectTraceContent");

        File home = null;
        String host = hostParam == null ? "localhost" : null;
        int port = portParam == null ? 8081 : Integer.parseInt(portParam);
        String profile = profileParam == null ? NuxeoApp.CORE_SERVER_541_SNAPSHOT
                : profileParam;
        if (homeParam == null) {
            String userDir = System.getProperty("user.home");
            String sep = userDir.endsWith("/") ? "" : "/";
            home = new File(userDir + sep + ".nxserver-gwt");
        } else {
            homeParam = StringUtils.expandVars(homeParam,
                    System.getProperties());
            home = new File(homeParam);
        }

        // start redirect service
        redirect = new RedirectService(host, port);
        if (redirectPrefix != null) {
            redirect.setRedirectPrefix(redirectPrefix);
        }
        if (redirectTrace != null && Boolean.parseBoolean(redirectTrace)) {
            redirect.setTrace(true);
        }
        if (redirectTraceContent != null
                && Boolean.parseBoolean(redirectTraceContent)) {
            redirect.setTrace(true);
            redirect.setTraceContent(true);
        }

        System.out.println("+---------------------------------------------------------");
        System.out.println("| Nuxeo Server Profile: "
                + (profile == null ? "custom" : profile));
        System.out.println("| Home Directory: " + home);
        System.out.println("| HTTP server at: " + host + ":" + port);
        System.out.println("| Use cache: " + useCache()
                + "; Snapshot update policy: " + updatePolicy + "; offline: "
                + offline);
        System.out.println("+---------------------------------------------------------\n");

        NuxeoApp.setHttpServerAddress(host, port);

        try {
            MyNuxeoApp app = new MyNuxeoApp(home, null, isIsolated);
            if (updatePolicy != null) {
                app.setUpdatePolicy(updatePolicy);
            }
            if (offline != null) {
                app.setOffline(Boolean.parseBoolean(offline));
            }
            if (cfg == null) {
                app.build(profile, useCache());
            } else {
                app.build(cfg, useCache());
            }
            System.setProperty("java.naming.factory.initial",
                    "org.nuxeo.runtime.jtajca.NamingContextFactory");
            System.setProperty("java.naming.factory.url.pkgs",
                    "org.nuxeo.runtime.jtajca");
            app.start();
            return app;
        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

    public class MyNuxeoApp extends NuxeoApp {
        public MyNuxeoApp(File home) throws Exception {
            super(home, null, false);
        }

        public MyNuxeoApp(File home, ClassLoader cl) throws Exception {
            super(home, cl, false);
        }

        public MyNuxeoApp(File home, ClassLoader cl, boolean isIsolated)
                throws Exception {
            super(home, cl, isIsolated);
        }

        @Override
        protected void initializeGraph() throws Exception {
            super.initializeGraph();
            NuxeoLauncher.this.initializeGraph(this);
        }

        @Override
        protected void aboutToStartFramework() throws Exception {
            super.aboutToStartFramework();
            NuxeoLauncher.this.aboutToStartFramework(this);
        }

        @Override
        protected void buildDone() {
            super.buildDone();
            NuxeoLauncher.this.buildDone(this);
        }

        @Override
        protected void frameworkStarted() throws Exception {
            super.frameworkStarted();
            NuxeoLauncher.this.frameworkStarted(this);
        }
    }

    /**
     * Checks for calls to nuxeo server to redirect them to avoid SOP errors.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (httpRequest.getRequestURI().startsWith(redirect.getRedirectPrefix())) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            redirect.redirect(httpRequest, httpResponse);
            return;
        }
        super.doFilter(request, response, chain);
    }
}
