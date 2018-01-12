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
 */
package org.nuxeo.ecm.webengine.model.view;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.WebEngine;
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
                throw new WebResourceNotFoundException("View not found: " + name + " for object " + owner);
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
            throw new NuxeoException("Not in WebEngine context");
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
        } catch (RenderingException e) {
            throw new NuxeoException(e);
        }
    }

    public void render(OutputStream out) {
        Writer writer = new OutputStreamWriter(out);
        render(writer);
        try {
            writer.flush();
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    public String getString() {
        StringWriter writer = new StringWriter();
        render(writer);
        return writer.toString();
    }

}
