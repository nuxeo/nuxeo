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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.ecm.webengine.util.DependencyTree;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebEngine implements WebEngine {

    protected final Scripting scripting;
    protected final File root;

    protected final ConcurrentMap<String, WebRoot> roots;
    protected WebRoot defaultRoot;

    protected final ObjectRegistry registry;
    protected final Map<String, ObjectDescriptor> objects; // instances for each document type
    protected final Map<String, String> bindings;

    protected final Map<String,Object> env;

    public DefaultWebEngine(File root, RenderingEngine engine) {
        this.root = root;
        roots = new ConcurrentHashMap<String, WebRoot>();
        scripting = new Scripting(engine);
        registry = new ObjectRegistry();
        objects = new HashMap<String, ObjectDescriptor>();
        bindings = new HashMap<String, String>();
        this.env = new HashMap<String, Object>();
    }

    public Scripting getScripting() {
        return scripting;
    }

    public File getRootDirectory() {
        return root;
    }

    public WebRoot getDefaultSiteRoot() throws WebException {
        if (defaultRoot == null) {
            File dir = new File(root, "default");
            defaultRoot = new WebRoot(this, dir);
            try {
                defaultRoot.loadConfiguration();
            } catch (IOException e) {
                throw new WebException("Failed to load configuration for web root default");
            }
        }
        return defaultRoot;
    }

    public WebRoot getSiteRoot(String name) throws WebException {
        WebRoot sroot = roots.get(name);
        if (sroot == null) {
            File dir = new File(root, name);
            File metadata = new File(dir, name);
            if (metadata.isFile()) {
                sroot = new WebRoot(this, dir);
                try {
                    sroot.loadConfiguration();
                } catch (IOException e) {
                    throw new WebException("Failed to load configuration for web root "+name);
                }
                roots.putIfAbsent(name, sroot);
            } else { // try dynamic binding
                sroot = getDefaultSiteRoot();
            }
        }
        return sroot;
    }

    public void reset() {
        objects.clear(); // clear cache
    }

    public synchronized ObjectDescriptor getDefaultObject() {
        ObjectDescriptor obj = objects.get("Document");
        if (obj == null) {
            String id = bindings.get("Document");
            if (id == null) {
                throw new IllegalStateException("The web object bindings are not correctly configured. You must specify a binding for the Document type.");
            }
            obj = registry.get(id);
            if (obj == null) {
                throw new IllegalStateException("The web object bindings are not correctly configured. The object "+id+" bound to Document type was not found.");
            }
            objects.put("Document", obj);
        }
        return obj;
    }

    public synchronized ObjectDescriptor getInstanceOf(Type type) {
        String typeName = type.getName();
        ObjectDescriptor obj = objects.get(typeName);
        if (obj == null) {
            String id = bindings.get(typeName);
            if (id == null) {
                Type stype = type.getSuperType();
                if (stype == null || stype.getName().equals("Document")) {// the default
                    obj = getDefaultObject();
                } else {
                    obj = getInstanceOf(stype);
                }
            } else {
                obj = registry.get(id);
                if (obj == null) {
                    Type stype = type.getSuperType();
                    if (stype == null || stype.getName().equals("Document")) {// the default
                        obj = getDefaultObject();
                    } else {
                        obj = getInstanceOf(stype);
                    }
                }
            }
            objects.put(typeName, obj);
        }
        return obj;
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
    }

    public synchronized void unregisterObject(ObjectDescriptor obj) {
        registry.remove(obj.getId());
    }

    //TODO bindings are not correctly redeployed when contributed from several web.xml files
    public String getBingding(String type) {
        return bindings.get(type);
    }

    public void registerBingding(String type, String objectId) {
        bindings.put(type, objectId);
    }

    public void unregisterBingding(String type) {
        bindings.remove(type);
    }

    public Map<String, Object> getEnvironment() {
        return env;
    }

    class ObjectRegistry extends DependencyTree<String, ObjectDescriptor> {

        public void resolved(DependencyTree.Entry<String, ObjectDescriptor> entry) {
            ObjectDescriptor obj = entry.get();
            String base = obj.getBase();
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
