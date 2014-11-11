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
package org.nuxeo.ecm.webengine;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineActivator implements BundleActivator, FrameworkListener {

    private static final Log log = LogFactory.getLog(WebEngineActivator.class);

    protected final Set<String> deployedBundles = new HashSet<String>();

    protected BundleContext context;

    @Override
    public void start(final BundleContext context) throws Exception {
        this.context = context;
        // hack to be sure runtime is deployed
        context.addFrameworkListener(this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context.removeFrameworkListener(this);
        context = null;
    }

    protected void deployModules() throws Exception {
        final WebEngine engine = Framework.getLocalService(WebEngine.class);

        // start deploying web bundles
        context.addBundleListener(new SynchronousBundleListener() {
            public void bundleChanged(BundleEvent event) {
                try {
                    switch (event.getType()) {
                    case BundleEvent.STARTED:
                        synchronized (deployedBundles) {
                            deployModules(engine, event.getBundle());
                        }
                    }
                } catch (Throwable e) {
                    log.error("Failed to deploy web modules in bundle: "
                            + event.getBundle().getSymbolicName(), e);
                }
            }
        });
        // synchronize next block with the listener since they may run in
        // parallel
        synchronized (deployedBundles) {
            // deploy bundles already installed
            for (Bundle b : context.getBundles()) {
                if (b.getState() == Bundle.ACTIVE) {
                    try {
                        deployModules(engine, b);
                    } catch (Throwable t) {
                        log.error("Failed to deploy web modules in bundle: "
                                + b.getSymbolicName(), t);
                    }
                }
            }
        }
    }

    protected void deployModules(WebEngine engine, Bundle b) throws Exception {
        String id = b.getSymbolicName();
        if (deployedBundles.contains(id)) {
            return; // already deployed
        }
        if (engine.getApplicationManager().deployApplication(b)) {
            // bundle contains a web module
            deployedBundles.add(id);
            return;
        }
        // the following is deprecated and should be removed when old webengine
        // deployment is removed
        URL url = b.getEntry("module.xml");
        if (url == null) {// not a webengine module
            return;
        }
        File bf = Framework.getRuntime().getBundleFile(b);
        if (bf == null) {
            log.warn("Bundle type not supported - cannot be resolved to a file. Bundle: "
                    + b.getSymbolicName());
            return;
        }
        deployedBundles.add(id);
        deployModule(engine, id, bf, url);
    }

    protected void deployModule(WebEngine engine, String bundleId,
            File bundleFile, URL moduleConfig) throws IOException {

        if (checkHasNuxeoService(bundleId)) {
            throw new WebException(
                    "This webengine module should not define a Nuxeo Service, please split up.");
        }

        if (bundleFile.isDirectory()) { // exploded jar - deploy it as is.
            File cfg = new File(bundleFile, "module.xml");
            engine.registerModule(cfg);
        } else { // should be a JAR - we copy the bundle module content
            File moduleRoot = new File(engine.getRootDirectory(), "modules/"
                    + bundleId);
            File cfg = new File(moduleRoot, "module.xml");
            if (moduleRoot.exists()) {
                if (bundleFile.lastModified() < moduleRoot.lastModified()) {
                    // already deployed and JAR was not modified since. ingore
                    // module
                    engine.registerModule(cfg);
                    return;
                }
                // remove existing files
                moduleRoot.delete();
            }
            // create the module root
            moduleRoot.mkdirs();
            ZipUtils.unzip(bundleFile, moduleRoot);
            engine.registerModule(cfg);
        }
        log.info("Deployed web module found in bundle: " + bundleId);
    }

    protected boolean checkHasNuxeoService(String bundleId) {
        ComponentManager cpManager = Framework.getRuntime().getComponentManager();
        RegistrationInfo regInfo = cpManager.getRegistrationInfo(new ComponentName(
                bundleId));
        if (null == regInfo) {
            return false;
        }

        String[] serviceNames = regInfo.getProvidedServiceNames();

        return !(serviceNames == null || serviceNames.length == 0);
    }

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if (FrameworkEvent.STARTED == event.getType()) {
            try {
                deployModules();
            } catch (Exception e) {
                log.error("Failed to deploy WebEngine modules", e);
            }
        }
    }
}
