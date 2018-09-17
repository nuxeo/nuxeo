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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.ecm.webengine.model.LinkDescriptor;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;

import com.sun.jersey.server.impl.inject.ServerInjectableProviderContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("module")
public class ModuleConfiguration {

    /**
     * A web module may have multiple roots
     *
     * @deprecated you should use new module definition - through {@link WebEngineModule}
     */
    @Deprecated
    @XNode("@path")
    public String path;

    /**
     * @deprecated you should use new module definition - through {@link WebEngineModule}
     */
    @Deprecated
    @XNode("@root-type")
    public String rootType;

    /**
     * Paths of root resources in the module. This is replacing the deprecated root-type.
     */
    public Class<?>[] roots;

    @XNode("@extends")
    public String base;

    @XNode("@name")
    public String name;

    /**
     * Use module links instead. If a module doesn't declare a module item it will be headless by default. Still used
     * for compatibility mode - for those modules not yet using moduleItems.
     */
    @Deprecated
    @XNode("@headless")
    public boolean isHeadless;

    /**
     * A list of entry points into the module - to be shown in the main webengine page. This is optional and may be
     * ignored if your don't want to provide shortcuts to your module entry points.
     */
    @XNodeList(value = "shortcuts/shortcut", type = ArrayList.class, componentType = ModuleShortcut.class, nullByDefault = true)
    public List<ModuleShortcut> moduleShortcuts;

    /**
     * Web Types explicitly declared. If null no web types were explicitly declared and old type loading method from the
     * generated web-types file should be used.
     */
    public Class<?>[] types;

    /**
     * The module directory. Must be set by the client before registering the descriptor.
     */
    @XNode("home")
    public File directory;

    @XNodeList(value = "fragments/directory", type = ArrayList.class, componentType = File.class, nullByDefault = false)
    public List<File> fragmentDirectories = new ArrayList<File>();

    /**
     * The module configuration file (this will be set by the module config parser)
     */
    public File file;

    @XNodeList(value = "nature", type = HashSet.class, componentType = String.class, nullByDefault = true)
    public Set<String> natures;

    @XNodeList(value = "links/link", type = ArrayList.class, componentType = LinkDescriptor.class, nullByDefault = true)
    public List<LinkDescriptor> links;

    /**
     * @deprecated resources are deprecated - you should use a jax-rs application to declare more resources.
     */
    @Deprecated
    @XNodeList(value = "resources/resource", type = ArrayList.class, componentType = ResourceBinding.class, nullByDefault = true)
    public List<ResourceBinding> resources;

    @XNode("templateFileExt")
    public String templateFileExt = "ftl";

    @XNodeList(value = "media-types/media-type", type = MediaTypeRef[].class, componentType = MediaTypeRef.class, nullByDefault = true)
    public MediaTypeRef[] mediatTypeRefs;

    public WebEngine engine;

    // volatile for double-checked locking
    private volatile ModuleImpl module;

    public boolean allowHostOverride;

    public ModuleConfiguration() {
    }

    public ModuleConfiguration(WebEngine engine) {
        this.engine = engine;
    }

    public WebEngine getEngine() {
        return engine;
    }

    public void setEngine(WebEngine engine) {
        this.engine = engine;
    }

    public String getName() {
        return name;
    }

    public List<ModuleShortcut> getShortcuts() {
        return moduleShortcuts;
    }

    public List<LinkDescriptor> getLinks() {
        return links;
    }

    public File getDirectory() {
        return directory;
    }

    public String getBase() {
        return base;
    }

    public Module get(WebContext context) {
        ModuleImpl mod = module;
        if (mod == null) {
            synchronized (this) {
                mod = module;
                if (mod == null) {
                    Module superModule = null;
                    if (base != null) { // make sure super modules are resolved
                        ModuleConfiguration superM = engine.getModuleManager().getModule(base);
                        if (superM == null) {
                            throw new WebResourceNotFoundException("The module '" + name
                                    + "' cannot be loaded since its super module '" + base + "' cannot be found");
                        }
                        // force super module loading
                        superModule = superM.get(context);
                    }
                    ServerInjectableProviderContext sic = context.getServerInjectableProviderContext();
                    mod = new ModuleImpl(engine, (ModuleImpl) superModule, this, sic);
                    if (sic != null) {
                        // cache the module only if it has a ServerInjectableProviderContext
                        module = mod;
                    }
                }
            }
        }
        return mod;
    }

    public void flushCache() {
        synchronized (this) {
            if (module == null) {
                return;
            }
            module.flushCache();
            module = null;
        }
    }

    public boolean isLoaded() {
        return module != null;
    }

    public boolean isHeadless() {
        return isHeadless;
    }

}
