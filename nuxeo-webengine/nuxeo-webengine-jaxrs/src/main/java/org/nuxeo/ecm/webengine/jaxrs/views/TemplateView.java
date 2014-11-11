/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.views;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TemplateView extends View {

    private static final FreemarkerEngine engine = new FreemarkerEngine(null, new Locator());
    private static final Map<String, TemplateView> locators = new HashMap<String, TemplateView>();


    public TemplateView(String name) {
        this (null, name);
    }

    public TemplateView(Object owner, String name) {
        super (owner, name);
    }

    @Override
    public void render(Writer writer) throws Exception {
        String id = addLocator(this);
        try {
            engine.render(id, vars, writer);
            writer.flush();
        } finally {
            removeLocator(id);
        }
    }

    @Override
    public void render(OutputStream out) throws Exception {
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
