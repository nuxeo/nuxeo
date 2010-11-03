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
 */
package org.nuxeo.ecm.webengine.model.view;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.runtime.api.Framework;

/**
 * A view to be used by regular JAX-RS resources to be able to use freemarker templates.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TemplateView {

    protected URL url;
    protected WebContext ctx;
    protected Object target;
    protected Map<String, Object> bindings;

    public static URL findTemplate(Object owner, String name) {
        URL url = owner.getClass().getResource(name);
        if (url == null) {
            url = Framework.getResourceLoader().getResource(name);
            if (url == null) {
                throw new WebResourceNotFoundException("View not found: "+name+" for object "+owner);
            }
        }
        return url;
    }

    public TemplateView(Object owner, String name) {
        this(WebEngine.getActiveContext(), owner, name);
    }

    public TemplateView(WebContext ctx, Object owner, String name) {
        this(ctx, owner, findTemplate(owner, name));
    }

    public TemplateView(Object owner, URL url) {
        this(WebEngine.getActiveContext(), owner, url);
    }

    public TemplateView(WebContext ctx, Object owner, URL url) {
        if (ctx == null) {
            throw new WebException("Not in WebEngine context");
        }
        this.ctx = ctx;
        this.target = owner;
        this.url = url;
        bindings = new HashMap<String, Object>();
        bindings.put("This", target);
        bindings.put("Context", ctx);
        bindings.put("Engine", ctx.getEngine());
        bindings.put("basePath", ctx.getBasePath());
        bindings.put("contextPath", VirtualHostHelper.getContextPathProperty());
    }

    public WebContext getContext() {
        return ctx;
    }

    public TemplateView arg(String key, Object value) {
        bindings.put(key, value);
        return this;
    }

    public TemplateView args(Map<String, Object> args) {
        bindings.putAll(args);
        return this;
    }

    public void render(Writer writer) {
        try {
            ctx.getEngine().getRendering().render(url.toExternalForm(), bindings, writer);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public void render(OutputStream out) {
        Writer writer = new OutputStreamWriter(out);
        try {
            render(writer);
        } finally{
            try { writer.flush(); } catch (Exception e) { throw WebException.wrap(e); }
        }
    }

    public String getString() {
        StringWriter writer = new StringWriter();
        render(writer);
        return writer.toString();
    }

}
