/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.AdapterNotFoundException;
import org.nuxeo.ecm.webengine.model.AdapterType;
import org.nuxeo.ecm.webengine.model.LinkDescriptor;
import org.nuxeo.ecm.webengine.model.Messages;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.TypeNotFoundException;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * The default implementation for a web configuration.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ModuleImpl implements Module {

    private static final Log log = LogFactory.getLog(ModuleImpl.class);

    protected final WebEngine engine;

    protected final Object typeLock = new Object();

    protected TypeRegistry typeReg;

    protected final ModuleConfiguration configuration;

    protected final ModuleImpl superModule;

    protected LinkRegistry linkReg;

    protected final String skinPathPrefix;

    /**
     * @deprecated Use {@link WebApplication} to declare modules - modules may have multiple roots
     * @return
     */
    @Deprecated
    protected ResourceType rootType;

    protected Messages messages;

    protected DirectoryStack dirStack;

    // cache used for resolved files
    protected ConcurrentMap<String, ScriptFile> fileCache;

    public ModuleImpl(WebEngine engine, ModuleImpl superModule, ModuleConfiguration config) {
        this.engine = engine;
        this.superModule = superModule;
        configuration = config;
        skinPathPrefix = new StringBuilder().append(engine.getSkinPathPrefix()).append('/').append(config.name).toString();
        fileCache = new ConcurrentHashMap<String, ScriptFile>();
        loadConfiguration();
        reloadMessages();
        loadDirectoryStack();
    }

    /**
     * Whether or not this module has a GUI and should be listed in available GUI module list. For example, REST modules
     * usually don't have a GUI.
     *
     * @return true if headless (no GUI is provided), false otherwise
     */
    public boolean isHeadless() {
        return configuration.isHeadless;
    }

    /**
     * @return the natures, or null if no natures were specified
     */
    public Set<String> getNatures() {
        return configuration.natures;
    }

    public boolean hasNature(String natureId) {
        return configuration.natures != null && configuration.natures.contains(natureId);
    }

    @Override
    public WebEngine getEngine() {
        return engine;
    }

    @Override
    public String getName() {
        return configuration.name;
    }

    @Override
    public ModuleImpl getSuperModule() {
        return superModule;
    }

    public ModuleConfiguration getModuleConfiguration() {
        return configuration;
    }

    /**
     * @deprecated Use {@link WebApplication} to declare modules
     * @return
     */
    @Deprecated
    public ResourceType getRootType() {
        // force type registry creation if needed
        getTypeRegistry();
        if (rootType == null) {
            throw new IllegalStateException("You use new web module declaration - should not call this compat. method");
        }
        return rootType;
    }

    /**
     * @deprecated Use {@link WebApplication} to declare modules
     * @return
     */
    @Override
    @Deprecated
    public Resource getRootObject(WebContext ctx) {
        ((AbstractWebContext) ctx).setModule(this);
        Resource obj = ctx.newObject(getRootType());
        obj.setRoot(true);
        return obj;
    }

    @Override
    public String getSkinPathPrefix() {
        return skinPathPrefix;
    }

    public TypeRegistry getTypeRegistry() {
        if (typeReg == null) { // create type registry if not already created
            synchronized (typeLock) {
                if (typeReg == null) {
                    typeReg = createTypeRegistry();
                    if (configuration.rootType != null) {
                        // compatibility code for avoiding NPE
                        rootType = typeReg.getType(configuration.rootType);
                    }
                }
            }
        }
        return typeReg;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return engine.loadClass(className);
    }

    @Override
    public ResourceType getType(String typeName) {
        ResourceType type = getTypeRegistry().getType(typeName);
        if (type == null) {
            throw new TypeNotFoundException(typeName);
        }
        return type;
    }

    @Override
    public ResourceType[] getTypes() {
        return getTypeRegistry().getTypes();
    }

    @Override
    public AdapterType[] getAdapters() {
        return getTypeRegistry().getAdapters();
    }

    @Override
    public AdapterType getAdapter(Resource ctx, String name) {
        AdapterType type = getTypeRegistry().getAdapter(ctx, name);
        if (type == null) {
            throw new AdapterNotFoundException(ctx, name);
        }
        return type;
    }

    @Override
    public List<String> getAdapterNames(Resource ctx) {
        return getTypeRegistry().getAdapterNames(ctx);
    }

    @Override
    public List<AdapterType> getAdapters(Resource ctx) {
        return getTypeRegistry().getAdapters(ctx);
    }

    @Override
    public List<String> getEnabledAdapterNames(Resource ctx) {
        return getTypeRegistry().getEnabledAdapterNames(ctx);
    }

    @Override
    public List<AdapterType> getEnabledAdapters(Resource ctx) {
        return getTypeRegistry().getEnabledAdapters(ctx);
    }

    @Override
    public String getMediaTypeId(MediaType mt) {
        if (configuration.mediatTypeRefs == null) {
            return null;
        }
        MediaTypeRef[] refs = configuration.mediatTypeRefs;
        for (MediaTypeRef ref : refs) {
            String id = ref.match(mt);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    @Override
    public List<ResourceBinding> getResourceBindings() {
        return configuration.resources;
    }

    @Override
    public boolean isDerivedFrom(String moduleName) {
        if (configuration.name.equals(moduleName)) {
            return true;
        }
        if (superModule != null) {
            return superModule.isDerivedFrom(moduleName);
        }
        return false;
    }

    public void loadConfiguration() {
        linkReg = new LinkRegistry();
        if (configuration.links != null) {
            for (LinkDescriptor link : configuration.links) {
                linkReg.registerLink(link);
            }
        }
        configuration.links = null; // avoid storing unused data
    }

    @Override
    public List<LinkDescriptor> getLinks(String category) {
        return linkReg.getLinks(category);
    }

    @Override
    public List<LinkDescriptor> getActiveLinks(Resource context, String category) {
        return linkReg.getActiveLinks(context, category);
    }

    public LinkRegistry getLinkRegistry() {
        return linkReg;
    }

    @Override
    public String getTemplateFileExt() {
        return configuration.templateFileExt;
    }

    public void flushSkinCache() {
        log.info("Flushing skin cache for module: " + getName());
        fileCache = new ConcurrentHashMap<String, ScriptFile>();
    }

    public void flushTypeCache() {
        log.info("Flushing type cache for module: " + getName());
        synchronized (typeLock) {
            // remove type cache files if any
            new DefaultTypeLoader(this, typeReg, configuration.directory).flushCache();
            typeReg = null; // type registry will be recreated on first access
        }
    }

    /**
     * @deprecated resources are deprecated - you should use a jax-rs application to declare more resources.
     */
    @Deprecated
    public void flushRootResourcesCache() {
        if (configuration.resources != null) { // reregister resources
            for (ResourceBinding rb : configuration.resources) {
                try {
                    engine.removeResourceBinding(rb);
                    rb.reload(engine);
                    engine.addResourceBinding(rb);
                } catch (ClassNotFoundException e) {
                    log.error("Failed to reload resource", e);
                }
            }
        }
    }

    @Override
    public void flushCache() {
        reloadMessages();
        flushSkinCache();
        flushTypeCache();
    }

    public static File getSkinDir(File moduleDir) {
        return new File(moduleDir, "skin");
    }

    protected void loadDirectoryStack() {
        dirStack = new DirectoryStack();
        try {
            File skin = getSkinDir(configuration.directory);
            if (!configuration.allowHostOverride) {
                if (skin.isDirectory()) {
                    dirStack.addDirectory(skin);
                }
            }
            for (File fragmentDir : configuration.fragmentDirectories) {
                File fragmentSkin = getSkinDir(fragmentDir);
                if (fragmentSkin.isDirectory()) {
                    dirStack.addDirectory(fragmentSkin);
                }
            }
            if (configuration.allowHostOverride) {
                if (skin.isDirectory()) {
                    dirStack.addDirectory(skin);
                }
            }
            if (superModule != null) {
                DirectoryStack ds = superModule.dirStack;
                if (ds != null) {
                    dirStack.getDirectories().addAll(ds.getDirectories());
                }
            }
        } catch (IOException e) {
            throw WebException.wrap("Failed to load directories stack", e);
        }
    }

    @Override
    public ScriptFile getFile(String path) {
        int len = path.length();
        if (len == 0) {
            return null;
        }
        char c = path.charAt(0);
        if (c == '.') { // avoid getting files outside the web root
            path = new Path(path).makeAbsolute().toString();
        } else if (c != '/') {// avoid doing duplicate entries in document stack
                              // cache
            path = new StringBuilder(len + 1).append("/").append(path).toString();
        }
        try {
            return findFile(new Path(path).makeAbsolute().toString());
        } catch (IOException e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * @param path a normalized path (absolute path)
     */
    protected ScriptFile findFile(String path) throws IOException {
        ScriptFile file = fileCache.get(path);
        if (file == null) {
            File f = dirStack.getFile(path);
            if (f != null) {
                file = new ScriptFile(f);
                fileCache.put(path, file);
            }
        }
        return file;
    }

    @Override
    public ScriptFile getSkinResource(String path) throws IOException {
        File file = dirStack.getFile(path);
        if (file != null) {
            return new ScriptFile(file);
        }
        return null;
    }

    /**
     * TODO There are no more reasons to lazy load the type registry since module are lazy loaded. Type registry must be
     * loaded at module creation
     */
    public TypeRegistry createTypeRegistry() {
        // double s = System.currentTimeMillis();
        TypeRegistry typeReg = null;
        // install types from super modules
        if (superModule != null) { // TODO add type reg listener on super
                                   // modules to update types when needed?
            typeReg = new TypeRegistry(superModule.getTypeRegistry(), engine, this);
        } else {
            typeReg = new TypeRegistry(new TypeRegistry(engine, null), engine, this);
        }
        if (configuration.directory.isDirectory()) {
            DefaultTypeLoader loader = new DefaultTypeLoader(this, typeReg, configuration.directory);
            loader.load();
        }
        // System.out.println(">>>>>>>>>>>>>"+((System.currentTimeMillis()-s)/1000));
        return typeReg;
    }

    @Override
    public File getRoot() {
        return configuration.directory;
    }

    public void reloadMessages() {
        messages = new Messages(superModule != null ? superModule.getMessages() : null, this);
    }

    @Override
    public Messages getMessages() {
        return messages;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getMessages(String language) {
        log.info("Loading i18n files for module " + configuration.name);
        File file = new File(configuration.directory,
                new StringBuilder().append("/i18n/messages_").append(language).append(".properties").toString());
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            Properties p = new Properties();
            p.load(in);
            return new HashMap(p); // HashMap is faster than Properties
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ee) {
                    log.error(ee);
                }
            }
        }
    }

    @Override
    public String toString() {
        return getName();
    }

}
