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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.platform.rendering.api.RenderingTransformer;
import org.nuxeo.ecm.webengine.util.DependencyTree;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebEngine implements WebEngine {

    protected final File root;

    protected final ObjectRegistry registry;
    protected final Map<String, String> bindings;

    protected Map<String, WebApplication> apps;

    protected final Map<String,Object> env;
    protected ResourceBundle messages;

    protected Map<String, Object> templates;
    protected Map<String, RenderingTransformer> transformers;

    protected ListenerList listeners = new ListenerList();

    public DefaultWebEngine(File root, ResourceBundle messages) {
        this.root = root;
        this.messages = messages;
        registry = new ObjectRegistry();
        bindings = new HashMap<String, String>();
        this.env = new HashMap<String, Object>();
        this.apps = new HashMap<String, WebApplication>();
        env.put("installDir", root);
        env.put("engine", "Nuxeo Web Engine");
        env.put("version", "1.0.0");
        this.transformers = new Hashtable<String, RenderingTransformer>();
        this.templates = new Hashtable<String, Object>();
    }

    /**
     * @return the messages.
     */
    public ResourceBundle getMessages() {
        return messages;
    }

    /**
     * @param messages the messages to set.
     */
    public void setMessages(ResourceBundle messages) {
        this.messages = messages;
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
        WebApplication app =  new DefaultWebApplication(this, desc);
        apps.put(desc.id, app);
    }

    public void unregisterApplication(String id) {
        apps.remove(id);
    }

    public WebApplication[]  getApplications() {
        return apps.values().toArray(new WebApplication[apps.size()]);
    }


    public void registerRenderingTemplate(String id, Object obj) {
        templates.put(id, obj);
        // notify all registered applications about the new template
        for (WebApplication app : apps.values()) {
            app.registerTemplate(id, obj);
        }
    }

    public void unregisterRenderingTemplate(String id) {
        templates.remove(id);
        for (WebApplication app : apps.values()) {
            app.unregisterTemplate(id);
        }
    }

    public void registerRenderingTransformer(String id, RenderingTransformer obj) {
        transformers.put(id, obj);
        for (WebApplication app : apps.values()) {
            app.registerTransformer(id, obj);
        }
    }

    public void unregisterRenderingTransformer(String id) {
        transformers.remove(id);
        for (WebApplication app : apps.values()) {
            app.unregisterTransformer(id);
        }
    }

    /**
     * @return the transformers.
     */
    public Map<String, RenderingTransformer> getTransformers() {
        return transformers;
    }

    /**
     * @return the templates.
     */
    public Map<String, Object> getTemplates() {
        return templates;
    }

    public Object getRenderingTemplate(String id) {
        return templates.get(id);
    }

    public RenderingTransformer getRenderingTransformer(String id) {
        return transformers.get(id);
    }

    public void addConfigurationChangedListener(
            ConfigurationChangedListener listener) {
        listeners.add(listener);
    }

    public void removeConfigurationChangedListener(
            ConfigurationChangedListener listener) {
        listeners.remove(listener);
    }

    public void fireConfigurationChanged() throws WebException {
        reset();
        for (Object obj : listeners.getListenersCopy()) {
            ((ConfigurationChangedListener)obj).configurationChanged(this);
        }
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
