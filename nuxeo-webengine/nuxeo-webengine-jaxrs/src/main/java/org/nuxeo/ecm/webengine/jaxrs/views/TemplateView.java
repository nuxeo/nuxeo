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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.osgi.framework.Bundle;

/**
 * Template for compatibility with Nuxeo WebEngine
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TemplateView {

    private static final FreemarkerEngine engine = new FreemarkerEngine(null, new Locator());

    private static final Map<String, TemplateView> locators = new HashMap<>();

    public static URL resolveFile(File file) throws ViewNotFoundException {
        if (!file.isFile()) {
            throw new ViewNotFoundException(null, null, file.getAbsolutePath());
        }
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new ViewNotFoundException(e, null, file.getAbsolutePath());
        }
    }

    public static URL resolveResource(Object owner, String name) throws ViewNotFoundException {
        URL url = owner.getClass().getResource(name);
        if (url == null) {
            throw new ViewNotFoundException(null, owner, name);
        }
        return url;
    }

    public static URL resolveResourceFromBundle(Bundle bundle, String name) throws ViewNotFoundException {
        URL url = bundle.getEntry(name);
        if (url == null) {
            throw new ViewNotFoundException(null, bundle, name);
        }
        return url;
    }

    private static URL resolveResource(Bundle bundle, Object owner, String name) throws ViewNotFoundException {
        return bundle != null ? resolveResourceFromBundle(bundle, name) : resolveResource(owner, name);
    }

    protected Object owner;

    protected final URL url;

    protected final Map<String, Object> vars;

    public TemplateView(String name) {
        this(null, null, name);
    }

    public TemplateView(Object owner, String name) {
        this(null, owner, name);
    }

    public TemplateView(Bundle bundle, Object owner, String name) {
        this(owner, resolveResource(bundle, owner, name));
    }

    public TemplateView(File file) {
        this(null, file);
    }

    public TemplateView(Object owner, File file) {
        this(owner, resolveFile(file));
    }

    public TemplateView(URL url) {
        this(null, url);
    }

    public TemplateView(Object owner, URL url) {
        vars = new HashMap<>();
        this.url = url;
        if (owner != null) {
            forObject(owner);
        }
    }

    public TemplateView forObject(Object owner) {
        this.owner = owner;
        vars.put("This", owner);
        return this;
    }

    public URL getUrl() {
        return url;
    }

    public Object getOwner() {
        return owner;
    }

    public TemplateView arg(String key, Object value) {
        vars.put(key, value);
        return this;
    }

    public void render(Writer writer) throws RenderingException, IOException {
        String id = addLocator(this);
        try {
            engine.render(id, vars, writer);
            writer.flush();
        } finally {
            removeLocator(id);
        }
    }

    public void render(OutputStream out) throws RenderingException, IOException {
        render(new OutputStreamWriter(out, "UTF-8"));
    }

    private static synchronized String addLocator(TemplateView view) {
        String locatorId = "view:/" + view.getUrl().toExternalForm();
        locators.put(locatorId, view);
        return locatorId;
    }

    private static synchronized void removeLocator(String id) {
        locators.remove(id);
    }

    private static synchronized TemplateView getLocator(String id) {
        return locators.get(id);
    }

    static class Locator implements ResourceLocator {
        @Override
        public File getResourceFile(String key) {
            return null;
        }

        @Override
        public URL getResourceURL(String key) {
            TemplateView view = getLocator(key);
            if (view != null) {
                return view.getUrl();
            }
            return null;
        }
    }

}
