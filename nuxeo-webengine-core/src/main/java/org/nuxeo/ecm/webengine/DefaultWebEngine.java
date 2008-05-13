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

    protected final ObjectRegistry registry;
    protected final Map<String, String> bindings;

    protected Map<String, WebApplication> apps;

    protected final Map<String,Object> env;

    public DefaultWebEngine(File root, RenderingEngine engine) {
        this.root = root;
        scripting = new Scripting(engine);
        registry = new ObjectRegistry();
        bindings = new HashMap<String, String>();
        this.env = new HashMap<String, Object>();
        this.apps = new HashMap<String, WebApplication>();
    }

    public Scripting getScripting() {
        return scripting;
    }

    public File getRootDirectory() {
        return root;
    }


    public void reset() {
        for (WebApplication app : apps.values()) {
            app.flushCache();
        }
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
    public String getTypeBinding(String type) {
        return bindings.get(type);
    }

    public void registerBinding(String type, String objectId) {
        bindings.put(type, objectId);
    }

    public void unregisterBinding(String type) {
        bindings.remove(type);
    }

    public Map<String, Object> getEnvironment() {
        return env;
    }

    public WebApplication getApplication(String name) {
        return apps.get(name);
    }

    public void registerApplication(WebApplicationDescriptor desc) throws WebException {
        apps.put(desc.id, new DefaultWebApplication(this, desc));
    }

    public void unregisterApplication(String id) {
        apps.remove(id);
    }

    public WebApplication[]  getApplications() {
        return apps.values().toArray(new WebApplication[apps.size()]);
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
