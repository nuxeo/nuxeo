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
package org.nuxeo.ecm.webengine.jaxrs.views;

import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.LinkedList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.ApplicationHost;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.osgi.framework.Bundle;

/**
 * A resource request context. This class is not thread safe.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ResourceContext {

    private static ThreadLocal<ResourceContext> perThreadContext = new ThreadLocal<ResourceContext>();

    public final static void setContext(ResourceContext context) {
        perThreadContext.set(context);
    }

    public final static ResourceContext getContext() {
        return perThreadContext.get();
    }

    public final static void destroyContext() {
        perThreadContext.remove();
    }

    /**
     * The JAX-RS application providing the resources.
     */
    protected ApplicationHost app;

    protected HttpServletRequest request;

    protected UriInfo uriInfo;

    private LinkedList<Bundle> bundleStack;

    private CoreSession session;

    protected ResourceContext() {
    }

    public ResourceContext(ApplicationHost app) {
        // TODO rendering in app
        this.app = app;
        bundleStack = new LinkedList<Bundle>();
        // this.bundleStack.add(app.getBundle());
    }

    public ApplicationHost getApplication() {
        return app;
    }

    public final LinkedList<Bundle> getBundleStack() {
        return bundleStack;
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
        session = SessionFactory.getSession(request);
    }

    public final Bundle getBundle() {
        return bundleStack.isEmpty() ? null : bundleStack.get(bundleStack.size() - 1);
    }

    public final RenderingEngine getRenderingEngine() {
        return app.getRendering();
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public Principal getPrincipal() {
        return request.getUserPrincipal();
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public CoreSession getSession() {
        return session;
    }

    public URI getBaseUri() {
        return uriInfo.getBaseUri();
    }

    public void pushBundleFor(Object obj) {
        Bundle b = getResourceBundle(obj);
        if (b != null) {
            pushBundle(b);
        }
    }

    public void pushBundle(Bundle bundle) {
        for (Bundle b : bundleStack) {
            if (b == bundle) {
                // already present
                return;
            }
        }
        bundleStack.add(bundle);
    }

    protected Bundle getResourceBundle(Object res) {
        // return FrameworkUtil.getBundle(res.getClass());
        return app.getBundle(res.getClass());
    }

    /**
     * The prefix used to reference templates in template source locators
     *
     * @return
     */
    public String getViewRoot() {
        return bundleStack.isEmpty() ? "" : "view:" + bundleStack.get(bundleStack.size() - 1).getBundleId() + ":/";
    }

    public URL findEntry(String path) {
        if (path.startsWith("view:")) {
            int p = path.indexOf(":/");
            if (p > -1) {
                path = path.substring(p + 2);
            }
        }
        for (int i = bundleStack.size() - 1; i >= 0; i--) {
            URL url = bundleStack.get(i).getEntry(path);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

}
