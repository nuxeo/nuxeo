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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.webengine.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.forms.FormManager;
import org.nuxeo.ecm.webengine.nl.ResourceComposite;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.ecm.webengine.servlet.WebConst;
import org.nuxeo.ecm.webengine.util.PathMap;
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

    protected final ConcurrentMap<String, WebApplication> apps;
    protected final PathMap<WebApplicationMapping> pathMap;
    protected WebApplicationMapping defaultMapping;
    protected final ReentrantLock mappingLock = new ReentrantLock();

    protected WebApplication defaultApplication;

    protected final Map<String,Object> env;
    protected ResourceBundle messages;

    protected final Map<String, Object> renderingExtensions;

    protected final ListenerList listeners = new ListenerList();
    protected FileChangeNotifier notifier;
    protected long lastMessagesUpdate = 0;
    protected Scripting scripting;

    protected FormManager formMgr;


    public DefaultWebEngine(File root, FileChangeNotifier notifier) throws IOException {
        this.root = root;
        this.notifier = notifier;
        if (notifier != null) {
            notifier.addListener(this);
        }
        registry = new ConcurrentHashMap<String, WebObjectDescriptor>();
        bindings = new HashMap<String, String>();
        formMgr = new FormManager();
        env = new HashMap<String, Object>();
        apps = new ConcurrentHashMap<String, WebApplication>();
        this.pathMap =new PathMap<WebApplicationMapping>();
        env.put("installDir", root);
        env.put("engine", "Nuxeo Web Engine");
        env.put("version", "1.0.0.b1"); //TODO this should be put in the MANIFEST
        renderingExtensions = new Hashtable<String, Object>();
        loadMessageBundle(true);
        scripting = new Scripting();
    }

    public WebApplication getDefaultApplication() {
        if (defaultApplication == null) {
            defaultApplication = apps.get("default");
        }
        return defaultApplication;
    }

    public Path getUrlPath(Path docPath) {
        if (defaultMapping != null) {
            Path basePath = defaultMapping.getRootPath();
            if (basePath != null) {
                Path path = DefaultWebContext.getRelativePath(basePath, docPath);
                if (path != null) {
                    return defaultMapping.getPath().append(path);
                }
            }
        }
        return null;
    }

    public WebContext createContext(HttpServletRequest req, HttpServletResponse resp) throws WebException {
        // normalize the path and remove the action from the path if any
        Path path = null;
        String action = null;
        String reqPath = req.getPathInfo();
        if (reqPath == null) {
            path = PathInfo.ROOT_PATH;
        } else {
            // remove the action
            int p = reqPath.lastIndexOf(WebConst.ACTION_SEPARATOR);
            if (p > -1) {
                action = reqPath.substring(p+WebConst.ACTION_SEPARATOR.length());
                reqPath = reqPath.substring(0, p);
            }
            path = new Path(reqPath).makeAbsolute().removeTrailingSeparator();
        }
        // resolve application
        WebApplicationMapping mapping = getApplicationMapping(path);
        WebApplication app;
        if (mapping != null) {
            // get the application
            app = getApplication(mapping.webApp);
            if (app == null) {
                throw new WebResourceNotFoundException("Application is not registered: "+mapping.webApp);
            }
        } else {
            app = getDefaultApplication(); //use the default app
        }
        // construct the path info
        PathInfo pif = new PathInfo(path, mapping.getPath());
        pif.setAction(action);
        pif.setDocument(mapping.getDocRoot());
        // apply rewrite rules on the path info
        app.getPathMapper().rewrite(pif);
        DefaultWebContext context = new DefaultWebContext(app, pif, req, resp);
        if (mapping != null) {
            // check the application guard if any
            mapping.checkPermission(context); // throw an exception if the user doesn't have the required permission
        }
        // traverse documents if any
        return context;
    }

    public Scripting getScripting() {
        return scripting;
    }

    private void loadMessageBundle(boolean watch) throws IOException {
        File file = new File(root, "i18n");
        WebClassLoader cl = new WebClassLoader();
        cl.addFile(file);
        //messages = ResourceBundle.getBundle("messages", Locale.getDefault(), cl);
        messages = new ResourceComposite(cl);
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
        defaultApplication = null;
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

    public WebApplicationMapping getApplicationMapping(Path path) {
        mappingLock.lock();
        try {
            return pathMap.match(path);
        } finally {
            mappingLock.unlock();
        }
    }

    public void addApplicationMapping(WebApplicationMapping mapping) {
        mappingLock.lock();
        try {
            pathMap.put(mapping.getPath(), mapping);
            if (mapping.isDefault()) {
                defaultMapping = mapping;
            }
        } finally {
            mappingLock.unlock();
        }
        try {
            fireConfigurationChanged();
        } catch (WebException e) {
            e.printStackTrace();
        }
    }

    public void removeApplicationMapping(WebApplicationMapping mapping) {
        mappingLock.lock();
        try {
            pathMap.remove(mapping.getPath());
            if (mapping.isDefault()) { //TODO: stack default mappings so we can revert them back
                defaultMapping = mapping;
            }
        } finally {
            mappingLock.unlock();
        }
        try {
            fireConfigurationChanged();
        } catch (WebException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getEnvironment() {
        return env;
    }

    public WebApplication getApplication(String name) {
        return apps.get(name);
    }

    public FormManager getFormManager() {
        return formMgr;
    }

    public WebApplication getApplicationByPath(Path path) {
        WebApplicationMapping mapping = pathMap.match(path);
        if (mapping != null) {
            return apps.get(mapping.webApp);
        }
        return null;
    }

    public synchronized void registerApplication(WebApplicationDescriptor desc) throws WebException {
        WebApplication app =  new DefaultWebApplication(this, desc);
        apps.put(desc.getId(), app);
        fireConfigurationChanged();
    }

    public synchronized void unregisterApplication(String id) {
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
        if (lastMessagesUpdate == now) {
            return;
        }
        String path = entry.file.getAbsolutePath();
        String rootPath = root.getAbsolutePath();
        if (!path.startsWith(rootPath)) {
            return;
        }
        lastMessagesUpdate = now;
        loadMessageBundle(false);
    }


}
