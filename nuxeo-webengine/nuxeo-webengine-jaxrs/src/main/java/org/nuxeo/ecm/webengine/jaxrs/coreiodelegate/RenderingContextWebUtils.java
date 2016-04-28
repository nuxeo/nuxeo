/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.webengine.jaxrs.coreiodelegate;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.io.registry.context.RenderingContextImpl.RenderingContextBuilder;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;

/**
 * Utility class that get or create a {@link RenderingContext} from the current {@link HttpServletRequest}.
 *
 * @since 7.2
 */
public final class RenderingContextWebUtils {

    private static final String CTX_KEY = "_STORED_GENERATED_RENDERING_CONTEXT";

    public static final String REQUEST_KEY = "_STORED_GENERATED_HTTP_SERVLET_REQUEST";

    private RenderingContextWebUtils() {
    }

    /**
     * Get the current context in the current {@link HttpServletRequest} or create it from the current
     * {@link HttpServletRequest}.
     *
     * @since 7.2
     */
    public static RenderingContext getContext(ServletRequest request) {
        // try to get an existing RenderingContext from the request
        Object stored = request.getAttribute(getContextKey());
        if (stored != null) {
            return (RenderingContext) stored;
        }
        RenderingContextBuilder builder = CtxBuilder.builder();
        fillContext(builder, request);
        RenderingContext ctx = builder.get();
        registerContext(request, ctx);
        return ctx;
    }

    /**
     * Register the given context as the context to use to manage the marshalling.
     *
     * @param request The current request.
     * @param ctx The context to register.
     * @since 7.10
     */
    public static void registerContext(ServletRequest request, RenderingContext ctx) {
        request.setAttribute(getContextKey(), ctx);
    }

    /**
     * Return the key used to store the context in the request.
     *
     * @since 7.10
     */
    public static String getContextKey() {
        return CTX_KEY;
    }

    /**
     * Create an {@link RenderingContextBuilder}, fill it with the current {@link HttpServletRequest} and return it.
     *
     * @since 7.2
     */
    public static RenderingContextBuilder getBuilder(ServletRequest request) {
        RenderingContextBuilder builder = CtxBuilder.builder();
        fillContext(builder, request);
        return builder;
    }

    /**
     * Fill an existing with the current {@link HttpServletRequest}.
     *
     * @since 7.2
     */
    public static RenderingContextBuilder fillContext(RenderingContextBuilder builder, ServletRequest request) {
        // create a context builder and put base url, session and request
        builder.param(REQUEST_KEY, request);
        // for web context, put the base url, the session and the headers
        if (request instanceof HttpServletRequest) {
            HttpServletRequest webRequest = (HttpServletRequest) request;
            // base url
            @SuppressWarnings("deprecation")
            String baseURL = VirtualHostHelper.getBaseURL(webRequest);
            // current session
            CoreSession session = SessionFactory.getSession(webRequest);
            builder.base(baseURL).session(session);
            // gets the locale from the request or takes the server's default
            Locale locale = request.getLocale();
            if (locale != null) {
                builder.locale(locale);
            }
            // headers
            Enumeration<String> headerNames = webRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                Enumeration<String> headerValues = webRequest.getHeaders(headerName);
                while (headerValues.hasMoreElements()) {
                    builder.param(headerName, headerValues.nextElement());
                }
            }
        }
        // parameters
        for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            builder.paramValues(parameter.getKey(), (Object[]) parameter.getValue());
        }
        // attributes
        Enumeration<String> attributeNames = request.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = request.getAttribute(attributeName);
            builder.param(attributeName, attributeValue);
        }
        return builder;
    }

}
