/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.osgi;

import java.io.File;
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

        // if no environment was setup create it now.
        initEnvironment();

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
        return bundles[0];
    }

    protected void initEnvironment() {
        if (Environment.getDefault() == null) {
            String homeDir = System.getProperty("nuxeo.home");
            if (homeDir != null) {
                File home = new File(homeDir);
                if (home.isDirectory()) {
                    Environment.setDefault(new Environment(home));
                }
            }
        }
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
