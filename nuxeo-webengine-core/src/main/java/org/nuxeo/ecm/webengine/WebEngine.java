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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.ws.rs.Path;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.url.URLFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.fm.i18n.ResourceComposite;
import org.nuxeo.ecm.webengine.loader.WebClassLoader;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.WebService;
import org.nuxeo.ecm.webengine.model.impl.BundleTypeProvider;
import org.nuxeo.ecm.webengine.model.impl.DirectoryTypeProvider;
import org.nuxeo.ecm.webengine.model.impl.ModuleDescriptor;
import org.nuxeo.ecm.webengine.model.impl.ModuleImpl;
import org.nuxeo.ecm.webengine.model.impl.ModuleRegistry;
import org.nuxeo.ecm.webengine.model.io.ResourceWriter;
import org.nuxeo.ecm.webengine.model.io.ScriptFileWriter;
import org.nuxeo.ecm.webengine.model.io.TemplateWriter;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.runtime.annotations.AnnotationManager;
import org.nuxeo.runtime.annotations.loader.AnnotationLoader;
import org.nuxeo.runtime.annotations.loader.BundleAnnotationsLoader;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.deploy.FileChangeListener;
import org.nuxeo.runtime.deploy.FileChangeNotifier;
import org.nuxeo.runtime.deploy.FileChangeNotifier.FileEntry;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngine implements FileChangeListener, ResourceLocator, AnnotationLoader {

    private final static ThreadLocal<WebContext> CTX = new ThreadLocal<WebContext>();
    
    protected static Map<Object, Object> mimeTypes = loadMimeTypes();
    
    static Map<Object, Object> loadMimeTypes() {
        HashMap<Object,Object> mimeTypes = new HashMap<Object, Object>();
        Properties p = new Properties();
        URL url = WebEngine.class.getClassLoader().getResource("OSGI-INF/mime.properties");
        InputStream in = null;
        try {
            in = url.openStream();
            p.load(in);
            mimeTypes.putAll(p);
        } catch (IOException e) {
            throw new Error("Failed to load mime types");
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) {}
            }
        }
        return mimeTypes;
    }
    
    public final static WebContext getActiveContext() {
        return CTX.get();
    }

    public final static void setActiveContext(WebContext ctx) {
        CTX.set(ctx);
    }

    
    protected File root;
    protected ModuleRegistry moduleReg;
    protected FileChangeNotifier notifier;
    protected volatile long lastMessagesUpdate = 0;
    protected Scripting scripting;
    protected RenderingEngine rendering;
    protected Map<String, Object> env;
    protected ResourceBundle messages;
    protected boolean isDebug = false;

    protected BundleTypeProvider bundleTypeProvider;
    protected DirectoryTypeProvider directoryTypeProvider;    

    protected AnnotationManager annoMgr;

    protected ResourceRegistry registry;

    
    public WebEngine(ResourceRegistry registry, File root) throws IOException {
        this.registry = registry;
        isDebug = Boolean.parseBoolean(Framework.getProperty("debug", "false"));
        this.root = root;
        if (isDebug) { // TODO notifier must be intialized by WebEngine
            this.notifier = new FileChangeNotifier();
            notifier.start();
        }
        this.scripting = new Scripting(isDebug);
        this.annoMgr = new AnnotationManager();
        String cp = System.getProperty("groovy.classpath");
        if (cp == null) {
            cp = new File(root, "classes").getAbsolutePath();        
        }
        scripting.addClassPath(new File(root,".").getAbsolutePath());
        scripting.addClassPath(cp);
        
        this.bundleTypeProvider = new BundleTypeProvider();
        this.directoryTypeProvider = new DirectoryTypeProvider(this);
        BundleAnnotationsLoader.getInstance().addLoader(WebObject.class.getName(), bundleTypeProvider);
        BundleAnnotationsLoader.getInstance().addLoader(WebService.class.getName(), bundleTypeProvider);        
        
        loadModules();
        
        this.env = new HashMap<String, Object>();
        env.put("installDir", root);
        env.put("engine", "Nuxeo Web Engine");
        env.put("version", "1.0.0.b1"); //TODO this should be put in the MANIFEST

        rendering = new FreemarkerEngine();
        rendering.setResourceLocator(this);
        rendering.setSharedVariable("env", getEnvironment());
        loadMessageBundle(true);
        if (notifier != null) {
            notifier.addListener(this);
        }
        // register annotation loader
        BundleAnnotationsLoader.getInstance().addLoader(Path.class.getName(), this);
        
        // register writers
        registry.addMessageBodyWriter(new ResourceWriter());
        registry.addMessageBodyWriter(new TemplateWriter());
        registry.addMessageBodyWriter(new ScriptFileWriter());
    }
    
    /**
     * @return the registry.
     */
    public ResourceRegistry getRegistry() {
        return registry;
    }
    
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return scripting.loadClass(className);
    }
    
    public BundleTypeProvider getBundleTypeProvider() {
        return bundleTypeProvider;
    }
    
    public DirectoryTypeProvider getDirectoryTypeProvider() {
        return directoryTypeProvider;
    }
    
    public String getMimeType(String ext) {
        return (String)mimeTypes.get(ext);
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
    
    public AnnotationManager getAnnotationManager() {
        return annoMgr;
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

    protected  void loadModules() {
        moduleReg = new ModuleRegistry(this);
        for (File file : root.listFiles()) {
            try {
                if (file.isDirectory()) {
                    ModuleDescriptor ad = null;
                    File appFile = new File(file, "Main.groovy");
                    if (appFile.isFile()) {       
                        ad = loadModuleDescriptor(file.getName()+".Main");
                        if (notifier != null) { 
                            notifier.watch(appFile);
                        }
                        appFile = new File(file, "module.xml");
                        if (appFile.isFile()) {
                            ModuleDescriptor ad2 = loadModuleDescriptor(appFile);
                            ad.links = ad2.links;
                            ad2.links = null;
                            if (notifier != null) { 
                                notifier.watch(appFile);
                            }
                        }
                    } else {
                        appFile = new File(file, "module.xml");
                        if (appFile.isFile()) {
                            ad = loadModuleDescriptor(appFile);
                        }
                    }
                    if (ad != null) {
                        moduleReg.registerDescriptor(file, ad);                       
                        if (notifier != null) {
                            watchModule(file);
                        }
                        notifier.watch(appFile); // always track the file
                    }
                }
            } catch (Exception e) {                
                e.printStackTrace(); // TODO log
            }                    
        }
    }

    

    protected void watchModule(File root) throws IOException {
        notifier.watch(root);
        for (File f : root.listFiles()) {
            if (f.isDirectory()) {
                watchModule(f);
            }
        }
    }
    
    /**
     * Load an module given its annotated class 
     * @param clazz
     */
    protected synchronized ModuleDescriptor loadModuleDescriptor(String className) {
        try {
            Class<?> clazz = scripting.loadClass(className);
            ModuleDescriptor ad = ModuleDescriptor.fromAnnotation(clazz);
            if (ad != null) {
                ad.binding = ResourceBinding.fromAnnotation(clazz);
                if (ad.binding != null) {
                    addResourceBinding(ad.binding);
                }
            }            
            return ad;
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); //TODO log
            // do nothing
        }
        return null;
    }
    
    /**
     * load an module given its configuration file
     * @param ctx
     * @param cfgFile
     */
    protected synchronized ModuleDescriptor loadModuleDescriptor(File cfgFile) {
        try {
            XMap xmap = new XMap();
            xmap.register(ModuleDescriptor.class);
            InputStream in = new BufferedInputStream(new FileInputStream(cfgFile));            
            return (ModuleDescriptor)xmap.load(createXMapContext(), in);
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

    public void registerRenderingExtension(String id, Object obj) {
        rendering.setSharedVariable(id, obj);
    }

    public void unregisterRenderingExtension(String id) {
        rendering.setSharedVariable(id, null);
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

    public ModuleRegistry getModuleRegistry() {
        return moduleReg;
    }
    
    public Module getModule(String name) {
        return moduleReg.getModule(name); 
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
        registry.addBinding(binding);
    }

    public void removeResourceBinding(ResourceBinding binding) {
        registry.removeBinding(binding);
    }

    public ResourceBinding[] getBindings() {
        return registry.getBindings();
    }

    /**
     * Reload configuration
     */
    public synchronized void reload() {
        bundleTypeProvider.flushCache();
        directoryTypeProvider.flushCache();
        reloadModules();
    }

    public synchronized void reloadModules() {
        for (Module module : moduleReg.getModules()) {
            ResourceBinding binding = module.getModuleBinding();
            registry.removeBinding(binding);
        }
        loadModules();
    }

    public void destroy() {
        if (notifier != null) {
            notifier.removeListener(this);
            notifier = null;
        }
        registry.clear();
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
        if (entry.file.isFile()) {
            return;
        }
        if (entry.file.getParentFile().equals(root)) {
            reload();
        } else if (path.endsWith("/i18n")) {
            loadMessageBundle(false);
        } else {
            path = path.substring(rootPath.length()+1);
            int p = path.indexOf('/');
            String moduleName = path; 
            if (p > -1) {
                moduleName = path.substring(0, p); 
            }
            Module module = getModule(moduleName);
            if (module != null) {
                if (path.contains("/skin")) {
                    if (module instanceof ModuleImpl) {
                        ((ModuleImpl)module).flushSkinCache();
                    } else {
                        module.flushCache();    
                    }
                } else {
                    module.flushCache();
                }
            }
        }
        lastMessagesUpdate = now;        
    }

    public void loadAnnotation(Bundle bundle, String annoType,
            String className, String[] args) throws Exception {
        if (Path.class.getName().equals(annoType)) {
            Class<?> clazz = bundle.loadClass(className);
            Path p = clazz.getAnnotation(Path.class);
            ResourceBinding rb = new ResourceBinding();
            rb.path = p.value();
            rb.clazz = clazz;
            addResourceBinding(rb);
        }
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
        WebContext ctx = WebEngine.getActiveContext();
        ScriptFile file = ctx.getFile(key);
        if (file != null) {
            return file.getFile();
        }
        return null;
    }
    
            
}
