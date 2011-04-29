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
package org.nuxeo.ecm.webengine.jaxrs.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Application;

import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.webengine.jaxrs.Utils;
import org.nuxeo.ecm.webengine.jaxrs.scan.DynamicApplication;
import org.nuxeo.ecm.webengine.jaxrs.views.ViewMessageBodyWriter;
import org.osgi.framework.Bundle;

import com.sun.jersey.spi.container.servlet.ServletContainer;


/**
 * A JAX-RS servlet based on jersey that load the application declared by the bundle registering the servlet.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JerseyApplicationServlet extends HttpServlet implements ResourceLocator {

    private static final long serialVersionUID = 1L;


    protected Bundle bundle;

    protected ServletContainer container;

    protected RenderingEngine rendering;

    protected String resourcesPrefix;


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (container == null) {
            try {
                doInit(config);
            } catch (ServletException e) {
                throw e;
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
    }

    @Override
    public void destroy() {
        destroyContainer();
        container = null;
        bundle = null;
        resourcesPrefix = null;
    }

    public synchronized void reload() throws Exception {
        if (container != null) {
            return;
        }
        destroyContainer();
        bundle = lookupBundle();
        // reload is not working correctly since old classes are still referenced
        // for this to work we need a custom ResourceConfig but all fields in jersey
        // classes are private so we cannot set it ...
        //super.reload();
        initContainer(getServletConfig());
    }

    protected void doInit(ServletConfig config) throws Exception {
        try {
            this.bundle = lookupBundle();
            initContainer(config);
            String v = config.getInitParameter(RenderingEngine.class.getName());
            if (v != null) {
                rendering = (RenderingEngine)Utils.getClassRef(v, bundle).newInstance();
            } else {
                rendering = new FreemarkerEngine();
                ((FreemarkerEngine)rendering).getConfiguration().setClassicCompatible(false);
            }
            rendering.setResourceLocator(this);
            getServletContext().setAttribute(Bundle.class.getName(), bundle);
            getServletContext().setAttribute(RenderingEngine.class.getName(), rendering);
            resourcesPrefix = config.getInitParameter("resourcesPrefix");
            if (resourcesPrefix == null) {
                resourcesPrefix = "/skin";
            }
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public RenderingEngine getRenderingEngine() {
        return rendering;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public ServletContainer getContainer() {
        return container;
    }


    protected Bundle lookupBundle() {
        return (Bundle)getServletContext().getAttribute(Bundle.class.getName());
    }

    protected ServletContainer createContainer() throws Exception {
        return new ServletContainer(new DefaultApplication(createApplication()));
    }

    protected Application createApplication() throws Exception {
        String cname = getServletConfig().getInitParameter("javax.ws.rs.Application");
        if (cname != null) {
            return (Application)Utils.getClassRef(cname, bundle).newInstance();
        }
        final String pkg = getServletConfig().getInitParameter("org.eclipse.ecr.web.jaxrs.packages");
        return new DynamicApplication() {
            @Override
            protected Bundle getBundle() {
                return bundle;
            }
            @Override
            protected String getPackageBase() {
                return pkg != null ? pkg : "/";
            }
        };
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String pinfo = request.getPathInfo();
        if (pinfo != null && pinfo.startsWith(resourcesPrefix)) {
            super.service(request, response);
        } else {
            String method = request.getMethod().toUpperCase();
            if (!"GET".equals(method)) {
                // force reading properties because jersey is consuming one
                // character
                // from the input stream - see WebComponent.isEntityPresent.
                request.getParameterMap();
            }
            container.service(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        InputStream in = getServletContext().getResourceAsStream(pathInfo.substring(resourcesPrefix.length()));
        if (in != null) {
            String ctype = getServletContext().getMimeType(pathInfo);
            if (ctype != null) {
                resp.addHeader("Content-Type", ctype);
            }
            try {
                OutputStream out = resp.getOutputStream();
                byte[] bytes = new byte[1024*64];
                int r = in.read(bytes);
                while (r > -1) {
                    if (r > 0) {
                        out.write(bytes, 0, r);
                    }
                    r = in.read(bytes);
                }
                out.flush();
            } finally {
                in.close();
            }
        }
    }


    protected synchronized void initContainer(ServletConfig config) throws Exception {
        if (container != null) {
            return;
        }
        this.container = createContainer();
        Thread thread = Thread.currentThread();
        ClassLoader cl = thread.getContextClassLoader();
        thread.setContextClassLoader(ServiceClassLoader.getLoader());
        try {
            container.init(config);
        } finally {
            thread.setContextClassLoader(cl);
        }
    }

    protected synchronized void destroyContainer() {
        if (container == null) {
            return;
        }
        Thread thread = Thread.currentThread();
        ClassLoader cl = thread.getContextClassLoader();
        thread.setContextClassLoader(ServiceClassLoader.getLoader());
        try {
            container.destroy();
            container = null;
        } finally {
            thread.setContextClassLoader(cl);
        }
    }

    @Override
    public File getResourceFile(String key) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public URL getResourceURL(String key) {
        //        return bundle.getEntry(key);
        int i = key.lastIndexOf('/');
        String path;
        if (i == -1) {
            path = "/";
        } else {
            path = key.substring(0, i);
            key = key.substring(i+1);
        }
        Enumeration<URL> e = bundle.findEntries(path, key, false);
        if (e == null) {
            return null;
        }
        URL url = null;
        while (e.hasMoreElements()) {
            url = e.nextElement();
        }
        return url;
    }

    static class DefaultApplication extends Application {
        Application app;
        public DefaultApplication(Application app) {
            this.app = app;
        }
        @Override
        public Set<Class<?>> getClasses() {
            return app.getClasses();
        }
        @Override
        public Set<Object> getSingletons() {
            HashSet<Object> set = new HashSet<Object>(app.getSingletons());
            set.add(new ViewMessageBodyWriter());
            return set;
        }
    }

}
