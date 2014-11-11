/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.osgi;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * The default BundleActivator for NXRuntime over an OSGi comp. platform.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiRuntimeActivator implements BundleActivator {

    private static final Log log = LogFactory.getLog(OSGiRuntimeActivator.class);

    private static OSGiRuntimeActivator instance;

    protected OSGiRuntimeService runtime;
    protected OSGiComponentLoader componentLoader;

    protected ServiceReference pkgAdmin;

    protected BundleContext context;


    public static OSGiRuntimeActivator getInstance() {
        return instance;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("Starting Runtime Activator");
        instance = this;
        this.context = context;

        pkgAdmin = context.getServiceReference(PackageAdmin.class.getName());

        // assert the environment is setup
        if (Environment.getDefault() == null) {
            throw new IllegalStateException("Environment is not setup");
        }

        // create the runtime
        runtime = new OSGiRuntimeService(context);

        // load main config file if any
        URL config = context.getBundle().getResource("/OSGI-INF/nuxeo.properties");
        if (config != null) {
            System.setProperty(OSGiRuntimeService.PROP_CONFIG_DIR, config.toExternalForm());
        }

        initialize(runtime);
        // start it
        Framework.initialize(runtime);
        // register bundle component loader
        componentLoader = new OSGiComponentLoader(runtime);
        // TODO register osgi services
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("Stopping Runtime Activator");
        instance = null;
        pkgAdmin = null;
        // remove component loader
        componentLoader.uninstall();
        componentLoader = null;
        // unregister
        Framework.shutdown();
        uninitialize(runtime);
        runtime = null;
        context = null;
    }


    public Bundle getBundle(String name) {
        if (pkgAdmin == null) {
            return null;
        }
        PackageAdmin pa = (PackageAdmin)context.getService(pkgAdmin);
        Bundle[] bundles = pa.getBundles(name, null);
        context.ungetService(pkgAdmin);
        return bundles == null ? null : bundles[0];
    }

    /**
     * Load a class from another bundle given its reference as <code>bundleSymbolicName:className</code>
     * If no <code>bundleSymbolicName:</code> prefix is given then a classForName will be done
     * @param ref
     * @return
     */
    public Class<?> loadClass(String ref) throws Exception {
        int i = ref.indexOf(':');
        if (i == -1) {
            return Class.forName(ref);
        }
        return loadClass(ref.substring(0, i), ref.substring(i+1));
    }

    public Class<?> loadClass(String bundleName, String className) throws Exception {
        Bundle bundle = getBundle(bundleName);
        if (bundle == null) {
            throw new ClassNotFoundException("No bundle found with name: "+bundleName+". Unable to load class "+className);
        }
        return bundle.loadClass(className);
    }

    public Object newInstance(String ref) throws Exception {
        return loadClass(ref).newInstance();
    }

    public Object newInstance(String bundleName, String className) throws Exception {
        return loadClass(bundleName, className).newInstance();
    }

    /**
     * Gives a chance to derived classes to initialize them before the runtime is
     * started.
     *
     * @param runtime the current runtime
     */
    protected void initialize(OSGiRuntimeService runtime) {
        // do nothing
    }

    /**
     * Gives a chance to derived classes to uninitialize them after the runtime
     * is stopped.
     *
     * @param runtime the current runtime
     */
    protected void uninitialize(OSGiRuntimeService runtime) {
        // do nothing
    }

}
