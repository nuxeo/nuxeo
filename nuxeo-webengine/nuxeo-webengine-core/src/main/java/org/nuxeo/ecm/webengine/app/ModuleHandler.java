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

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.app.extensions.ResourceContribution;
import org.nuxeo.ecm.webengine.app.impl.DefaultModule;
import org.nuxeo.ecm.webengine.model.Module;
import org.osgi.framework.Bundle;

/**
 * Manage a WebEngine module. 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModuleHandler {
    
    protected WebEngine engine;
    
    /**
     * The bundled application that deployed the module 
     */
    protected BundledApplication app;
    
    /**
     * The module instance. The instance is lazy loaded at first access. 
     */
    protected volatile Module module;
    
    /**
     * Lock to build module instance
     */
    private final Object lock = new Object();
    
    
    public ModuleHandler(WebEngine engine, BundledApplication app) {
        this.engine = engine;
        this.app = app;
    }
    
    public WebEngine getEngine() {
        return engine;
    }
    
    public BundledApplication getBundledApplication() {
        return app;
    }
    
    public Bundle getBundle() {
        return app.getBundle();
    }
    
    public Module getModule() {
        Module _module = module;
        if (_module == null) {
            synchronized (lock) {
                if (module != null) {
                    _module = module;
                } else {
                    module = buildModule();
                    _module = module;
                }
            }
        }
        return _module;
    }
    
    /**
     * Reset the module instance - this will force module rebuild on next call to {@link #getModule()}.
     */
    public void reset() {
        synchronized (lock) {            
            module = null;
        }
    }
    
    /**
     * The module name to be displayed in UI
     * @return
     */
    public String getName() {
        return ((WebEngineModule)app.getApplication()).getName();
    }
    
    public Class<?>[] getRootClasses() {
        return ((WebEngineModule)app.getApplication()).getRootClasses();
    }


    public Class<? extends ResourceContribution>[] getContributions() {
        return ((WebEngineModule)app.getApplication()).getContributions();
    }
        
    public Class<? extends WebEngineModule> getBaseModule() {
        return ((WebEngineModule)app.getApplication()).getBaseModule();
    }

    protected Module buildModule() {
        return new DefaultModule(this);
    }
    
}
