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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.url.URLFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.webengine.debug.ReloadManager;
import org.nuxeo.ecm.webengine.loader.WebLoader;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.impl.GlobalTypes;
import org.nuxeo.ecm.webengine.model.impl.ModuleConfiguration;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;
import org.nuxeo.ecm.webengine.model.io.BlobWriter;
import org.nuxeo.ecm.webengine.model.io.FileWriter;
import org.nuxeo.ecm.webengine.model.io.ScriptFileWriter;
import org.nuxeo.ecm.webengine.model.io.TemplateViewWriter;
import org.nuxeo.ecm.webengine.model.io.TemplateWriter;
import org.nuxeo.ecm.webengine.model.io.URLWriter;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.runtime.annotations.AnnotationManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngine implements ResourceLocator {

    public static final String SKIN_PATH_PREFIX_KEY = "org.nuxeo.ecm.webengine.skinPathPrefix";

    protected static final Pattern PATH_PATTERN = Pattern.compile("\\s+@Path\\(\"([^\"]*)\"\\)\\s+");

    private static final Log log = LogFactory.getLog(WebEngine.class);

    private static final ThreadLocal<WebContext> CTX = new ThreadLocal<WebContext>();

    protected static final Map<Object, Object> mimeTypes = loadMimeTypes();

    static Map<Object, Object> loadMimeTypes() {
        Map<Object, Object> mimeTypes = new HashMap<Object, Object>();
        Properties p = new Properties();
        URL url = WebEngine.class.getClassLoader().getResource(
                "OSGI-INF/mime.properties");
        InputStream in = null;
        try {
            in = url.openStream();
            p.load(in);
            mimeTypes.putAll(p);
        } catch (IOException e) {
            throw new Error("Failed to load mime types");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return mimeTypes;
    }

    public static WebContext getActiveContext() {
        return CTX.get();
    }

    public static void setActiveContext(WebContext ctx) {
        CTX.set(ctx);
    }

    protected final File root;

    protected ModuleManager moduleMgr;

    protected final Scripting scripting;

    protected final RenderingEngine rendering;

    protected final Map<String, Object> env;

    protected String devMode;

    protected final GlobalTypes globalTypes;

    protected final AnnotationManager annoMgr;

    protected final ResourceRegistry registry;

    protected String skinPathPrefix;

    protected final List<File> registeredModules = new ArrayList<File>();

    protected final WebLoader webLoader;

    protected ReloadManager reloadMgr;

    public WebEngine(ResourceRegistry registry, File root) {
        this.registry = registry;
        this.root = root;
        devMode = Framework.getProperty("org.nuxeo.dev");
        if (devMode != null) {
            reloadMgr = new ReloadManager(this);
        }
        webLoader = new WebLoader(this);

        scripting = new Scripting(webLoader);
        annoMgr = new AnnotationManager();

        globalTypes = new GlobalTypes(this);

        skinPathPrefix = Framework.getProperty(SKIN_PATH_PREFIX_KEY);
        if (skinPathPrefix == null) {
            // TODO: should put this in web.xml and not use jboss.home.dir to
            // test if on jboss
            skinPathPrefix = System.getProperty("jboss.home.dir") != null ? "/nuxeo/site/skin"
                    : "/skin";
        }

        env = new HashMap<String, Object>();
        env.put("installDir", root);
        env.put("engine", "Nuxeo Web Engine");
        // TODO this should be put in the MANIFEST
        env.put("version", "1.0.0.rc");

        rendering = new FreemarkerEngine();
        rendering.setResourceLocator(this);
        rendering.setSharedVariable("env", getEnvironment());

        // register writers - TODO make an extension point
        // resource writers may generate coding problems so we disable it for
        // now
        // registry.addMessageBodyWriter(new ResourceWriter());
        registry.addMessageBodyWriter(new TemplateWriter());
        registry.addMessageBodyWriter(new ScriptFileWriter());
        registry.addMessageBodyWriter(new BlobWriter());
        registry.addMessageBodyWriter(new FileWriter());
        registry.addMessageBodyWriter(new URLWriter());
        registry.addMessageBodyWriter(new TemplateViewWriter());
    }

    public WebLoader getWebLoader() {
        return webLoader;
    }

    public void setSkinPathPrefix(String skinPathPrefix) {
        this.skinPathPrefix = skinPathPrefix;
    }

    public String getSkinPathPrefix() {
        return skinPathPrefix;
    }

    public ResourceRegistry getRegistry() {
        return registry;
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return webLoader.loadClass(className);
    }

    public GlobalTypes getGlobalTypes() {
        return globalTypes;
    }

    public String getMimeType(String ext) {
        return (String) mimeTypes.get(ext);
    }

    public AnnotationManager getAnnotationManager() {
        return annoMgr;
    }

    public boolean isDevMode() {
        return devMode != null;
    }

    public String getDevMode() {
        return devMode;
    }

    public void setDevMode(String devModeId) {
        this.devMode = devModeId;
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

    public Scripting getScripting() {
        return scripting;
    }

    /**
     * Registers a module reference given its configuration file. The module
     * configuration is not yet loaded. It will be loaded the first time an HTTP
     * request will be made.
     */
    public void registerModule(File config) {
        registeredModules.add(config);
        if (moduleMgr != null) { // avoid synchronizing if not needed
            synchronized (this) {
                if (moduleMgr != null) {
                    moduleMgr.loadModule(config);
                }
            }
        }
    }

    /**
     * Make a copy to avoid concurrent modification exceptions
     *
     * @return
     */
    public File[] getRegisteredModules() {
        return registeredModules.toArray(new File[registeredModules.size()]);
    }

    public ModuleManager getModuleManager() {
        synchronized (this) {
            if (moduleMgr == null) {
                moduleMgr = new ModuleManager(this);
                File deployRoot = getDeploymentDirectory();
                if (deployRoot.isDirectory()) {
                    moduleMgr.loadModules(deployRoot);
                }
                // make a copy to avoid concurrent modifications with
                // registerModule
                for (File mod : registeredModules.toArray(new File[registeredModules.size()])) {
                    moduleMgr.loadModule(mod);
                }
            }
        }
        return moduleMgr;
    }

    public Module getModule(String name) {
        ModuleConfiguration md = getModuleManager().getModule(name);
        if (md != null) {
            return md.get();
        }
        return null;
    }

    public File getRootDirectory() {
        return root;
    }

    public File getDeploymentDirectory() {
        return new File(root, "deploy");
    }

    public File getModulesDirectory() {
        return new File(root, "modules");
    }

    public ReloadManager getReloadManager() {
        return reloadMgr;
    }

    public RenderingEngine getRendering() {
        return rendering;
    }

    /**
     * Manage jax-rs root resource bindings
     */
    public void addResourceBinding(ResourceBinding binding) {
        try {
            binding.resolve(this);
            registry.addBinding(binding);
        } catch (Exception e) {
            throw WebException.wrap("Failed o register binding: " + binding, e);
        }
    }

    public void removeResourceBinding(ResourceBinding binding) {
        registry.removeBinding(binding);
    }

    public ResourceBinding[] getBindings() {
        return registry.getBindings();
    }

    /**
     * Reloads configuration.
     */
    public synchronized void reload() {
        log.info("Reloading WebEngine");
        if (moduleMgr != null) { // avoid synchronizing if not needed
            for (ModuleConfiguration mc : moduleMgr.getModules()) {
                if (mc.isLoaded()) {
                    // this is needed even if the module manager
                    // is rebuild since it may remove groovy file caches
                    mc.get().flushCache();
                }
            }
            synchronized (this) {
                if (moduleMgr != null) {
                    moduleMgr = null;
                }
            }
        }
        webLoader.flushCache();
    }

    public synchronized void reloadModules() {
        moduleMgr.reloadModules();
    }

    public void start() {
        if (reloadMgr != null) {
            reloadMgr.start();
        }
    }

    public void stop() {
        if (reloadMgr != null) {
            reloadMgr.stop();
        }
        registry.clear();
    }

    protected ModuleConfiguration getModuleFromPath(String rootPath, String path) {
        path = path.substring(rootPath.length() + 1);
        int p = path.indexOf('/');
        String moduleName = path;
        if (p > -1) {
            moduleName = path.substring(0, p);
        }
        return moduleMgr.getModule(moduleName);
    }

    /* ResourceLocator API */

    public URL getResourceURL(String key) {
        try {
            return URLFactory.getURL(key);
        } catch (Exception e) {
            return null;
        }
    }

    public File getResourceFile(String key) {
        WebContext ctx = getActiveContext();
        if (key.startsWith("@")) {
            Resource rs = ctx.getTargetObject();
            if (rs != null) {
                return rs.getView(key.substring(1)).script().getFile();
            }
        } else {
            ScriptFile file = ctx.getFile(key);
            if (file != null) {
                return file.getFile();
            }
        }
        return null;
    }

}
