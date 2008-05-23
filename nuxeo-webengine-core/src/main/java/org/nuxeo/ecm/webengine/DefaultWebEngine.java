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
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.runtime.deploy.FileChangeListener;
import org.nuxeo.runtime.deploy.FileChangeNotifier;
import org.nuxeo.runtime.deploy.FileChangeNotifier.FileEntry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebEngine implements WebEngine, FileChangeListener {

    protected final File root;

    protected final Map<String,WebObjectDescriptor> registry;
    protected final Map<String, String> bindings;

    protected Map<String, WebApplication> apps;

    protected final Map<String,Object> env;
    protected ResourceBundle messages;

    protected Map<String, Object> renderingExtensions;

    protected ListenerList listeners = new ListenerList();
    protected FileChangeNotifier notifier;
    protected long lastMessagesUpdate = 0;

    protected Scripting scripting;

    public DefaultWebEngine(File root, FileChangeNotifier notifier) throws IOException {
        this.root = root;
        this.notifier = notifier;
        if (notifier != null) {
            notifier.addListener(this);
        }
        registry = new Hashtable<String, WebObjectDescriptor>();
        bindings = new HashMap<String, String>();
        this.env = new HashMap<String, Object>();
        this.apps = new HashMap<String, WebApplication>();
        env.put("installDir", root);
        env.put("engine", "Nuxeo Web Engine");
        env.put("version", "1.0.0");
        this.renderingExtensions = new Hashtable<String, Object>();
        loadMessageBundle(true);
        scripting = new Scripting();
    }

    public Scripting getScripting() {
        return scripting;
    }

    private void loadMessageBundle(boolean watch) throws IOException {
        File file = new File(root, "i18n");
        WebClassLoader cl = new WebClassLoader();
        cl.addFile(file);
        messages = ResourceBundle.getBundle("messages", Locale.getDefault(), cl);
        // make a copy to avoid concurrent modifs
        WebApplication[] apps = this.apps.values().toArray(new WebApplication[this.apps.size()]);
        for (WebApplication app : apps) {
            app.getRendering().setMessageBundle(messages);
        }
        if (watch && notifier != null) {
            notifier.watch(file);
            for (File f : file.listFiles()) {
                notifier.watch(f);
            }
        }
    }

    public void destroy() {
        if (notifier != null) {
            notifier.removeListener(this);
            notifier = null;
        }
    }

    /**
     * @return the messages.
     */
    public ResourceBundle getMessageBundle() {
        return messages;
    }


    public File getRootDirectory() {
        return root;
    }


    public void reset() {
        for (WebApplication app : apps.values()) {
            app.flushCache();
        }
    }

    public WebObjectDescriptor getObject(String id) {
        return registry.get(id);
    }

    public Collection<WebObjectDescriptor> getObjects() {
        return registry.values();
    }

    public void registerObject(WebObjectDescriptor obj) {
        registry.put(obj.getId(), obj);
    }

    public void unregisterObject(WebObjectDescriptor obj) {
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
        apps.put(desc.getId(), app);
        fireConfigurationChanged();
    }

    public void unregisterApplication(String id) {
        apps.remove(id);
    }

    public WebApplication[]  getApplications() {
        return apps.values().toArray(new WebApplication[apps.size()]);
    }


    public void registerRenderingExtension(String id, Object obj) {
        renderingExtensions.put(id, obj);
        // notify all registered applications about the new template
        for (WebApplication app : apps.values()) {
            app.registerRenderingExtension(id, obj);
        }
    }

    public void unregisterRenderingExtension(String id) {
        renderingExtensions.remove(id);
        for (WebApplication app : apps.values()) {
            app.unregisterRenderingExtension(id);
        }
    }

    public Map<String, Object> getRenderingExtensions() {
        return renderingExtensions;
    }

    public Object getRenderingExtension(String id) {
        return renderingExtensions.get(id);
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

    public FileChangeNotifier getFileChangeNotifier() {
        return notifier;
    }

    public void fileChanged(FileEntry entry, long now) throws Exception {
        if (lastMessagesUpdate == now) return;
        String path = entry.file.getAbsolutePath();
        String rootPath = root.getAbsolutePath();
        if (!path.startsWith(rootPath)) {
            return;
        }
        lastMessagesUpdate = now;
        loadMessageBundle(false);
    }
}
