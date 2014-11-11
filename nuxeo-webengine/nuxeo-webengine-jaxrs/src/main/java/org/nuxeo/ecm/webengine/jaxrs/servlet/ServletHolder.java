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
package org.nuxeo.ecm.webengine.jaxrs.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ListenerSetDescriptor;
import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ServletDescriptor;
import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ServletRegistry;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServletHolder extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected RequestChain chain;

    protected ServletDescriptor descriptor;

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
            throw new ServletException("No such servlet descriptor: "+name);
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
                listeners.init(config);
            }
            super.init(config);
            chain.init(descriptor, config);
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException("Initialization exception for servlet "+config.getServletName(), e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (chain != null) {
            chain.destroy();
            chain = null;
        }
        ListenerSetDescriptor lsd = descriptor.getListenerSet();
        if (lsd != null) {
            if (lsd.destroy(getServletConfig())) {
                lsd = null;
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        chain.execute(request, response);
    }


}
