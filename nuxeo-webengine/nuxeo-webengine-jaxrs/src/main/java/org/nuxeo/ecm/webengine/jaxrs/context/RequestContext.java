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
package org.nuxeo.ecm.webengine.jaxrs.context;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An HTTP request context
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RequestContext extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    private static final ThreadLocal<RequestContext> CTX = new ThreadLocal<RequestContext>();

    public static RequestContext getActiveContext() {
        return CTX.get();
    }

    public static RequestContext getActiveContext(ServletRequest request) {
        return (RequestContext) request.getAttribute(RequestContext.class.getName());
    }

    protected HttpServletRequest request;

    protected HttpServletResponse response;

    protected List<RequestCleanupHandler> cleanupHandlers;

    public RequestContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        cleanupHandlers = new ArrayList<RequestCleanupHandler>();
        CTX.set(this);
        request.setAttribute(RequestContext.class.getName(), this);
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public Principal getUserPrincipal() {
        return request.getUserPrincipal();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object o = get(key);
        return (T) o;
    }

    public void addRequestCleanupHandler(RequestCleanupHandler handler) {
        cleanupHandlers.add(handler);
    }

    public boolean removeCleanupHandler(RequestCleanupHandler handler) {
        return cleanupHandlers.remove(handler);
    }

    public void dispose() {
        request.removeAttribute(RequestContext.class.getName());
        CTX.remove();
        RuntimeException suppressed = null;
        for (RequestCleanupHandler handler : cleanupHandlers) {
            try {
                handler.cleanup(request);
            } catch (RuntimeException e) {
                // allow other cleanup handlers to do their work
                if (suppressed == null) {
                    suppressed = new RuntimeException("Exceptions during cleanup");
                }
                suppressed.addSuppressed(e);
            }
        }
        cleanupHandlers = null;
        request = null;
        response = null;
        if (suppressed != null) {
            throw suppressed;
        }
    }
}
