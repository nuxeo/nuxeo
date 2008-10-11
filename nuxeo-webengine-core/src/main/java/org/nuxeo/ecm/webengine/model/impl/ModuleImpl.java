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

package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.LinkDescriptor;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.ModuleType;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.ServiceNotFoundException;
import org.nuxeo.ecm.webengine.model.ServiceType;
import org.nuxeo.ecm.webengine.model.TypeNotFoundException;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * The default implementation for a web configuration
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModuleImpl implements Module {

    protected WebEngine engine;
    protected ModuleDescriptor descriptor;
    
    protected File root;
    protected DirectoryStack dirStack; 
    // cache used for resolved files
    protected ConcurrentMap<String, ScriptFile> fileCache;
    
    protected final Object typeLock = new Object();

    // these two members are lazy initialized when needed
    protected TypeRegistry typeReg;
    protected TypeConfigurationProvider localTypes;

    protected LinkRegistry linkReg; 
    
    protected ModuleTypeImpl type;
    protected ModuleImpl superModule;
    
    public ModuleImpl(WebEngine engine, File root, ModuleDescriptor descriptor) throws WebException {
        this.fileCache = new ConcurrentHashMap<String, ScriptFile>();
        this.root = root;
        this.descriptor = descriptor;
        this.engine = engine;
        if (descriptor.base != null) {
            superModule = (ModuleImpl)engine.getModule(descriptor.base);
        }
        loadDirectoryStack();
        loadLinks();
    }


    public boolean isFragment() {
        return descriptor.fragment != null;
    }


    public ModuleDescriptor getDescriptor() {
        return descriptor;
    }

    public String getName() {
        return descriptor.name;
    }

    public WebEngine getEngine() {
        return engine;
    }

    public void flushSkinCache() {
        fileCache.clear();
    }
   
    public void flushCache() {
        fileCache.clear();
        synchronized (typeLock) {
            typeReg = null; // type registry will be recreated on first access
            localTypes = null;
        }
        engine.getScripting().flushCache();
    }

    public ModuleType getModuleType() {
        if (type == null) {
            getTypeRegistry();
        }
        return type;
    }
    
    public ResourceBinding getModuleBinding() {
        return descriptor.binding;
    }
    
    public static File getSkinDir(File moduleDir) {
        return new File(moduleDir, "skin");
    }
    
    protected void loadDirectoryStack() throws WebException {
        dirStack = new DirectoryStack();
        try {
            File skin = getSkinDir(root);
            if (skin.isDirectory()) {
                dirStack.addDirectory(skin);
            }
            if (superModule != null && superModule.dirStack != null) {
                dirStack.getDirectories().addAll(superModule.dirStack.getDirectories());
            }
        } catch (IOException e) {
            WebException.wrap("Failed to load directories stack", e);
        }
    }

    public ModuleImpl getSuperModule() {
        return superModule;               
    }

    public ScriptFile getFile(String path) throws WebException {
        int len = path.length();
        if (len == 0) {
            return null;
        }
        char c = path.charAt(0);
        if (c == '.') { // avoid getting files outside the web root
            path = new org.nuxeo.common.utils.Path(path).makeAbsolute().toString();
        } else if (c != '/') {// avoid doing duplicate entries in document stack cache
            path = new StringBuilder(len+1).append("/").append(path).toString();
        } 
        try {
            return findFile(new org.nuxeo.common.utils.Path(path).makeAbsolute().toString());
        } catch (IOException e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * @param path a normalized path (absolute path)
     * @return
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
    
    protected void loadConfiguredTypes() {
        localTypes = new TypeConfigurationProvider();
        // load declared types and actions
        localTypes.types = new ArrayList<TypeDescriptor>();
        if (descriptor.types != null) {
            localTypes.types.addAll(descriptor.types);
        }
        localTypes.services = new ArrayList<ServiceDescriptor>();
        if (descriptor.actions != null) {
            descriptor.actions.addAll(descriptor.actions);
        }
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return engine.getScripting().loadClass(className);
    }

    public TypeRegistry getTypeRegistry() {
        if (typeReg == null) { // create type registry if not already created
            synchronized (typeLock) {
//                double s = System.currentTimeMillis();
                if (typeReg == null) {
                    typeReg = new TypeRegistry(this);
                    // install global types
                    BundleTypeProvider bundleTypeProvider = engine.getBundleTypeProvider();
                    bundleTypeProvider.install(typeReg);
                    DirectoryTypeProvider directoryTypeProvider = engine.getDirectoryTypeProvider();
                    directoryTypeProvider.load();
                    directoryTypeProvider.install(typeReg);
                    // install local configured types (in XML configuration) - TODO remove this?
                    loadConfiguredTypes();
                    localTypes.install(typeReg);
                    type = (ModuleTypeImpl)typeReg.getModuleType();
                }
//                System.out.println(">>>>>>>>>>>>>"+((System.currentTimeMillis()-s)/1000));
            }
        }
        return typeReg;
    }
    
    public ResourceType getType(String typeName) throws TypeNotFoundException {
        ResourceType type = getTypeRegistry().getType(typeName);
        if (type == null) {
            throw new TypeNotFoundException(typeName);
        }
        return type;
    }

    public ResourceType[] getTypes() {
        return getTypeRegistry().getTypes();
    }

    public ServiceType[] getServices() {
        return getTypeRegistry().getServices();
    }

    public ServiceType getService(Resource ctx, String name)
            throws ServiceNotFoundException {
        ServiceType type = getTypeRegistry().getService(ctx, name);
        if (type == null) {
            throw new ServiceNotFoundException(ctx, name);
        }
        return type;
    }
    
    public List<String> getServiceNames(Resource ctx) {
        return getTypeRegistry().getServiceNames(ctx);
    }
    
    public List<ServiceType> getServices(Resource ctx) {
        return getTypeRegistry().getServices(ctx);
    }
    
    public List<String> getEnabledServiceNames(Resource ctx) {
        return getTypeRegistry().getEnabledServiceNames(ctx);
    }
    
    public List<ServiceType> getEnabledServices(Resource ctx) {
        return getTypeRegistry().getEnabledServices(ctx);
    }

    protected void loadLinks() {
        linkReg = new LinkRegistry();
        if (descriptor.links != null) {
            for (LinkDescriptor link : descriptor.links) {
                linkReg.registerLink(link);
            }
        }
        descriptor.links = null; // avoid storing unused data
    }
    
    public List<LinkDescriptor> getLinks(String category) {
        return linkReg.getLinks(category);
    }
    
    public List<LinkDescriptor> getActiveLinks(Resource context, String category) {
        return linkReg.getActiveLinks(context, category);
    }
    
    public LinkRegistry getLinkRegistry() {
        return linkReg;
    }

}
