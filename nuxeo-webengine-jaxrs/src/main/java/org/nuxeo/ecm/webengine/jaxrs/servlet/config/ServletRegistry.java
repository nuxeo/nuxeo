/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.jaxrs.Activator;
import org.nuxeo.ecm.webengine.jaxrs.Utils.ClassRef;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Handle servlet registration from Nuxeo extension points.
 * This class is a singleton shared by the {@link Activator} and the {@link ServletRegistryComponent} component.
 * Because we don;t have yet a solution to synchronize the intialization time of the Activator and a Nuxeo component
 * we are using a singleton instance to be able
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServletRegistry {


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

    public synchronized static void dispose() {
        instance = null;
    }

    /**
     * Servlets contributed to the extension points.
     * Servlets are contributed to the {@link HttpService} when it becomes available.
     */
    protected List<ServletDescriptor> servlets;

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
            service.registerServlet(sd.path, sd.getServlet(), sd.initParams, ctx);
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

