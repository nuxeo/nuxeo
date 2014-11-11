/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.jaxrs.Activator;
import org.nuxeo.ecm.webengine.jaxrs.Utils.ClassRef;
import org.nuxeo.ecm.webengine.jaxrs.servlet.ServletHolder;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Handle servlet registration from Nuxeo extension points. This class is a
 * singleton shared by the {@link Activator} and the
 * {@link ServletRegistryComponent} component. Because we don't have yet a
 * solution to synchronize the initialization time of the Activator and a Nuxeo
 * component we are using a singleton instance to be able
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ServletRegistry {

    public static final String SERVLET_NAME = ServletRegistry.class.getName()+".name";

    private static volatile ServletRegistry instance;

    public static ServletRegistry getInstance() {
        ServletRegistry reg = instance;
        if (reg == null) {
            synchronized (ServletRegistry.class) {
                reg = new ServletRegistry();
                instance = reg;
            }
        }
        return reg;
    }

    public static synchronized void dispose() {
        instance = null;
    }

    /**
     * Servlets contributed to the extension points.
     * Servlets are contributed to the {@link HttpService} when it becomes available.
     */
    protected List<ServletDescriptor> servlets;

    protected List<FilterSetDescriptor> filters;

    /**
     * The HttpService instance is injected when the service becomes
     * available by the Activator.
     */
    protected HttpService service;

    /**
     * The bundle owning this class
     */
    protected Bundle bundle;


    private ServletRegistry() {
        this.servlets = new ArrayList<ServletDescriptor>();
        this.filters = new ArrayList<FilterSetDescriptor>();
    }

    public synchronized ServletDescriptor[] getServletDescriptors() {
        return servlets.toArray(new ServletDescriptor[servlets.size()]);
    }

    public synchronized FilterSetDescriptor[] getFilterSetDescriptors() {
        return filters.toArray(new FilterSetDescriptor[filters.size()]);
    }

    public ServletDescriptor getServletDescriptor(String name) {
        for (ServletDescriptor servlet : getServletDescriptors()) {
            if (name.equals(servlet.name)) {
                return servlet;
            }
        }
        return null;
    }


    public List<FilterSetDescriptor> getFiltersFor(String name) {
        ArrayList<FilterSetDescriptor> list = new ArrayList<FilterSetDescriptor>();
        for (FilterSetDescriptor filter : getFilterSetDescriptors()) {
            if (name.equals(filter.targetServlet)) {
                list.add(filter);
            }
        }
        return list;
    }

    /**
     * Called by the service tracker when HttpService is up to configure it
     * with current contributed servlets
     * @param service
     */
    public synchronized void initHttpService(HttpService service) throws Exception {
        if (this.service == null) {
            this.service = service;
            installServlets();
        }
    }

    public HttpService getHttpService() {
        return service;
    }


    protected HttpContext getHttpContext(Bundle bundle) {
        return new BundleHttpContext(bundle);
    }

    public synchronized void addServlet(ServletDescriptor descriptor) throws Exception {
        servlets.add(descriptor);
        installServlet(descriptor);
    }


    public synchronized void removeServlet(ServletDescriptor descriptor) {
        servlets.remove(descriptor);
        if (service != null) {
            service.unregister(descriptor.path);
        }
    }

    public synchronized void addFilterSet(FilterSetDescriptor filter) {
        filters.add(filter);
    }

    public synchronized void removeFilterSet(FilterSetDescriptor filter) {
        filters.remove(filter);
    }

    private synchronized void installServlets() throws Exception {
        if (service != null) {
            for (ServletDescriptor sd : servlets) {
                installServlet(sd);
            }
        }
    }

    private void installServlet(ServletDescriptor sd) throws Exception {
        if (service != null) {
            ClassRef ref = sd.getClassRef();
            HttpContext ctx = getHttpContext(ref.bundle());
            Hashtable<String, String> params = new Hashtable<String, String>();
            if (sd.name != null) {
                params.put(SERVLET_NAME, sd.name);
            }
            service.registerServlet(sd.path, new ServletHolder(), params, ctx);
        }
    }

    static class BundleHttpContext implements HttpContext {
        Bundle bundle;
        public BundleHttpContext(Bundle bundle) {
            this.bundle = bundle;
        }
        @Override
        public String getMimeType(String name) {
            return null;
        }
        @Override
        public URL getResource(String name) {
            return null;
        }
        @Override
        public boolean handleSecurity(HttpServletRequest request,
                HttpServletResponse response) throws IOException {
            // default behaviour assumes the container has already performed authentication
            return true;
        }
    }
}

