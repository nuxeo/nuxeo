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
package org.nuxeo.ecm.webengine.app.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.webengine.ApplicationManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.app.BundledApplication;
import org.nuxeo.ecm.webengine.app.ModuleHandler;
import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.ecm.webengine.app.extensions.ExtensibleResource;
import org.nuxeo.ecm.webengine.app.extensions.ResourceContribution;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultApplicationManager implements ApplicationManager {

    /** Pattern to read manifest value parameters */
    public static final Pattern PARAMS_PATTERN
        = Pattern.compile("\\s*([^:\\s]+)\\s*:=\\s*([^;\\s]+)\\s*;?");

    private static final Log log = LogFactory.getLog(DefaultApplicationManager.class);

    protected WebEngine engine;
    
    /**
     * Collected user applications keyed by application ID. Deployment order is preserved.
     * The application ID (i.e. bundle symbolic name is used as the key in the map)
     */
    protected LinkedHashMap<String, BundledApplication> apps;
    
    /** A reloadable module registry which is lazy built when first requested. */
    protected volatile ModuleRegistry registry;

    /** A lock used to synchronize mutable operations on the registry. */
    private final Object lock = new Object();
    
    
    public DefaultApplicationManager(WebEngine engine) {
        this.engine = engine;
        this.apps = new LinkedHashMap<String, BundledApplication>();
        this.registry = new ModuleRegistry(engine);
    }

    public void addApplication(Bundle bundle, Application app) {
        BundledApplication bapp = new BundledApplication(bundle, app);
        synchronized (lock) {
            apps.put(bapp.getId(), bapp);
            registry = null;
        }
    }
    
    public void removeApplication(Bundle bundle) {
        synchronized (lock) {
            BundledApplication app = apps.remove(bundle.getSymbolicName());
            if (app != null) {
                registry = null;
            }
        }
    }
    
    /**
     * Gets the module registry.
     */
    public ModuleRegistry getRegistry() {
        ModuleRegistry _registry = registry;
        if (_registry == null) {
            synchronized (lock) {
                if (registry != null) {
                    _registry = registry;
                } else {
                    _registry = new ModuleRegistry(engine);
                    for (BundledApplication app : apps.values()) {
                        _registry.addApplication(app);
                    }
                    registry = _registry;
                }
            }
        }
        return _registry;
    }

    public BundledApplication getApplication(String bundleId) {
        synchronized (lock) {
            return apps.get(bundleId);
        }
    }

    public BundledApplication[] getApplications() {
        synchronized (lock) {
            return apps.values().toArray(new BundledApplication[apps.size()]);
        }
    }
    
    public ModuleHandler getModuleHandler(String appId) {
        return getRegistry().getModuleHandler(appId);
    }

    public ModuleHandler[] getModuleHandlers() {
        return getRegistry().getModuleHandlers();
    }

    public Object getContribution(Resource target, String key) throws Exception {
        return getRegistry().getContribution(target, key);
    }
    
    public List<ResourceContribution> getContributions(ExtensibleResource target, String category) {
        return getRegistry().getContributions(target, category);
    }
    
    public List<ResourceContribution> getContributions(Class<? extends ExtensibleResource> target, String category) {
        return getRegistry().getContributions(target, category);
    }

    public ResourceContribution getContribution(Class<? extends ExtensibleResource> target, String key) throws Exception {
        return getRegistry().getContribution(target, key);
    }

    public ModuleHandler getModuleHandlerFor(Class<?> rootResource) {
        return getRegistry().getModuleHandlerFor(rootResource);
    }

    /**
     * Reload modules - this is useful for hot reload when application classes changes  
     */
    public void reload() {
        synchronized (lock) {
            ModuleRegistry _registry = new ModuleRegistry(engine);
            for (BundledApplication app : apps.values()) {
                try {
                    app.reload(engine);
                } catch (Exception e) {
                    log.error("Failed to reload web module: "+app.getId(), e);
                }
                _registry.addApplication(app);
            }
            registry = _registry;
        }
    }
    
    public boolean deployApplication(Bundle bundle) throws Exception {        
        String webAppEntry = (String)bundle.getHeaders().get("Nuxeo-WebModule");
        if (webAppEntry == null) {
            return false;
        }
        String bundleId = bundle.getSymbolicName();
        if (checkHasNuxeoService(bundleId)) {
            throw new WebException(
                    "This webengine module should not define a Nuxeo Service, please split up.");
        }
        StringBuilder result = new StringBuilder();
        Map<String,String> attrs = readManifestEntryValue(webAppEntry, result);
        String type = result.toString();
        boolean explode = false;
        boolean compat = false;
        if (attrs != null) {
            String v = attrs.get("explode");
            if ("true".equals(v)) {
                explode = true;
            } else if ("false".equals(v)) {
                explode = false;
            } else { // not specified
                // load the class to check if a WebEngine Module is present
                explode = isWebEngineModule(bundle, type);                  
            }
            v = attrs.get("compat");
            if ("true".equals(v)) {
                compat = true;
            }            
        }
        
        if (explode) { // this will also add the exploded directory to WebEngine class loader            
            File moduleDir = explodeBundle(bundle);
            if (compat) { // old style deploy
                File config = new File(moduleDir, "module.xml");
                if (config.isFile()) { // the module is already in the classpath because of explodeBundle()
                    engine.registerModule(config, false);
                }
            }
        }
        // register application        
        try {
            // load the class using WebEngine loader
            Class<?> appClass = engine.loadClass(type);
            Application app = (Application)appClass.newInstance();
            addApplication(bundle, app);
        } catch (ClassCastException e) {
            throw new Error("Invalid web module specified in MANIFEST for bundle "+bundleId+". Must be an instance of "+Application.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate web module "+type+" found in bundle "+bundleId, e);
        }
        log.info("Deployed web module found in bundle: " + bundleId);
        return true;
    }
    
    protected boolean isWebEngineModule(Bundle b, String type) throws Exception {
        return WebEngineModule.class.isAssignableFrom(b.loadClass(type));
    }
    
    protected File explodeBundle(Bundle bundle) throws IOException {
        File bundleFile = Framework.getRuntime().getBundleFile(bundle);
        if (bundleFile.isDirectory()) { // exploded jar - deploy it as is.
            engine.getWebLoader().addClassPathElement(bundleFile);
            return bundleFile;
        } else { // should be a JAR - we copy the bundle module content
            File moduleRoot = new File(engine.getRootDirectory(), "modules/"
                    + bundle.getSymbolicName());
            if (moduleRoot.exists()) {
                if (bundleFile.lastModified() < moduleRoot.lastModified()) {
                    // already deployed and JAR was not modified since.
                    engine.getWebLoader().addClassPathElement(moduleRoot);
                    return moduleRoot;
                }
                // remove existing files
                FileUtils.deleteTree(moduleRoot);
            }            
            // create the module root
            moduleRoot.mkdirs();
            ZipUtils.unzip(bundleFile, moduleRoot);
            engine.getWebLoader().addClassPathElement(moduleRoot);
            return moduleRoot;
        }        
    }

    protected static Map<String,String> readManifestEntryValue(String value, StringBuilder result) {
        int p = value.indexOf(';');
        if (p > 0) {
            result.append(value.substring(0, p).trim());
            value = value.substring(p + 1);
            HashMap<String,String> params = new HashMap<String, String>();
            Matcher m = PARAMS_PATTERN.matcher(value);
            while (m.find()) {
                params.put(m.group(1), m.group(2));
            }
            return params;
        } else {
            result.append(value.trim());
            return null;
        }
    }

    protected boolean checkHasNuxeoService(String bundleId) {

        ComponentManager cpManager = Framework.getRuntime()
                .getComponentManager();
        RegistrationInfo regInfo = cpManager
                .getRegistrationInfo(new ComponentName(bundleId));
        if (null == regInfo) {
            return false;
        }

        String[] serviceNames = regInfo.getProvidedServiceNames();

        return !(serviceNames == null || serviceNames.length == 0);
    }
        
}
