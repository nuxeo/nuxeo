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

    protected SiteObject defaultObject;

    protected List<SiteObjectBinding> bindings;

    protected Map<String, SiteObject> objects;

    protected LinkedHashMap<String, SiteObject> cache;

    protected SiteObjectResolver resolver;

    protected File root;
    protected RenderingEngine engine;

    public SiteManagerImpl(File root, RenderingEngine engine) {
        this.bindings = new ArrayList<SiteObjectBinding>();
        this.objects = new HashMap<String, SiteObject>();
        this.root = root;
        this.cache = new LinkedHashMap<String, SiteObject>();
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

    public SiteObject getSiteObject(String name) {
        return objects.get(name);
    }

    public SiteObject resolve(DocumentModel doc) {
        SiteObject obj =  resolver.resolve(doc, objects);
        if (obj == null) {
            return defaultObject;
        }
        return obj;
    }

    public SiteObject[] getSiteObjects() {
        return objects.values().toArray(new SiteObject[objects.size()]);
    }

    public void registerBinding(SiteObjectBinding binding) {
        bindings.add(binding);
    }

    public void registerSiteObject(SiteObject object) {
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

    public SiteObject getDefaultSiteObject() {
        return defaultObject;
    }

    public void setDefaultSiteObject(SiteObject object) {
        this.defaultObject = object;
    }

}
