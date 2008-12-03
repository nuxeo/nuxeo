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
import java.util.List;
import java.util.MissingResourceException;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.AdapterNotFoundException;
import org.nuxeo.ecm.webengine.model.AdapterType;
import org.nuxeo.ecm.webengine.model.LinkDescriptor;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.ModuleType;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractModule implements Module {

    public static final Log log = LogFactory.getLog(AbstractModule.class);
    
    protected final WebEngine engine; 
    protected final Object typeLock = new Object();
    protected TypeRegistry typeReg;
    protected final ModuleDescriptor descriptor;
    protected final AbstractModule superModule; 
    protected ModuleTypeImpl type;
    protected LinkRegistry linkReg;
    protected final String skinPathPrefix;
    
    protected AbstractModule(WebEngine engine, ModuleDescriptor descriptor) {
        this.engine = engine;
        this.descriptor = descriptor;
        if (descriptor.base != null) {
            superModule = (AbstractModule) engine.getModule(descriptor.base);
        } else {
            superModule = null;
        }
        skinPathPrefix = new StringBuilder()
            .append(engine.getSkinPathPrefix()).append('/').append(descriptor.name).toString();
        loadLinks();
    }
    
    public WebEngine getEngine() {
        return engine;
    }
    
    public String getName() {
        return descriptor.name;
    }
    
    public AbstractModule getSuperModule() {
        return superModule;
    }

    
    public ModuleDescriptor getDescriptor() {
        return descriptor;
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

    public String getSkinPathPrefix() {
        return skinPathPrefix;
    }

    public boolean isFragment() {
        return descriptor.fragment != null;
    }

    public TypeRegistry getTypeRegistry() {
        if (typeReg == null) { // create type registry if not already created
            synchronized (typeLock) {
                if (typeReg == null) {
                    typeReg = createTypeRegistry();
                    type = (ModuleTypeImpl)typeReg.getModuleType();
                }
            }
        }
        return typeReg;
    }
    
    protected abstract TypeRegistry createTypeRegistry();
    
    public ModuleType getModuleType() {
        if (type == null) {
            getTypeRegistry();
        }
        return type;
    }
    
    public void flushTypeCache() {
        log.info("Flushing type cache for module: "+getName());
        synchronized (typeLock) {
            typeReg = null; // type registry will be recreated on first access
        }
    }
    
    public void flushCache() {
        flushTypeCache();
        engine.getScripting().flushCache();
    }

    public String getTemplateFileExt() {
        return descriptor.templateFileExt;
    }

    public String getModuleTitle() {
        String title = null;
        try {
            title = getMessages().getString("module."+descriptor.name);
            if (title == null) {
                title = descriptor.name;
            }
        } catch (MissingResourceException e) {
            title = descriptor.name;
        }
        return title;
    }

    public File getModuleIcon() {
        String icon = null;
        try {
            icon = getMessages().getString("module.icon");
            if (icon == null) {
                return null;
            }
        } catch (MissingResourceException e) {
            return null;
        }
        File f = new File(descriptor.directory, icon);
        return f.isFile() ? f : null;
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return engine.getScripting().loadClass(className);
    }

    public ResourceType[] getTypes() {
        return getTypeRegistry().getTypes();
    }

    public AdapterType[] getAdapters() {
        return getTypeRegistry().getAdapters();
    }

    public AdapterType getAdapter(Resource ctx, String name) {
        AdapterType type = getTypeRegistry().getAdapter(ctx, name);
        if (type == null) {
            throw new AdapterNotFoundException(ctx, name);
        }
        return type;
    }

    public List<String> getAdapterNames(Resource ctx) {
        return getTypeRegistry().getAdapterNames(ctx);
    }

    public List<AdapterType> getAdapters(Resource ctx) {
        return getTypeRegistry().getAdapters(ctx);
    }

    public List<String> getEnabledAdapterNames(Resource ctx) {
        return getTypeRegistry().getEnabledAdapterNames(ctx);
    }

    public List<AdapterType> getEnabledAdapters(Resource ctx) {
        return getTypeRegistry().getEnabledAdapters(ctx);
    }

    public String getMediaTypeId(MediaType mt) {
        if (descriptor.mediatTypeRefs == null) {
            return null;
        }
        MediaTypeRef[] refs = descriptor.mediatTypeRefs;
        for (MediaTypeRef ref : refs) {
            String id = ref.match(mt);
            if (id != null) {
                return id;
            }
        }
        return null;
    }
    
    public ResourceBinding getModuleBinding() {
        return descriptor.binding;
    }

    public List<ResourceBinding> getResourceBindings() {
        return descriptor.resources;
    }

    public boolean isDerivedFrom(String moduleName) {
        if (descriptor.name.equals(moduleName)) {
            return true;
        }
        if (superModule != null) {
            return superModule.isDerivedFrom(moduleName);
        }
        return false;
    }

    
    @Override
    public String toString() {
        return getName();
    }
}
