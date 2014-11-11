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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.app.WebApplication;
import org.nuxeo.ecm.webengine.model.LinkDescriptor;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.Validator;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("module")
public class ModuleConfiguration implements Cloneable {

    private static final Log log = LogFactory.getLog(ModuleConfiguration.class);

    /**
     * A web module may have multiple roots
     *
     * @deprecated you should use new module definition - through
     *             {@link WebApplication}
     */
    @Deprecated
    @XNode("@path")
    public String path;

    /**
     * @deprecated you should use new module definition - through
     *             {@link WebApplication}
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
     * Use module links instead. If a module doesn't declare a module item it
     * will be headless by default. Still used for compatibility mode - for
     * those modules not yet using moduleItems.
     */
    @Deprecated
    @XNode("@headless")
    public boolean isHeadless;

    /**
     * A list of entry points into the module - to be shown in the main
     * webengine page. This is optional and may be ignored if your don't want to
     * provide shortcuts to your module entry points.
     */
    @XNodeList(value = "shortcuts/shortcut", type = ArrayList.class, componentType = ModuleShortcut.class, nullByDefault = true)
    public List<ModuleShortcut> moduleShortcuts;

    /**
     * Web Types explicitly declared. If null no web types were explicitly
     * declared and old type loading method from the generated web-types file
     * should be used.
     */
    public Class<?>[] types;

    /**
     * The module directory. Must be set by the client before registering the
     * descriptor.
     */
    @XNode("home")
    public File directory;

    /**
     * The module configuration file (this will be set by the module config
     * parser)
     */
    public File file;

    @XNodeList(value = "nature", type = HashSet.class, componentType = String.class, nullByDefault = true)
    public Set<String> natures;

    @XNodeList(value = "links/link", type = ArrayList.class, componentType = LinkDescriptor.class, nullByDefault = true)
    public List<LinkDescriptor> links;

    @XNodeMap(value = "validators/validator", key = "@type", type = HashedMap.class, componentType = Class.class, nullByDefault = true)
    public void setValidators(Map<String, Class<Validator>> m) {
        if (m != null) {
            validators = new HashMap<String, Validator>();
            for (Map.Entry<String, Class<Validator>> entry : m.entrySet()) {
                try {
                    validators.put(entry.getKey(),
                            entry.getValue().newInstance());
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }

    public Map<String, Validator> validators;

    /**
     * @deprecated resources are deprecated - you should use a jax-rs
     *             application to declare more resources.
     */
    @XNodeList(value = "resources/resource", type = ArrayList.class, componentType = ResourceBinding.class, nullByDefault = true)
    public List<ResourceBinding> resources;

    @XNode("templateFileExt")
    public String templateFileExt = "ftl";

    @XNodeList(value = "media-types/media-type", type = MediaTypeRef[].class, componentType = MediaTypeRef.class, nullByDefault = true)
    public MediaTypeRef[] mediatTypeRefs;

    public WebEngine engine;

    private ModuleImpl module;

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

    /**
     * @deprecated you should use new module definition - through
     *             {@link WebApplication}
     */
    @Deprecated
    public String getPath() {
        return path;
    }

    public Module get() {
        if (module == null) {
            try {
                Module superModule = null;
                if (base != null) { // make sure super modules are resolved
                    ModuleConfiguration superM = engine.getModuleManager().getModule(
                            base);
                    if (superM == null) {
                        throw new WebResourceNotFoundException(
                                "The module '"
                                        + name
                                        + "' cannot be loaded since it's super module '"
                                        + base + "' cannot be found");
                    }
                    // force super module loading
                    superModule = superM.get();
                }
                module = new ModuleImpl(engine, (ModuleImpl) superModule, this);
            } catch (Exception e) {
                throw WebException.wrap(e);
            }
        }
        return module;
    }

    public boolean isLoaded() {
        return module != null;
    }

    public boolean isHeadless() {
        return isHeadless;
    }

}
