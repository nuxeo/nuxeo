/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.ws.WebServiceException;

import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.nuxeo.ecm.core.opencmis.bindings.LoginProvider;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Binder;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.transport.http.DeploymentDescriptorParser;
import com.sun.xml.ws.transport.http.DeploymentDescriptorParser.AdapterFactory;
import com.sun.xml.ws.transport.http.ResourceLoader;
import com.sun.xml.ws.transport.http.servlet.ServletAdapter;
import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import com.sun.xml.ws.transport.http.servlet.WSServlet;
import com.sun.xml.ws.transport.http.servlet.WSServletDelegate;

/**
 * Feature that starts a Web Services session.
 */
@Deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/servletcontainer-webservices-config.xml")
public class CmisFeatureSessionWebServices extends CmisFeatureSessionHttp {

    public static final String JAXWS_XML = "/sun-jaxws.xml";

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        super.configure(runner, binder);
        setWebServices();
    }

    // JavaUtilLoggingHelper.redirectToApacheCommons();

    // JavaUtilLoggingHelper.reset();

    @Override
    protected void setUpServer() throws Exception {
        // disable SOAP login checks
        System.setProperty(LoginProvider.class.getName(), TrustingLoginProvider.class.getName());
        super.setUpServer();
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        super.afterRun(runner);
        System.clearProperty(LoginProvider.class.getName());
    }

    @Override
    protected void addParams(Map<String, String> params) {
        String uri = serverURI.toString();
        uri += "services/"; // from sun-jaxws.xml
        params.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
        params.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, uri + "RepositoryService?wsdl");
        params.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, uri + "NavigationService?wsdl");
        params.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, uri + "ObjectService?wsdl");
        params.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, uri + "VersioningService?wsdl");
        params.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, uri + "DiscoveryService?wsdl");
        params.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, uri + "RelationshipService?wsdl");
        params.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, uri + "MultiFilingService?wsdl");
        params.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, uri + "PolicyService?wsdl");
        params.put(SessionParameter.WEBSERVICES_ACL_SERVICE, uri + "ACLService?wsdl");
    }

    /**
     * Loads a delegate configured for a local sun-jaxws.xml file.
     */
    public static class LocalWSServletContextListener implements ServletContextListener {

        /** Delegate expected by WSServlet */
        protected WSServletDelegate delegate;

        @Override
        public void contextDestroyed(ServletContextEvent event) {
            if (delegate != null) {
                delegate.destroy();
                delegate = null;
            }
        }

        /*
         * Puts in the servlet context the proper delegate expected by WSServlet.
         */
        @Override
        public void contextInitialized(ServletContextEvent event) {
            try {
                ServletContext context = event.getServletContext();
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = getClass().getClassLoader();
                }
                ResourceLoader loader = new ServletContextResourceLoader(context);
                Container container = newServletContainer(context);
                AdapterFactory<ServletAdapter> adapterFactory = new ServletAdapterList();

                DeploymentDescriptorParser<ServletAdapter> parser = new DeploymentDescriptorParser<ServletAdapter>(cl,
                        loader, container, adapterFactory);
                URL endpoints = context.getResource(JAXWS_XML);
                if (endpoints == null) {
                    throw new WebServiceException("No endpoints configured, missing file: " + JAXWS_XML);
                }
                List<ServletAdapter> adapters = parser.parse(endpoints.toExternalForm(), endpoints.openStream());

                delegate = new WSServletDelegate(adapters, context);
                context.setAttribute(WSServlet.JAXWS_RI_RUNTIME_INFO, delegate);
            } catch (WebServiceException e) {
                throw e;
            } catch (Exception e) {
                throw new WebServiceException(e.toString(), e);
            }
        }

        protected Container newServletContainer(ServletContext context) throws Exception {
            // return new ServletContainer(context);
            // the ServletContainer class from JAX-WS RI is package-private doh!
            Class<?> klass = Class.forName("com.sun.xml.ws.transport.http.servlet.ServletContainer");
            Constructor<?> ctor = klass.getDeclaredConstructor(ServletContext.class);
            ctor.setAccessible(true);
            return (Container) ctor.newInstance(context);
        }
    }

    // The ServletResourceLoader class from JAX-WS RI is package-private doh!
    public static final class ServletContextResourceLoader implements ResourceLoader {

        protected final ServletContext context;

        public ServletContextResourceLoader(ServletContext context) {
            this.context = context;
        }

        @Override
        public URL getResource(String path) throws MalformedURLException {
            return context.getResource(path);
        }

        @Override
        public URL getCatalogFile() throws MalformedURLException {
            // TODO catalog location
            return getResource("/WEB-INF/jax-ws-catalog.xml");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Set<String> getResourcePaths(String path) {
            return context.getResourcePaths(path);
        }
    }

}
