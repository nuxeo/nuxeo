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

package org.nuxeo.ecm.webengine;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.ecm.webengine.util.DependencyTree;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultSiteManager implements SiteManager {

    protected final Scripting scripting;
    protected final File root;

    protected final ConcurrentMap<String, SiteRoot> roots;
    protected SiteRoot defaultRoot;

    protected final ObjectRegistry registry;
    protected final Map<String, ObjectDescriptor> objects; // instances for each document type

    public DefaultSiteManager(File root, RenderingEngine engine) {
        this.root = root;
        roots = new ConcurrentHashMap<String, SiteRoot>();
        scripting = new Scripting(engine);
        registry = new ObjectRegistry();
        objects = new HashMap<String, ObjectDescriptor>();
    }

    public Scripting getScripting() {
        return scripting;
    }

    public File getRootDirectory() {
        return root;
    }

    public SiteRoot getDefaultSiteRoot() throws Exception {
        if (defaultRoot == null) {
            File dir = new File(root, "default");
            defaultRoot = new SiteRoot(this, dir);
            defaultRoot.loadConfiguration();
        }
        return defaultRoot;
    }

    public SiteRoot getSiteRoot(String name) throws Exception {
        SiteRoot sroot = roots.get(name);
        if (sroot == null) {
            File dir = new File(root, name);
            File metadata = new File(dir, name);
            if (metadata.isFile()) {
                sroot = new SiteRoot(this, dir);
                sroot.loadConfiguration();
                roots.putIfAbsent(name, sroot);
            } else { // try dynamic binding
                sroot = getDefaultSiteRoot();
            }
        }
        return sroot;
    }

    public void reset() {
        //TODO
    }

    public synchronized ObjectDescriptor getInstanceOf(String type) {
        ObjectDescriptor obj = objects.get(type);
        return obj != null ? obj : objects.get("Document");
    }

    public synchronized ObjectDescriptor getObject(String id) {
        return registry.get(id);
    }

    public synchronized boolean isObjectResolved(String id) {
        return registry.isResolved(id);
    }

    public synchronized List<ObjectDescriptor> getPendingObjects() {
        return registry.getPendingObjects();
    }

    public synchronized List<ObjectDescriptor> getRegisteredObjects() {
        return registry.getRegisteredObjects();
    }

    public synchronized List<ObjectDescriptor> getResolvedObjects() {
        return registry.getResolvedObjects();
    }

    public synchronized void registerObject(ObjectDescriptor obj) {
        registry.add(obj.getId(), obj);
        objects.put(obj.getType(), obj); // TODO keep a stack of objects or use a chained list
    }

    public synchronized void unregisterObject(ObjectDescriptor obj) {
        registry.remove(obj.getId());
        objects.remove(obj.getType());
    }


    class ObjectRegistry extends DependencyTree<String, ObjectDescriptor> {

        public void resolved(DependencyTree.Entry<String, ObjectDescriptor> entry) {
            ObjectDescriptor obj = entry.get();
            String base = obj.getBase();
            if (base  == null) {
                base = "default";
            }
            ObjectDescriptor baseObj = registry.getResolved(base);
            if (baseObj != null) { // compute inheritance data
                obj.merge(baseObj);
            }
        }

        public void unresolved(DependencyTree.Entry<String, ObjectDescriptor> entry) {
            // do nothing
        }

    }


}
