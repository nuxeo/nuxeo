package org.nuxeo.ecm.webengine.rest.jersey.patch;

import java.util.Map;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.api.core.HttpResponseContext;
import com.sun.jersey.api.uri.ExtendedUriInfo;

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


/**
 * We need to statically expose the thread local context. So we added the static get method
 *
 *@author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class ThreadLocalHttpContext implements HttpContext {
    private static ThreadLocal<HttpContext> context = new ThreadLocal<HttpContext>();

    public static HttpContext get() {
        return context.get();
    }

    /**
     * Set the {@link HttpRequestContext} and {@link HttpResponseContext} instances
     * for the current thread.
     */
    public void set(HttpContext context) {
        ThreadLocalHttpContext.context.set(context);
    }

    public ExtendedUriInfo getUriInfo() {
        return context.get().getUriInfo();
    }

    public HttpRequestContext getRequest() {
        return context.get().getRequest();
    }

    public HttpResponseContext getResponse() {
        return context.get().getResponse();
    }

    public Map<String, Object> getProperties() {
        return context.get().getProperties();
    }
}
