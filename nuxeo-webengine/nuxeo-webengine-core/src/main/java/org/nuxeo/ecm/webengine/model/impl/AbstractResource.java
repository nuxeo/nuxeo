/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.AdapterResource;
import org.nuxeo.ecm.webengine.model.LinkDescriptor;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.View;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.security.PermissionService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// DO NOT MODIFY class declaration! Cannot use WebResourceType<?> since groovy
// doesn't supports wildcards for now
@SuppressWarnings("unchecked")
public abstract class AbstractResource<T extends ResourceType> implements Resource {

    protected WebContext ctx;

    protected AbstractResource<?> next;

    protected AbstractResource<?> prev;

    protected String path;

    protected T type;

    @Override
    public Resource initialize(WebContext ctx, ResourceType type, Object... args) {
        this.ctx = ctx;
        this.type = (T) type;
        path = ctx.getUriInfo().getMatchedURIs().get(0);
        // quote path component to replace special characters (except slash and
        // @ chars)
        path = URIUtils.quoteURIPathComponent(path, false, false);
        // avoid paths ending in / -> this will mess-up URLs in FTL files.
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        // resteasy doesn't return correct paths - that should be relative as
        // is JAX-RS specs on resteasy paths begin with a /
        StringBuilder buf = new StringBuilder(64).append(ctx.getBasePath());
        if (!path.startsWith("/")) {
            buf.append('/');
        }
        path = buf.append(path).toString();
        if (!this.type.getGuard().check(this)) {
            throw new WebSecurityException("Failed to initialize object: " + path
                    + ". Object is not accessible in the current context", path);
        }
        initialize(args);
        return this;
    }

    protected void initialize(Object... args) {
        // do nothing
    }

    @Override
    public boolean isAdapter() {
        return type.getClass() == AdapterTypeImpl.class;
    }

    @Override
    public boolean isRoot() {
        return this == ctx.getRoot();
    }

    @Override
    public void setRoot(boolean isRoot) {
        AbstractWebContext ctx = (AbstractWebContext) this.ctx;
        if (isRoot) {
            ctx.root = this;
        } else {
            if (ctx.root == this) {
                ctx.root = null;
            }
        }
    }

    @Override
    public boolean isInstanceOf(String type) {
        return this.type.isDerivedFrom(type);
    }

    @Override
    public Response redirect(String uri) {
        try {
            if (!uri.contains("://")) {
                // not an absolute URI
                if (uri.startsWith("/")) {
                    uri = ctx.getServerURL().append(uri).toString();
                } else {
                    uri = ctx.getServerURL().append('/').append(uri).toString();
                }
            }
            return Response.seeOther(new URI(uri)).build();
        } catch (URISyntaxException e) {
            throw WebException.wrap(e);
        }
    }

    @Override
    public AdapterResource getActiveAdapter() {
        return next != null && next.isAdapter() ? (AdapterResource) next : null;
    }

    @Override
    public void dispose() {
        ctx = null;
        type = null;
        path = null;
    }

    @Override
    public Set<String> getFacets() {
        return type.getFacets();
    }

    @Override
    public boolean hasFacet(String facet) {
        return type.hasFacet(facet);
    }

    @Override
    public T getType() {
        return type;
    }

    @Override
    public WebContext getContext() {
        return ctx;
    }

    @Override
    public Module getModule() {
        return ctx.getModule();
    }

    @Override
    public Resource getNext() {
        return next;
    }

    @Override
    public void setNext(Resource next) {
        this.next = (AbstractResource<?>) next;
    }

    @Override
    public Resource getPrevious() {
        return prev;
    }

    @Override
    public void setPrevious(Resource previous) {
        this.prev = (AbstractResource<?>) previous;
    }

    @Override
    public String getName() {
        int e = path.endsWith("/") ? path.length() - 1 : path.length();
        int p = path.lastIndexOf('/', e - 1);
        return p > -1 ? path.substring(p + 1) : path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getTrailingPath() {
        int len = path.length();
        String urlPath = ctx.getUrlPath();
        return len < urlPath.length() ? urlPath.substring(len) : "/";
    }

    @Override
    public String getNextSegment() {
        String p = getTrailingPath();
        if (p != null && !"/".equals(p)) {
            int s = p.startsWith("/") ? 1 : 0;
            int k = p.indexOf('/', s);
            if (k == -1) {
                return s > 0 ? p.substring(s) : p;
            } else {
                return p.substring(s, k);
            }
        }
        return null;
    }

    @Override
    public String getURL() {
        return ctx.getServerURL().append(path).toString();
    }

    @Override
    public List<LinkDescriptor> getLinks(String category) {
        return ctx.getModule().getActiveLinks(this, category);
    }

    @Override
    public <A> A getAdapter(Class<A> adapter) {
        if (adapter == CoreSession.class) {
            return adapter.cast(ctx.getCoreSession());
        } else if (adapter == Principal.class) {
            return adapter.cast(ctx.getPrincipal());
        }
        if (adapter == WebContext.class) {
            return adapter.cast(ctx);
        }
        if (Resource.class.isAssignableFrom(adapter)) {
            return adapter.cast(this);
        }
        return null;
    }

    @Override
    public Resource newObject(String type, Object... args) {
        return ctx.newObject(type, args);
    }

    @Override
    public AdapterResource newAdapter(String type, Object... args) {
        return ctx.newAdapter(this, type, args);
    }

    @Override
    public Template getView(String viewId) {
        return new View(this, viewId).resolve();
    }

    @Override
    public Template getTemplate(String fileName) {
        return new Template(this, getModule().getFile(fileName));
    }

    @Override
    public boolean checkGuard(String guard) throws ParseException {
        return PermissionService.parse(guard).check(this);
    }

    @Override
    public String toString() {
        return type.getName() + " [" + path + "]";
    }

}
