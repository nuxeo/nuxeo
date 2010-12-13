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
package org.nuxeo.ecm.webengine.debug;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.model.impl.ModuleConfiguration;
import org.nuxeo.ecm.webengine.model.impl.ModuleImpl;

/**
 * The default module tracker is tracking any file in a web module for changes.
 * If a change is detected it is invalidating WebEngine. (see setDirty). In dev.
 * mode the servlet is checking at each request if WebEngine is dirty and reload
 * all JAX-RS applications.
 *
 * You should override the {@link #doRun()} method of this class and implement a
 * fine grained check if you want to check only some modifications. Also you
 * need to update {@link ModuleImpl#getTracker()} to instantiate your tracker
 * (you can use the value of nuxeo.dev.mode to test if your tracker is needed)
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModuleTracker implements Runnable {

    private static final Log log = LogFactory.getLog(ModuleTracker.class);

    protected final ModuleImpl module;

    protected final DirectoryEntry rootEntry;

    public ModuleTracker(ModuleImpl module) {
        this.module = module;
        File root = module.getRoot();
        rootEntry = new DirectoryEntry(root);
    }

    public void run() {
        try {
            doRun();
        } catch (Exception e) {
            log.error("Failed to check module changes for " + module.getName(),
                    e);
        }
    }

    protected void doRun() throws Exception {
        if (rootEntry.check()) {
            module.getEngine().setDirty(true);
        }
    }

    /**
     * Flush the type cache of a module. This will flush all modules type cache
     * since some modules may be dependent on this one. TODO: optimize this
     *
     * @param module
     */
    public static void flushTypeCache(ModuleImpl module) {
        // module.flushTypeCache();
        ModuleConfiguration[] modules = module.getEngine().getModuleManager().getModules();
        for (ModuleConfiguration mc : modules) {
            if (mc.isLoaded()) {
                ((ModuleImpl) mc.get()).flushTypeCache();
            }
        }
    }

    /**
     * Flush the skin cache for the given module. Note that all modules skin
     * cache will be flushed since they may depend on this module skins. TODO:
     * optimize this
     *
     * @param module
     */
    public static void flushSkinCache(ModuleImpl module) {
        // module.flushSkinCache();
        ModuleConfiguration[] modules = module.getEngine().getModuleManager().getModules();
        for (ModuleConfiguration mc : modules) {
            if (mc.isLoaded()) {
                ((ModuleImpl) mc.get()).flushSkinCache();
            }
        }
    }

}
