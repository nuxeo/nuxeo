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
 */
package org.nuxeo.ecm.webengine.model.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;

/**
 * A module descriptor is a proxy to a module and provide information
 * about that module so that the module can be referenced before being loaded.
 * Modules are lazy loaded.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModuleDescriptor {

    protected WebEngine engine;
    protected String name;
    protected File configFile;    

    private ModuleConfiguration config;
    private Module module;
    
    protected ModuleDescriptor() {
        
    }
    
    public ModuleDescriptor(WebEngine engine, String name, File configFile) {
        this.engine = engine;
        this.name = name;
        this.configFile = configFile;
    }
    
    public WebEngine getEngine() {
        return engine;
    }
    
    public String getName() {
        return name;
    }
    
    public File getConfigurationFile() {
        return configFile;
    }
    
    public ModuleConfiguration getConfiguration() {
        if (config == null) {
            synchronized (this) {
                if (config == null) {
                    config = loadConfiguration();
                }
            }
        }
        return config;
    }
    
    public String getTitle() {
        String title = getConfiguration().getTitle();
        return title == null ? name : title;
    }
    
    public String getIcon() {
        return getConfiguration().getIcon();
    }

    public String getPath() {
        return getConfiguration().path;
    }
    
    public Module get() {
        if (module == null) {
            try {
                getConfiguration(); // make sure config is loaded
                Module superModule = null;
                if (config.base != null) { // make sure super modules are resolved
                    ModuleDescriptor superM = engine.getModuleManager().getModule(config.base);
                    if (superM == null) {
                        throw new WebResourceNotFoundException("The module '"
                                +name+"' cannot be loaded since it's super module '"+config.base+"' cannot be found");
                    }
                    // force super module loading
                    superModule = superM.get();
                }
                module = new ModuleImpl(engine, name, (ModuleImpl)superModule, config);
            } catch (Exception e) {
                throw WebException.wrap(e);
            }
        }
        return module;
    }
    
    public boolean isLoaded() {
        return module != null;
    }
    
    public void unload() {
        module = null;
        config = null;
    }
    
    
    protected ModuleConfiguration loadConfiguration() {
        try {
            XMap xmap = new XMap();
            xmap.register(ModuleConfiguration.class);
            InputStream in = new BufferedInputStream(new FileInputStream(configFile));
            ModuleConfiguration mc = (ModuleConfiguration) xmap.load(createXMapContext(), in);
            if (mc.resources != null) {
                for (ResourceBinding rb : mc.resources) {
                    engine.addResourceBinding(rb);
                }
            }
            if (mc.directory == null) {
                mc.directory = configFile.getParentFile().getCanonicalFile();
            }
            return mc;
        } catch (Exception e) {
            throw WebException.wrap("Faile to load module configuration: "+configFile, e);
        }
    }

    protected Context createXMapContext() {
        return new Context() {
            private static final long serialVersionUID = 1L;
            @Override
            public Class<?> loadClass(String className)
            throws ClassNotFoundException {
                return engine.loadClass(className);
            }
            @Override
            public URL getResource(String name) {
                return engine.getScripting().getGroovyScripting().getGroovyClassLoader().getResource(name);
            }
        };
    }
    
}
