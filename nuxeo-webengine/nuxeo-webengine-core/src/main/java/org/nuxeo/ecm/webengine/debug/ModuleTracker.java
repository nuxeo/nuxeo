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
 * This listener is tacking global i18n file changes and web shared classes:
 * <ul>
 * <li> WEB-INF/i18n
 * <li> WEB-INF/classes
 * </ul>
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModuleTracker implements Runnable {

    private final static Log log = LogFactory.getLog(ModuleTracker.class);
    
    protected ModuleImpl module;
    
    protected FileEntry moduleXml;
    protected FileEntry annotations;
    protected FileEntry i18n;
    protected DirectoryEntry skin;
    protected ModuleClassesEntry classes;
    
    public ModuleTracker(ModuleImpl module) {
        this.module = module;
        moduleXml = new FileEntry(module.getModuleConfiguration().file);
        File root = module.getRoot();
        classes = new ModuleClassesEntry(root);
        i18n = new FileEntry(new File(root, "i18n"));
        skin = new DirectoryEntry(new File(root, "skin"));
        annotations = new FileEntry(new File(root, "META-INF/annotations"));
    }
    
    
    public void run() {
        try {
            doRun();
        } catch (Exception e) {
            log.error("Failed to check module changes for "+module.getName(), e);
        }
    }
    
    protected void doRun() throws Exception {
        if (moduleXml.check()) { // module.xml changed - reload module
            module.getEngine().getModuleManager().reloadModule(module.getName());
            return; 
        }
        if (i18n.check()) { // i18n files changed - reload them
            module.reloadMessages();
        }
        if (annotations.check()) { // type registration changed - reload types 
            module.flushTypeCache();
        }
        if (classes.check()) { // classes changed - reload class loader
        	// reload class loaders
            module.getEngine().getWebLoader().flushCache();
            // remove registered types (which are using older version of classes)
            flushTypeCache(module);
            // re-register main entry point?
            //module.getEngine().registerRootBinding();
            // to speed up things we also invalidate skin cache and then return
            flushSkinCache(module);
            return;
        }
        if (skin.check()) { // skin changed - flush skin cache  
        	flushSkinCache(module);
        }
    }

    /**
     * Flush the type cache of a module.
     * This will flush all modules type cache since some modules may be dependent on this one.
     * TODO: optimize this
     * @param module
     */
    public static void flushTypeCache(ModuleImpl module) {
//        module.flushTypeCache();
        ModuleConfiguration[] modules = module.getEngine().getModuleManager().getModules();
        for (ModuleConfiguration mc : modules) {
        	if (mc.isLoaded()) {
        		((ModuleImpl)mc.get()).flushTypeCache();
        	}
        }
    }

    /**
     * Flush the skin cache for the given module.
     * Note that all modules skin cache will be flushed since they may depend on 
     * this module skins.
     * TODO: optimize this 
     * @param module
     */
    public static void flushSkinCache(ModuleImpl module) {
//      module.flushSkinCache();
      ModuleConfiguration[] modules = module.getEngine().getModuleManager().getModules();
      for (ModuleConfiguration mc : modules) {
      	if (mc.isLoaded()) {
      		((ModuleImpl)mc.get()).flushSkinCache();
      	}
      }
  }

}
