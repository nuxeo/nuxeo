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

package org.nuxeo.ecm.webengine.rest.servlet.resteasy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.rest.PathDescriptor;
import org.nuxeo.ecm.webengine.rest.ResourceBinding;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.io.ScriptFileWriter;
import org.nuxeo.ecm.webengine.rest.io.WebViewWriter;
import org.nuxeo.ecm.webengine.rest.model.ManagedResource;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.runtime.api.Framework;
import org.resteasy.Dispatcher;
import org.resteasy.Headers;
import org.resteasy.ResourceMethodRegistry;
import org.resteasy.plugins.providers.ByteArrayProvider;
import org.resteasy.plugins.providers.DefaultTextPlain;
import org.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.resteasy.plugins.providers.InputStreamProvider;
import org.resteasy.plugins.providers.StreamingOutputProvider;
import org.resteasy.plugins.providers.StringTextStar;
import org.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.resteasy.plugins.server.resourcefactory.SingletonResource;
import org.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.resteasy.plugins.server.servlet.HttpServletResponseWrapper;
import org.resteasy.plugins.server.servlet.ServletSecurityContext;
import org.resteasy.specimpl.HttpHeadersImpl;
import org.resteasy.specimpl.MultivaluedMapImpl;
import org.resteasy.specimpl.PathSegmentImpl;
import org.resteasy.specimpl.UriBuilderImpl;
import org.resteasy.specimpl.UriInfoImpl;
import org.resteasy.spi.HttpRequest;
import org.resteasy.spi.HttpResponse;
import org.resteasy.spi.Registry;
import org.resteasy.spi.ResteasyProviderFactory;
import org.resteasy.util.HttpHeaderNames;
import org.resteasy.util.MediaTypeHelper;

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
    private static final Log log = LogFactory.getLog(WebEngineServlet.class);


    protected WebEngineDispatcher dispatcher;

    public WebEngineDispatcher getDispatcher() {
        return dispatcher;
    }

    public void init(ServletConfig servletConfig) throws ServletException {
        ResteasyProviderFactory providerFactory = (ResteasyProviderFactory) servletConfig.getServletContext().getAttribute(
                ResteasyProviderFactory.class.getName());
        if (providerFactory == null) {
            providerFactory = new ResteasyProviderFactory();
            servletConfig.getServletContext().setAttribute(
                    ResteasyProviderFactory.class.getName(), providerFactory);
        }

        dispatcher = (WebEngineDispatcher) servletConfig.getServletContext().getAttribute(
                Dispatcher.class.getName());
        if (dispatcher == null) {
            dispatcher = new WebEngineDispatcher(providerFactory);
            servletConfig.getServletContext().setAttribute(
                    Dispatcher.class.getName(), dispatcher);
            servletConfig.getServletContext().setAttribute(
                    Registry.class.getName(), dispatcher.getRegistry());
        }

        //RegisterBuiltin.register(providerFactory);
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

        initializeWebEngine(providerFactory);
    }

    public void addPerRequestResource(ResourceMethodRegistry reg,
            Class<?> clazz, String base) {
        reg.addResourceFactory(new POJOResourceFactory(clazz), base, clazz, 0,
                true);
    }

    public void addSingletonResource(ResourceMethodRegistry reg,
            Object singleton, String base) {
        reg.addResourceFactory(new SingletonResource(singleton), base,
                singleton.getClass(), 0, true);
    }

    protected void service(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws ServletException,
            IOException {
        service(httpServletRequest.getMethod(), httpServletRequest,
                httpServletResponse);
    }

    /**
     * wrapper around service so we can test easily
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws ServletException
     * @throws IOException
     */
    public void invoke(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws ServletException,
            IOException {
        service(httpServletRequest, httpServletResponse);
    }

    public void service(String httpMethod, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        HttpHeaders headers = extractHttpHeaders(request);
// TODO buggy code : it is putting the servlet path in the resulting path ...
//        String path = PathHelper.getEncodedPathInfo(request.getRequestURI(),
//                request.getContextPath());
String path = request.getPathInfo();
if (path == null) path = "/";
        // System.out.println("contextPath: " + request.getContextPath());
        // System.out.println("path: " + path);
        // System.out.println("getRequestURI: " + request.getRequestURI());
        // System.out.println("getRequestURL: " + request.getRequestURL());
        URI absolutePath = null;
        try {
            URL absolute = new URL(request.getRequestURL().toString());

            UriBuilderImpl builder = new UriBuilderImpl();
            builder.scheme(absolute.getProtocol());
            builder.host(absolute.getHost());
            builder.port(absolute.getPort());
            builder.path(absolute.getPath());
            builder.replaceQueryParams(absolute.getQuery());
            absolutePath = builder.build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        List<PathSegment> pathSegments = PathSegmentImpl.parseSegments(path);
        UriInfoImpl uriInfo = new UriInfoImpl(absolutePath, path,
                request.getQueryString(), pathSegments);

        HttpRequest in;
        try {
            in = new WebEngineContext(request, headers,
                    httpMethod.toUpperCase(), uriInfo);
            WebEngine2.setActiveContext((WebContext2)in);
            HttpResponse theResponse = new HttpServletResponseWrapper(response,
                    dispatcher.getProviderFactory());
            try {
                ResteasyProviderFactory.pushContext(HttpServletRequest.class,
                        request);
                ResteasyProviderFactory.pushContext(HttpServletResponse.class,
                        response);
                ResteasyProviderFactory.pushContext(SecurityContext.class,
                        new ServletSecurityContext(request));
                dispatcher.invoke(in, theResponse);
            } finally {
                ResteasyProviderFactory.clearContextData();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            WebEngine2.setActiveContext(null);  
        }
    }

    public static HttpHeaders extractHttpHeaders(HttpServletRequest request) {
        HttpHeadersImpl headers = new HttpHeadersImpl();

        MultivaluedMapImpl<String, String> requestHeaders = extractRequestHeaders(request);
        headers.setRequestHeaders(requestHeaders);
        List<MediaType> acceptableMediaTypes = extractAccepts(requestHeaders);
        List<String> acceptableLanguages = extractLanguages(requestHeaders);
        headers.setAcceptableMediaTypes(acceptableMediaTypes);
        headers.setAcceptableLanguages(acceptableLanguages);
        headers.setLanguage(requestHeaders.getFirst(HttpHeaderNames.CONTENT_LANGUAGE));

        String contentType = request.getContentType();
        if (contentType != null)
            headers.setMediaType(MediaType.valueOf(contentType));

        Map<String, javax.ws.rs.core.Cookie> cookies = extractCookies(request);
        headers.setCookies(cookies);
        return headers;

    }

    private static Map<String, javax.ws.rs.core.Cookie> extractCookies(
            HttpServletRequest request) {
        Map<String, javax.ws.rs.core.Cookie> cookies = new HashMap<String, javax.ws.rs.core.Cookie>();
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                cookies.put(cookie.getName(), new javax.ws.rs.core.Cookie(
                        cookie.getName(), cookie.getValue(), cookie.getPath(),
                        cookie.getDomain(), cookie.getVersion()));

            }
        }
        return cookies;
    }

    public static List<MediaType> extractAccepts(
            MultivaluedMapImpl<String, String> requestHeaders) {
        List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        List<String> accepts = requestHeaders.get(HttpHeaderNames.ACCEPT);
        if (accepts == null)
            return acceptableMediaTypes;

        for (String accept : accepts) {
            acceptableMediaTypes.addAll(MediaTypeHelper.parseHeader(accept));
        }
        return acceptableMediaTypes;
    }

    public static List<String> extractLanguages(
            MultivaluedMapImpl<String, String> requestHeaders) {
        List<String> acceptable = new ArrayList<String>();
        List<String> accepts = requestHeaders.get(HttpHeaderNames.ACCEPT_LANGUAGE);
        if (accepts == null)
            return acceptable;

        for (String accept : accepts) {
            String[] splits = accept.split(",");
            for (String split : splits)
                acceptable.add(split.trim());
        }
        return acceptable;
    }

    public static MultivaluedMapImpl<String, String> extractRequestHeaders(
            HttpServletRequest request) {
        Headers<String> requestHeaders = new Headers<String>();

        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            Enumeration headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = (String) headerValues.nextElement();
                //System.out.println("ADDING HEADER: " + headerName + " value: " + headerValue);
                requestHeaders.add(headerName, headerValue);
            }
        }
        return requestHeaders;
    }


    protected void initializeWebEngine(ResteasyProviderFactory providerFactory) throws ServletException {
        WebEngine2 engine = Framework.getLocalService(WebEngine2.class);
        try {
            providerFactory.addMessageBodyWriter(new WebViewWriter());
            providerFactory.addMessageBodyWriter(new ScriptFileWriter());
            addRootResources(engine);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new ServletException("Failed to initialize WebEngine Root Resources", e);
        }
    }

    //TODO: refactor webapplication and rename it as ResourceContainer ?
    protected void addRootResources(WebEngine2 engine) throws Exception {
        org.nuxeo.ecm.webengine.rest.servlet.resteasy.patch.ResourceMethodRegistry registry = dispatcher.getRegistry();
        // add first annotated JAX-RS resources?? 
        for (ResourceBinding binding : engine.getBindings()) {
            Class<?> rc = null;
            if (binding.className != null && binding.path != null) {
                rc = engine.getScripting().loadClass(binding.className);
                if (binding.singleton) { // TODO use a factory to create singletons and remove singleton property
                    registry.addSingletonResource(rc.newInstance(), binding.path, binding.encode, binding.limited);
                } else {
                    registry.addPojoResource(rc, binding.path, binding.encode, binding.limited);
                }
            } else {
                log.error("Invalid resource binding: "+binding.path+" -> "+binding.className+". No resource path / class specified.");
                continue;
            }            
            // add managed resources
            for (WebApplication app : engine.getApplicationRegistry().getApplications()) {
                if (app.isFragment()) {
                    continue;
                }
                PathDescriptor path = app.getPath();
                ManagedResource res = app.getRootResource();
                registry.addSingletonResource(res, path.path, path.encode, path.limited);
            }
        }
    }

}
