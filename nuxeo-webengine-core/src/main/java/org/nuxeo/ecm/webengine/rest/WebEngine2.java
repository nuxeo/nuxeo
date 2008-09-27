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

package org.nuxeo.ecm.webengine.rest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.url.URLFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.webengine.WebClassLoader;
import org.nuxeo.ecm.webengine.nl.ResourceComposite;
import org.nuxeo.ecm.webengine.rest.annotations.Action;
import org.nuxeo.ecm.webengine.rest.annotations.Type;
import org.nuxeo.ecm.webengine.rest.impl.ApplicationDescriptor;
import org.nuxeo.ecm.webengine.rest.impl.ApplicationRegistry;
import org.nuxeo.ecm.webengine.rest.impl.GlobalTypesLoader;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.rest.scripting.Scripting;
import org.nuxeo.runtime.annotations.loader.BundleAnnotationsLoader;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.deploy.FileChangeListener;
import org.nuxeo.runtime.deploy.FileChangeNotifier;
import org.nuxeo.runtime.deploy.FileChangeNotifier.FileEntry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngine2 implements FileChangeListener, ResourceLocator {

    private final static ThreadLocal<WebContext2> CTX = new ThreadLocal<WebContext2>();
    
    public final static WebContext2 getActiveContext() {
        return CTX.get();
    }

    public final static void setActiveContext(WebContext2 ctx) {
        CTX.set(ctx);
    }

    
    protected File root;
    protected ApplicationRegistry appReg;
    protected FileChangeNotifier notifier;
    protected volatile long lastMessagesUpdate = 0;
    protected Scripting scripting;
    protected RenderingEngine rendering;
    protected Map<String, Object> env;
    protected ResourceBundle messages;
    protected Map<String, Object> renderingExtensions; //TODO this should be moved in rendering project
    protected boolean isDebug = false;

    protected GlobalTypesLoader globalTypes;
    protected List<ResourceBinding> bindings = new Vector<ResourceBinding>();



    public WebEngine2(File root, FileChangeNotifier notifier) throws IOException {
        isDebug = Boolean.parseBoolean(Framework.getProperty("debug", "false"));
        this.root = root;
        this.notifier = notifier;
        this.scripting = new Scripting(isDebug);
        String cp = System.getProperty("groovy.classpath");
        if (cp == null) {
            cp = new File(root, "classes").getAbsolutePath();        
        }
        scripting.addClassPath(new File(root,".").getAbsolutePath());
        scripting.addClassPath(cp);
        
        this.globalTypes = new GlobalTypesLoader(this);
        BundleAnnotationsLoader.getInstance().addLoader(Type.class.getName(), globalTypes);
        BundleAnnotationsLoader.getInstance().addLoader(Action.class.getName(), globalTypes);        
        
        loadApplications();
        
        this.renderingExtensions = new Hashtable<String, Object>();
        this.env = new HashMap<String, Object>();
        env.put("installDir", root);
        env.put("engine", "Nuxeo Web Engine");
        env.put("version", "1.0.0.b1"); //TODO this should be put in the MANIFEST

        rendering = new FreemarkerEngine();
        rendering.setResourceLocator(this);
        rendering.setSharedVariable("env", getEnvironment());
        Map<String, Object> renderingExtensions = getRenderingExtensions();
        for (Map.Entry<String, Object> entry : renderingExtensions.entrySet()) {
            rendering.setSharedVariable(entry.getKey(), entry.getValue());
        }
        loadMessageBundle(true);
        if (notifier != null) {
            notifier.addListener(this);
        }
    }
    
    public GlobalTypesLoader getGlobalTypes() {
        return globalTypes;
    }

    private void loadMessageBundle(boolean watch) throws IOException {
        File file = new File(root, "i18n");
        WebClassLoader cl = new WebClassLoader();
        cl.addFile(file);
        //messages = ResourceBundle.getBundle("messages", Locale.getDefault(), cl);
        messages = new ResourceComposite(cl);
        rendering.setMessageBundle(messages);
        // make a copy to avoid concurrent modifs
        if (watch && notifier != null) {
            notifier.watch(file);
            for (File f : file.listFiles()) {
                notifier.watch(f);
            }
        }
    }
    

    protected Context createXMapContext() {
        return new Context() {
            private static final long serialVersionUID = 1L;
            @Override
            public Class<?> loadClass(String className)
                    throws ClassNotFoundException {
                return scripting.loadClass(className);
            }
            @Override
            public URL getResource(String name) {
                return scripting.getGroovyScripting().getGroovyClassLoader().getResource(name);
            }
        };
    }

    protected  void loadApplications() {
        appReg = new ApplicationRegistry(this);
        for (File file : root.listFiles()) {
            if (file.isDirectory()) {
                ApplicationDescriptor ad = null;
                File appFile = new File(file, "Main.groovy");
                if (appFile.isFile()) {       
                    ad = loadApplicationDescriptor(file.getName()+".Main");
                } else {
                    appFile = new File(file, "application.xml");
                    if (appFile.isFile()) {
                        ad = loadApplicationDescriptor(appFile);
                    }
                }
                if (ad != null) {
                    try {
                        appReg.registerDescriptor(file, ad);
                        notifier.watch(appFile); // always track the file
                    } catch (Exception e) {                
                        e.printStackTrace(); // TODO log
                    }                    
                }
            }
        }
    }

    
    /**
     * Load an application given its annotated class 
     * @param clazz
     */
    protected synchronized ApplicationDescriptor loadApplicationDescriptor(String className) {
        try {
            Class<?> clazz = scripting.loadClass(className);
            return ApplicationDescriptor.fromAnnotation(clazz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); //TODO log
            // do nothing
        }
        return null;
    }
    
    /**
     * load an application given its configuration file
     * @param ctx
     * @param cfgFile
     */
    protected synchronized ApplicationDescriptor loadApplicationDescriptor(File cfgFile) {
        try {
            XMap xmap = new XMap();
            xmap.register(ApplicationDescriptor.class);
            InputStream in = new BufferedInputStream(new FileInputStream(cfgFile));            
            return (ApplicationDescriptor)xmap.load(createXMapContext(), in);
        } catch (Exception e) {
            e.printStackTrace(); // TODO log exception
        }
        return null;
    }
    
    public boolean isDebug() {
        return isDebug;
    }

    /**
     * @param isDebug the isDebug to set.
     */
    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public Map<String, Object> getRenderingExtensions() {
        return renderingExtensions;
    }

    public Object getRenderingExtension(String id) {
        return renderingExtensions.get(id);
    }

    public void registerRenderingExtension(String id, Object obj) {
        renderingExtensions.put(id, obj);
    }

    public void unregisterRenderingExtension(String id) {
        renderingExtensions.remove(id);
    }

    public Map<String, Object> getEnvironment() {
        return env;
    }

    public ResourceBundle getMessageBundle() {
        return messages;
    }

    /**
     * @return the scripting.
     */
    public Scripting getScripting() {
        return scripting;
    }

    public ApplicationRegistry getApplicationRegistry() {
        return appReg;
    }
    
    public WebApplication getApplication(String name) {
        return appReg.getApplication(name); 
    }
        
    public File getRootDirectory() {
        return root;
    }

    public FileChangeNotifier getFileChangeNotifier() {
        return notifier;
    }

    public RenderingEngine getRendering() {
        return rendering;
    }

    /** Manage jax-rs root resource bindings */
    public void addResourceBinding(ResourceBinding binding) {
        bindings.add(binding);
    }

    public void removeResourceBinding(ResourceBinding binding) {
        bindings.remove(binding);
    }

    public ResourceBinding[] getBindings() {
        return bindings.toArray(new ResourceBinding[bindings.size()]);
    }

    /**
     * Reload configuration
     */
    public void reload() {
        bindings.clear();
        //TODO
        //defaultApplication = null;
        //for (WebApplication app : apps.values()) {
        //    app.flushCache();
        //}
    }

    public void destroy() {
        if (notifier != null) {
            notifier.removeListener(this);
            notifier = null;
        }
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
        if (path.endsWith("/Main.groovy") || path.endsWith("/application.xml")) {
            loadApplications(); // TODO optimize reloading
        } else {
            loadMessageBundle(false);
        }
        lastMessagesUpdate = now;        
    }

    /** ResourceLocator API */

    public URL getResourceURL(String key) {
        try {
            return URLFactory.getURL(key);
        } catch (Exception e) {
            return null;
        }
    }

    public File getResourceFile(String key) {
        try {
            WebContext2 ctx = WebEngine2.getActiveContext();
            ScriptFile file = ctx.getFile(key);
            if (file != null) {
                return file.getFile();
            }
        } catch (IOException e) {
        }
        return null;
    }
}
