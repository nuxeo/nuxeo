/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.bindings.CmisBindingFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet;
import org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.nuxeo.ecm.core.opencmis.tests.StatusLoggingDefaultHttpInvoker;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.NuxeoRequestControllerFilter;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.FilterConfigDescriptor;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Feature that starts an embedded server configured for CMIS.
 * <p>
 * This is abstract, so subclasses can specify if AtomPub, Browser Bindings or Web Services are used
 */
@Deploy("org.nuxeo.ecm.platform.web.common")
public abstract class CmisFeatureSessionHttp extends CmisFeatureSession {

    public static final String BASE_RESOURCE = "web";

    public static final String HOST = "localhost";

    public static final int PORT = 17488;

    // Tomcat server
    public Tomcat tomcat;

    public URI serverURI;

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        String repositoryName = runner.getFeature(CoreFeature.class).getRepositoryName();
        setUpServer();
        setUpCmisSession(repositoryName);
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        try {
            tearDownCmisSession();
        } finally {
            tearDownServer();
        }
    }

    @Override
    public Session setUpCmisSession(String repositoryName) {

        SessionFactory sf = SessionFactoryImpl.newInstance();
        Map<String, String> params = new HashMap<String, String>();

        params.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS, CmisBindingFactory.STANDARD_AUTHENTICATION_PROVIDER);

        params.put(SessionParameter.CACHE_SIZE_REPOSITORIES, "10");
        params.put(SessionParameter.CACHE_SIZE_TYPES, "100");
        params.put(SessionParameter.CACHE_SIZE_OBJECTS, "100");

        params.put(SessionParameter.REPOSITORY_ID, repositoryName);
        params.put(SessionParameter.USER, USERNAME);
        params.put(SessionParameter.PASSWORD, PASSWORD);

        params.put(SessionParameter.HTTP_INVOKER_CLASS, StatusLoggingDefaultHttpInvoker.class.getName());

        addParams(params);

        session = sf.createSession(params);
        return session;
    }

    @Override
    public void tearDownCmisSession() {
        if (session != null) {
            session.clear();
            session = null;
        }
    }

    /** Adds protocol-specific parameters. */
    protected abstract void addParams(Map<String, String> params);

    protected abstract EventListener[] getEventListeners();

    protected abstract Servlet getServlet();

    protected List<FilterAndName> getFilters() {
        return Arrays.asList( //
                new FilterAndName(new NuxeoRequestControllerFilter(), "NuxeoRequestController"), //
                new FilterAndName(new TrustingNuxeoAuthFilter(), "NuxeoAuthenticationFilter"));
    }

    public static class FilterAndName {
        public Filter filter;

        public String name;

        public FilterAndName(Filter filter, String name) {
            this.filter = filter;
            this.name = name;
        }
    }

    protected void setUpServer() throws Exception {
        RequestControllerService ctrl = (RequestControllerService) Framework.getService(RequestControllerManager.class);
        // use transactional config
        FilterConfigDescriptor conf = new FilterConfigDescriptor("cmis-test", ".*", true, true, false, false, false,
                null);
        ctrl.registerFilterConfig(conf);
        setUpTomcat();
    }

    protected void tearDownServer() throws Exception {
        tearDownTomcat();
    }

    // org.apache.chemistry.opencmis.server.shared.HttpUtils.splitPath returns [""] instead of []
    // if there's not at least a non-empty servlet or context, so provide one.
    public static final String CONTEXT = "context";

    protected void setUpTomcat() throws Exception {
        tomcat = new Tomcat();
        tomcat.setBaseDir("."); // for tmp dir
        tomcat.setHostname(HOST);
        tomcat.setPort(PORT);
        ProtocolHandler p = tomcat.getConnector().getProtocolHandler();
        AbstractEndpoint<?> endpoint = (AbstractEndpoint<?>) getFieldValue(p, "endpoint");
        // ServerSocketFactory factory = new
        // ReuseAddrServerSocketFactory(endpoint);
        // endpoint.setServerSocketFactory(factory);
        // endpoint.getSocketProperties().setSoReuseAddress(true);
        endpoint.setMaxKeepAliveRequests(1); // vital for clean shutdown

        URL url = Thread.currentThread().getContextClassLoader().getResource(BASE_RESOURCE);
        assertNotNull(url);
        File docBase = new File(url.getPath());
        org.apache.catalina.Context context = tomcat.addContext("/" + CONTEXT, docBase.getAbsolutePath());
        String SERVLET_NAME = "testServlet";
        Wrapper servlet = tomcat.addServlet("/" + CONTEXT, SERVLET_NAME, getServlet());
        servlet.addInitParameter(CmisAtomPubServlet.PARAM_CALL_CONTEXT_HANDLER,
                BasicAuthCallContextHandler.class.getName());
        servlet.addInitParameter(CmisAtomPubServlet.PARAM_CMIS_VERSION, CmisVersion.CMIS_1_1.value());
        context.addServletMapping("/*", SERVLET_NAME);
        context.setApplicationLifecycleListeners(getEventListeners());
        for (FilterAndName f : getFilters()) {
            addFilter(context, SERVLET_NAME, f.name, f.filter);
        }

        serverURI = new URI("http://" + HOST + ':' + PORT + '/' + CONTEXT);
        tomcat.start();
    }

    protected void addFilter(org.apache.catalina.Context context, String servletName, String filterName, Filter filter) {
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

    protected static Object getFieldValue(Object object, String name) throws Exception {
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
