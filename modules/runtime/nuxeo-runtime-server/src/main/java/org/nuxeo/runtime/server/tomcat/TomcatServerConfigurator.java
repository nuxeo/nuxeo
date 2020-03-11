/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.server.tomcat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.nuxeo.common.Environment;
import org.nuxeo.common.server.WebApplication;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.server.FilterDescriptor;
import org.nuxeo.runtime.server.FilterMappingDescriptor;
import org.nuxeo.runtime.server.ServerConfigurator;
import org.nuxeo.runtime.server.ServletContextListenerDescriptor;
import org.nuxeo.runtime.server.ServletDescriptor;

/**
 * Configurator for an embedded Tomcat server.
 *
 * @since 10.2
 */
public class TomcatServerConfigurator implements ServerConfigurator {

    private static final Logger log = LogManager.getLogger(TomcatServerConfigurator.class);

    protected Tomcat tomcat;

    @Override
    public int initialize(int port) {
        tomcat = new Tomcat();
        tomcat.setBaseDir("."); // for tmp dir
        tomcat.setHostname("localhost");
        tomcat.setPort(port);
        Connector connector = tomcat.getConnector();
        connector.setProperty("maxKeepAliveRequests", "1"); // vital for clean shutdown
        connector.setProperty("socket.soReuseAddress", "true");
        log.info("Configuring test Tomcat on port: {}", port);
        return port;
    }

    @Override
    public void close() {
        tomcat = null;
    }

    @Override
    public void start() {
        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new RuntimeServiceException(e);
        }
    }

    @Override
    public void stop() {
        if (tomcat == null) {
            return;
        }
        try {
            tomcat.stop();
            File workDirectory = Paths.get(tomcat.getServer().getCatalinaHome().getAbsolutePath(), "work").toFile();
            FileUtils.deleteDirectory(workDirectory);
            tomcat.destroy();
        } catch (LifecycleException | IOException e) {
            throw new RuntimeServiceException(e);
        }
    }

    @Override
    public void addWepApp(WebApplication descriptor) {
        String contextPath = normalizeContextPath(descriptor.getContextPath());

        File home = Environment.getDefault().getHome();
        File docBase = new File(home, descriptor.getWebRoot());
        docBase.mkdirs(); // make sure the WAR root exists
        Context context = tomcat.addWebapp(contextPath, docBase.getAbsolutePath());
        StandardJarScanner jarScanner = (StandardJarScanner) context.getJarScanner();
        // avoid costly scanning, we register everything explicitly
        jarScanner.setScanManifest(false); // many MANIFEST.MF files have incorrect Class-Path
        jarScanner.setScanAllDirectories(false);
        jarScanner.setScanAllFiles(false);
        jarScanner.setScanBootstrapClassPath(false);
        jarScanner.setScanClassPath(false);
    }

    @Override
    public void addFilter(FilterDescriptor descriptor) {
        String name = descriptor.getName();
        Context context = getContextForPath(descriptor.getContext());
        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName(name);
        filterDef.setDisplayName(descriptor.getDisplayName());
        filterDef.setFilterClass(descriptor.getClazz().getName());
        Map<String, String> initParams = descriptor.getInitParams();
        if (initParams != null) {
            filterDef.getParameterMap().putAll(initParams);
        }
        context.addFilterDef(filterDef);
        for (FilterMappingDescriptor fmd : descriptor.getFilterMappings()) {
            FilterMap filterMap = new FilterMap();
            filterMap.setFilterName(name);
            filterMap.addURLPatternDecoded(fmd.getUrlPattern());
            for (String dispatch : fmd.getDispatchers()) {
                filterMap.setDispatcher(dispatch);
            }
            context.addFilterMap(filterMap);
        }
    }

    @Override
    public void addServlet(ServletDescriptor descriptor) {
        String name = descriptor.getName();
        Context context = getContextForPath(descriptor.getContext());
        // remove existing servlet, to allow overrides (usually to change init params)
        Container previous = context.findChild(name);
        if (previous != null) {
            context.removeChild(previous);
        }
        Wrapper servlet = Tomcat.addServlet(context, name, descriptor.getClazz().getName());
        Map<String, String> initParams = descriptor.getInitParams();
        if (initParams != null) {
            for (Entry<String, String> es : initParams.entrySet()) {
                servlet.addInitParameter(es.getKey(), es.getValue());
            }
        }
        for (String urlPattern : descriptor.getUrlPatterns()) {
            context.addServletMappingDecoded(urlPattern, name);
        }
    }

    @Override
    public void addLifecycleListener(ServletContextListenerDescriptor descriptor) {
        Context context = getContextForPath(descriptor.getContext());
        context.addApplicationListener(descriptor.getClazz().getName());
    }

    protected Context getContextForPath(String contextPath) {
        contextPath = normalizeContextPath(contextPath);
        Context context = (Context) tomcat.getHost().findChild(contextPath);
        if (context == null) {
            context = tomcat.addContext(contextPath, null);
        }
        return context;
    }

    protected String normalizeContextPath(String contextPath) {
        if (contextPath.equals("/")) {
            contextPath = "";
        }
        return contextPath;
    }

}
