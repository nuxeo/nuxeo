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
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
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
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.io.ResourceWriter;
import org.nuxeo.ecm.webengine.model.io.ScriptFileWriter;
import org.nuxeo.ecm.webengine.model.io.WebViewWriter;
import org.nuxeo.runtime.api.Framework;

/**
 * Copied from {@link HttpServletDispatcher}. Modifications:
 * <ul>
 * <li> Changed Dispatcher implementation.
 * <li> Added methods to register root resources without {@link Path} annotation.
 * <li> Added WebEngine initialization
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final static Log log = LogFactory.getLog(WebEngineServlet.class); 
    
    protected Dispatcher dispatcher;
    private String servletMappingPrefix = "";

    protected void initProviders(ResteasyProviderFactory providerFactory) {
        //RegisterBuiltin.register(providerFactory);
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


    protected void initializeWebEngine(ResteasyProviderFactory providerFactory) throws ServletException {
        WebEngine engine = Framework.getLocalService(WebEngine.class);
        try {
            providerFactory.addMessageBodyWriter(new ResourceWriter());
            providerFactory.addMessageBodyWriter(new WebViewWriter());
            providerFactory.addMessageBodyWriter(new ScriptFileWriter());
            addRootResources(engine);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new ServletException("Failed to initialize WebEngine Root Resources", e);
        }
    }

    //TODO: refactor webapplication and rename it as ResourceContainer ?
    protected void addRootResources(WebEngine engine) throws Exception {
        Registry registry = dispatcher.getRegistry();
        // TODO if (registry.getClass() == WebEngineDispatcher.class) {...}
        // add first annotated JAX-RS resources?? 
        for (ResourceBinding binding : engine.getBindings()) {
            Class<?> rc = null;
            if (binding.className != null && binding.path != null) {
                rc = engine.getScripting().loadClass(binding.className);
                if (binding.singleton) { // TODO use a factory to create singletons and remove singleton property
//                    registry.addSingletonResource(rc.newInstance(), binding.path, binding.encode, binding.limited);
                    registry.addSingletonResource(rc.newInstance());
                } else {
//                    registry.addPojoResource(rc, binding.path, binding.encode, binding.limited);
                    registry.addPerRequestResource(rc);
                }
            } else {
                log.error("Invalid resource binding: "+binding.path+" -> "+binding.className+". No resource path / class specified.");
                continue;
            }            
        }
    }


    public Dispatcher getDispatcher()
    {
       return dispatcher;
    }


    public void init(ServletConfig servletConfig) throws ServletException
    {
       //bs: initialize runtime delegate
       RuntimeDelegate.setInstance(new ResteasyProviderFactory());
       
       ResteasyProviderFactory providerFactory = (ResteasyProviderFactory) servletConfig.getServletContext().getAttribute(ResteasyProviderFactory.class.getName());
       if (providerFactory == null)
       {
          providerFactory = new ResteasyProviderFactory();
          servletConfig.getServletContext().setAttribute(ResteasyProviderFactory.class.getName(), providerFactory);
       }

       dispatcher = (Dispatcher) servletConfig.getServletContext().getAttribute(Dispatcher.class.getName());
       if (dispatcher == null)
       {
          dispatcher = new SynchronousDispatcher(providerFactory);
          servletConfig.getServletContext().setAttribute(Dispatcher.class.getName(), dispatcher);
          servletConfig.getServletContext().setAttribute(Registry.class.getName(), dispatcher.getRegistry());
       }
       servletMappingPrefix = servletConfig.getServletContext().getInitParameter("resteasy.servlet.mapping.prefix");
       if (servletMappingPrefix == null) servletMappingPrefix = "";
       servletMappingPrefix.trim();
       //bs: initialize webegnine
       initProviders(providerFactory);
       initializeWebEngine(providerFactory);
    }

    public void setDispatcher(Dispatcher dispatcher)
    {
       this.dispatcher = dispatcher;
    }

    protected void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException
    {
       service(httpServletRequest.getMethod(), httpServletRequest, httpServletResponse);
    }

    public void service(String httpMethod, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
//bs: is this needed anymore?
//        String path = request.getPathInfo();
//        if (path == null) path = "/";

       HttpHeaders headers = ServletUtil.extractHttpHeaders(request);
       //UriInfoImpl uriInfo = ServletUtil.extractUriInfo(request, servletMappingPrefix);
       //bs: using real servlet path
       UriInfoImpl uriInfo = ServletUtil.extractUriInfo(request, request.getServletPath());       

       HttpRequest in;
       try
       {
          in = new HttpServletInputMessage(headers, request.getInputStream(), uriInfo, httpMethod.toUpperCase());
       }
       catch (IOException e)
       {
          throw new RuntimeException(e);
       }
       HttpResponse theResponse = new HttpServletResponseWrapper(response, dispatcher.getProviderFactory());

       try
       {
           //bs: initialize webengine context
           WebContext ctx = new WebEngineContext(uriInfo, request);
           WebEngine.setActiveContext(ctx);
           
          ResteasyProviderFactory.pushContext(HttpServletRequest.class, request);
          ResteasyProviderFactory.pushContext(HttpServletResponse.class, response);
          ResteasyProviderFactory.pushContext(SecurityContext.class, new ServletSecurityContext(request));
          dispatcher.invoke(in, theResponse);
       }
       finally
       {
          ResteasyProviderFactory.clearContextData();
          //bs: cleanup webengine context
          WebEngine.setActiveContext(null);
       }
    }

}
