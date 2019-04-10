/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.webdav;

import java.io.File;
import java.lang.reflect.Field;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.ws.rs.core.Application;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.webengine.app.WebEngineFilter;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContextFilter;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionCleanupFilter;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.sun.jersey.api.core.ApplicationAdapter;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * @since 5.8
 */
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.ecm.platform.filemanager.api")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.filemanager.core.listener")
public class WebDavServerFeature extends WebEngineFeature {

    protected static final int PORT = 9998;

    protected static final String HOST = "localhost";

    protected static final String CONTEXT = "/";

    protected Tomcat tomcat;

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        setUpTomcat();
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        tearDownTomcat();
    }

    protected void setUpTomcat() throws Exception {
        tomcat = new Tomcat();
        tomcat.setBaseDir("."); // for tmp dir
        tomcat.setHostname(HOST);
        tomcat.setPort(PORT);
        ProtocolHandler p = tomcat.getConnector().getProtocolHandler();
        AbstractEndpoint<?> endpoint = (AbstractEndpoint<?>) getFieldValue(p, "endpoint");
        endpoint.setMaxKeepAliveRequests(1); // vital for clean shutdown

        File docBase = new File(".");
        Context context = tomcat.addContext(CONTEXT, docBase.getAbsolutePath());

        Application app = new org.nuxeo.ecm.webdav.Application();
        ApplicationAdapter conf = new ApplicationAdapter(app);
        conf.getFeatures().put(ResourceConfig.FEATURE_MATCH_MATRIX_PARAMS, Boolean.TRUE);
        String servletName = "testServlet";
        Servlet servlet = new ServletContainer(conf);
        tomcat.addServlet(CONTEXT, servletName, servlet);
        context.addServletMappingDecoded("/*", servletName);
        addFilter(context, servletName, "RequestContextFilter", new RequestContextFilter());
        addFilter(context, servletName, "SessionCleanupFilter", new SessionCleanupFilter());
        addFilter(context, servletName, "NuxeoAuthenticationFilter", new NuxeoAuthenticationFilter());
        addFilter(context, servletName, "WebEngineFilter", new WebEngineFilter());

        tomcat.start();
    }

    protected void addFilter(Context context, String servletName, String filterName, Filter filter) {
        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName(filterName);
        filterDef.setFilterClass(filter.getClass().getName());
        context.addFilterDef(filterDef);
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName(filterName);
        filterMap.addServletName(servletName);
        context.addFilterMap(filterMap);
    }

    protected void tearDownTomcat() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
            Thread.sleep(100);
            tomcat = null;
        }
    }

    protected static Object getFieldValue(Object object, String name) throws ReflectiveOperationException {
        Class<? extends Object> klass = object.getClass();
        Field f = null;
        while (f == null && klass != Object.class) {
            try {
                f = klass.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                klass = klass.getSuperclass();
            }
        }
        if (f == null) {
            throw new NoSuchFieldException("No field " + name + " on " + object.getClass().getName());
        }
        f.setAccessible(true);
        return f.get(object);
    }

}
