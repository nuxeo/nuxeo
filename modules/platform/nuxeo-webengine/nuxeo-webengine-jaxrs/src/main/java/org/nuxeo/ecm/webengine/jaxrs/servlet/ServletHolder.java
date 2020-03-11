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
package org.nuxeo.ecm.webengine.jaxrs.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.jaxrs.BundleNotFoundException;
import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ListenerSetDescriptor;
import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ServletDescriptor;
import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ServletRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ServletHolder extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected RequestChain chain;

    protected ServletDescriptor descriptor;

    protected volatile boolean initDone;

    protected String getName(ServletConfig config) {
        String name = config.getInitParameter(ServletRegistry.SERVLET_NAME);
        if (name == null) {
            name = config.getServletName();
        }
        return name;
    }

    protected ServletDescriptor getDescriptor(ServletConfig config) throws ServletException {
        String name = getName(config);
        if (name == null) {
            throw new ServletException("No name defined for the ServletHolder. Check your servlet contributions.");
        }
        ServletDescriptor desc = ServletRegistry.getInstance().getServletDescriptor(name);
        if (desc == null) {
            throw new ServletException("No such servlet descriptor: " + name);
        }
        return desc;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            descriptor = getDescriptor(config);
            chain = new RequestChain(descriptor.getServlet(), descriptor.getFilters());
            ListenerSetDescriptor listeners = descriptor.getListenerSet();
            if (listeners != null) {
                // initialize listeners if not already initialized
                listeners.init(config);
            }
            super.init(config);
            // lazy chain.init(descriptor, config);
        } catch (ServletException e) {
            throw e;
        } catch (ReflectiveOperationException | BundleNotFoundException e) {
            throw new ServletException("Initialization exception for servlet " + config.getServletName(), e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        initDone = false;
        if (chain != null) {
            chain.destroy();
            chain = null;
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        lazyInit();
        Thread t = Thread.currentThread();
        ClassLoader cl = t.getContextClassLoader();
        try {
            // use servlet class loader as the context class loader
            t.setContextClassLoader(chain.servlet.getClass().getClassLoader());
            chain.execute(request, response);
        } finally {
            t.setContextClassLoader(cl);
        }
    }

    protected synchronized void lazyInit() throws ServletException {
        if (!initDone) {
            synchronized(this) {
                if (!initDone) {
                    chain.init(descriptor, getServletConfig());
                    initDone = true;
                }
            }
        }
    }

}
