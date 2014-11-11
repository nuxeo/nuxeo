/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.server.resteasy;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.providers.ByteArrayProvider;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.plugins.providers.InputStreamProvider;
import org.jboss.resteasy.plugins.providers.StreamingOutputProvider;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServletInputMessage;
import org.jboss.resteasy.plugins.server.servlet.HttpServletResponseWrapper;
import org.jboss.resteasy.plugins.server.servlet.ServletSecurityContext;
import org.jboss.resteasy.plugins.server.servlet.ServletUtil;
import org.jboss.resteasy.specimpl.UriInfoImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;

/**
 * Mostly copied from {@link HttpServletDispatcher}.
 * <p>
 * Modifications:
 * <ul>
 * <li>Changed Dispatcher implementation.
 * <li>Added methods to register root resources without {@link Path} annotation.
 * <li>Added WebEngine initialization
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngineServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected Dispatcher dispatcher;
    protected ResourceRegistryImpl registry;

    protected void initializeBuiltinProviders(
            ResteasyProviderFactory providerFactory) {
        // RegisterBuiltin.register(providerFactory);
        try {
            providerFactory.addMessageBodyReader(new DefaultTextPlain());
            providerFactory.addMessageBodyWriter(new DefaultTextPlain());
            providerFactory.addMessageBodyReader(new StringTextStar());
            providerFactory.addMessageBodyWriter(new StringTextStar());
            providerFactory.addMessageBodyReader(new InputStreamProvider());
            providerFactory.addMessageBodyWriter(new InputStreamProvider());
            providerFactory.addMessageBodyReader(new ByteArrayProvider());
            providerFactory.addMessageBodyWriter(new ByteArrayProvider());
            providerFactory.addMessageBodyReader(new FormUrlEncodedProvider());
            providerFactory.addMessageBodyWriter(new FormUrlEncodedProvider());
            providerFactory.addMessageBodyWriter(new StreamingOutputProvider());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        ResourceContainer rc = (ResourceContainer) Framework.getRuntime().getComponent(ResourceContainer.NAME);
        dispatcher = rc.getDispatcher();
        // bs: initialize webegnine
        initializeBuiltinProviders(dispatcher.getProviderFactory());
        addInterceptors();
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    protected void service(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws ServletException,
            IOException {
        service(httpServletRequest.getMethod(), httpServletRequest, httpServletResponse);
    }

    public void service(String httpMethod, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        // bs: is this needed anymore?
        // String path = request.getPathInfo();
        // if (path == null) path = "/";

        HttpHeaders headers = ServletUtil.extractHttpHeaders(request);
        // UriInfoImpl uriInfo = ServletUtil.extractUriInfo(request,
        // servletMappingPrefix);
        // bs: using real servlet path
        //UriInfoImpl uriInfo = ServletUtil.extractUriInfo(request, request.getServletPath());
        UriInfoImpl uriInfo = UriInfoImpl.create(request);

        HttpRequest in;
        in = new HttpServletInputMessage(headers, new HttpRequestLazyInputStream(request),
                    uriInfo, httpMethod.toUpperCase());
        HttpResponse theResponse = new HttpServletResponseWrapper(response,
                dispatcher.getProviderFactory());
//        double d = System.currentTimeMillis();
        WebContext ctx =null;
        try {
            // bs: initialize webengine context
            ctx = new WebEngineContext(in, request);
            WebEngine.setActiveContext(ctx);

            ResteasyProviderFactory.pushContext(HttpServletRequest.class, request);
            ResteasyProviderFactory.pushContext(HttpServletResponse.class, response);
            ResteasyProviderFactory.pushContext(SecurityContext.class, new ServletSecurityContext(
                    request));
            dispatcher.invoke(in, theResponse);
        } finally {
            if (ctx!=null)
            {
                UserSession us = ctx.getUserSession();
                if (us!=null)
                    us.terminateRequest(request);
            }
            ResteasyProviderFactory.clearContextData();
            // bs: cleanup webengine context
            WebEngine.setActiveContext(null);
//            System.out.println(">>>>>>>>>>>>"+((System.currentTimeMillis()-d)/1000));
        }
    }

    protected void addInterceptors() {
        dispatcher.getProviderFactory().getInterceptorRegistry().registerResourceMethodInterceptor(
                new SecurityInterceptor());
    }

}
