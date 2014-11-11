/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webengine.app;

import java.io.File;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.impl.ModuleConfiguration;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;

/**
 * A WebEngine module definition. It is replacing old style type declarations
 * (though apt plugin at build time). Also it is making the module.xml file
 * optional
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class WebApplication extends Application {

    protected ModuleConfiguration cfg;

    /**
     * Create a web module which doesn't extend another module
     *
     * @param name the module name
     */
    protected WebApplication(String name) {
        this(name, null);
    }

    /**
     * Create a web module which extends a base module
     *
     * @param name the module name
     * @param baseModule the baseModule name
     */
    protected WebApplication(String name, String baseModule) {
        cfg = new ModuleConfiguration();
        cfg.base = baseModule;
        cfg.name = name;
        cfg.types = getWebTypes();
    }

    public void setModuleDirectory(File moduleDir) {
        cfg.file = new File(moduleDir, "module.xml");
        cfg.directory = moduleDir;
    }

    public ModuleConfiguration resolve() throws Exception {
        if (cfg.file.isFile()) {
            // merge definition from file with cfg.
            ModuleConfiguration mc = ModuleManager.readConfiguration(
                    cfg.engine, cfg.file);
            mc.base = cfg.base;
            mc.name = cfg.name;
            mc.types = cfg.types;
            mc.file = cfg.file;
            mc.directory = cfg.directory;
            cfg = mc;
        }
        return cfg;
    }

    public ModuleConfiguration getConfiguration() {
        return cfg;
    }

    public Module getModule() {
        return cfg.get();
    }

    public abstract Class<?>[] getWebTypes();

}
