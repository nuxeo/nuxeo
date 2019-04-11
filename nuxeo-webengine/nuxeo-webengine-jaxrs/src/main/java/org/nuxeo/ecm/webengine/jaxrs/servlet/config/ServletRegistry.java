/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.nuxeo.ecm.webengine.jaxrs.Activator;
import org.nuxeo.ecm.webengine.jaxrs.servlet.ServletHolder;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * Handle servlet registration from Nuxeo extension points. This class is a singleton shared by the {@link Activator}
 * and the {@link ServletRegistryComponent} component. Because we don't have yet a solution to synchronize the
 * initialization time of the Activator and a Nuxeo component we are using a singleton instance to be able
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ServletRegistry {

    public static final String SERVLET_NAME = ServletRegistry.class.getName() + ".name";

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
     * Servlets contributed to the extension points. Servlets are contributed to the {@link HttpService} when it becomes
     * available.
     */
    protected List<ServletDescriptor> servlets;

    protected List<FilterSetDescriptor> filters;

    /**
     * Store resources contributed from external bundles to servlets. Map the servlet path to the list of contributed
     * resources
     */
    protected Map<String, List<ResourcesDescriptor>> resources;

    /**
     * The registered HttpContext mapped to the servlet path. An HttpContext is created and inserted in this map when
     * its servlet is registered against the HttpService. The context is removed when the servlet is unregsitered.
     * <p>
     * Resource contributions are injected into the context and reinjected each time the context is restarted.
     */
    protected Map<String, BundleHttpContext> contexts;

    /**
     * The HttpService instance is injected when the service becomes available by the Activator.
     */
    protected HttpService service;

    /**
     * The bundle owning this class
     */
    protected Bundle bundle;

    private ServletRegistry() {
        this.servlets = new ArrayList<>();
        this.filters = new ArrayList<>();
        this.resources = new HashMap<>();
        this.contexts = new HashMap<>();
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
        ArrayList<FilterSetDescriptor> list = new ArrayList<>();
        for (FilterSetDescriptor filter : getFilterSetDescriptors()) {
            if (name.equals(filter.targetServlet)) {
                list.add(filter);
            }
        }
        return list;
    }

    /**
     * Called by the service tracker when HttpService is up to configure it with current contributed servlets
     *
     * @param service
     */
    public synchronized void initHttpService(HttpService service) throws ServletException, NamespaceException {
        if (this.service == null) {
            this.service = service;
            installServlets();
        }
    }

    public HttpService getHttpService() {
        return service;
    }

    public synchronized void addServlet(ServletDescriptor descriptor) throws ServletException, NamespaceException {
        servlets.add(descriptor);
        installServlet(descriptor);
    }

    public synchronized void removeServlet(ServletDescriptor descriptor) {
        servlets.remove(descriptor);
        contexts.remove(descriptor.path);
        if (service != null) {
            // destroy first the listeners if any was initialized
            ListenerSetDescriptor lsd = descriptor.getListenerSet();
            if (lsd != null) {
                lsd.destroy();
            }
            // unregister the servlet
            service.unregister(descriptor.path);
        }
    }

    public synchronized void reloadServlet(ServletDescriptor descriptor) throws ServletException, NamespaceException {
        removeServlet(descriptor);
        addServlet(descriptor);
    }

    public synchronized void addFilterSet(FilterSetDescriptor filter) {
        filters.add(filter);
    }

    public synchronized void removeFilterSet(FilterSetDescriptor filter) {
        filters.remove(filter);
    }

    public synchronized void addResources(ResourcesDescriptor rd) {
        List<ResourcesDescriptor> list = resources.get(rd.getServlet());
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(rd);
        // update context
        BundleHttpContext ctx = contexts.get(rd.getServlet());
        if (ctx != null) {
            ctx.setResources(list.toArray(new ResourcesDescriptor[list.size()]));
        }
    }

    public synchronized void removeResources(ResourcesDescriptor rd) {
        List<ResourcesDescriptor> list = resources.get(rd.getServlet());
        if (list != null) {
            if (list.remove(rd)) {
                if (list.isEmpty()) {
                    resources.remove(rd.getServlet());
                }
                // update context
                BundleHttpContext ctx = contexts.get(rd.getServlet());
                if (ctx != null) {
                    ctx.setResources(list.toArray(new ResourcesDescriptor[list.size()]));
                }
            }
        }
    }

    private synchronized void installServlets() throws ServletException, NamespaceException {
        if (service != null) {
            for (ServletDescriptor sd : servlets) {
                installServlet(sd);
            }
        }
    }

    private void installServlet(ServletDescriptor sd) throws ServletException, NamespaceException {
        if (service != null) {
            // ClassRef ref = sd.getClassRef();
            BundleHttpContext ctx = new BundleHttpContext(sd.bundle, sd.resources);
            List<ResourcesDescriptor> rd = resources.get(sd.path);
            // register resources contributed so far
            if (rd != null) {
                ctx.setResources(rd.toArray(new ResourcesDescriptor[rd.size()]));
            }
            Hashtable<String, String> params = new Hashtable<>();
            if (sd.name != null) {
                params.putAll(sd.getInitParams());
                params.put(SERVLET_NAME, sd.name);
            }
            service.registerServlet(sd.path, new ServletHolder(), params, ctx);
            contexts.put(sd.path, ctx);
        }
    }

    // static class BundleHttpContext implements HttpContext {
    // protected Bundle bundle;
    // public BundleHttpContext(Bundle bundle) {
    // this.bundle = bundle;
    // }
    // @Override
    // public String getMimeType(String name) {
    // return null;
    // }
    // @Override
    // public URL getResource(String name) {
    // return null;
    // }
    // @Override
    // public boolean handleSecurity(HttpServletRequest request,
    // HttpServletResponse response) throws IOException {
    // // default behaviour assumes the container has already performed authentication
    // return true;
    // }
    // }
}
