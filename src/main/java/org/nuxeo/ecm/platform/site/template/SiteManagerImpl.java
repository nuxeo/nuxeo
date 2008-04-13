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

package org.nuxeo.ecm.platform.site.template;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SiteManagerImpl implements SiteManager {

    protected SitePageTemplate defaultObject;

    protected List<SiteObjectBinding> bindings;

    protected Map<String, SitePageTemplate> objects;

    protected LinkedHashMap<String, SitePageTemplate> cache;

    protected SiteObjectResolver resolver;

    protected File root;
    protected RenderingEngine engine;

    public SiteManagerImpl(File root, RenderingEngine engine) {
        this.bindings = new ArrayList<SiteObjectBinding>();
        this.objects = new HashMap<String, SitePageTemplate>();
        this.root = root;
        this.cache = new LinkedHashMap<String, SitePageTemplate>();
        this.resolver = new FileBasedResolver(this);
        this.engine = engine;
    }

    public RenderingEngine getRenderingEngine() {
        return engine;
    }

    public File getRootDirectory() {
        return root;
    }

    public SiteObjectBinding[] getBindings() {
        return bindings.toArray(new SiteObjectBinding[bindings.size()]);
    }

    public SitePageTemplate getTemplate(String name) {
        return objects.get(name);
    }

    public SitePageTemplate getOrCreateTemplate(String name) {
        SitePageTemplate template = objects.get(name);
        if (template == null) {
            template = new SitePageTemplate(name);
            objects.put(name, template);
        }
        return template;
    }

    public SitePageTemplate resolve(DocumentModel doc) {
        SitePageTemplate obj = resolver.resolve(doc, objects);
        if (obj == null) {
            return defaultObject;
        }
        return obj;
    }

    public SitePageTemplate[] getSiteObjects() {
        return objects.values().toArray(new SitePageTemplate[objects.size()]);
    }

    public void registerBinding(SiteObjectBinding binding) {
        bindings.add(binding);
    }

    public void registerSiteObject(SitePageTemplate object) {
        objects.put(object.getName(), object);
    }

    public void reset() {
        objects.clear();
        bindings.clear();
    }

    public void unregisterBinding(SiteObjectBinding binding) {
        bindings.remove(binding);
    }

    public void unregisterSiteObject(String name) {
        objects.remove(name);
    }

    public SitePageTemplate getDefaultSiteObject() {
        return defaultObject;
    }

    public void setDefaultSiteObject(SitePageTemplate object) {
        this.defaultObject = object;
    }

}
