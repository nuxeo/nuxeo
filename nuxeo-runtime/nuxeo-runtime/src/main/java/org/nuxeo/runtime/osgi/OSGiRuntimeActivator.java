/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.nuxeo.runtime.AbstractRuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * The default BundleActivator for NXRuntime over an OSGi comp. platform.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
    public void start(BundleContext context) {
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
    public void stop(BundleContext context) {
        log.info("Stopping Runtime Activator");
        instance = null;
        pkgAdmin = null;
        // remove component loader
        componentLoader.uninstall();
        componentLoader = null;
        // unregister
        try {
            Framework.shutdown();
            uninitialize(runtime);
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during shutdown", cause);
        } finally {
            runtime = null;
            this.context = null;
        }
    }

    public Bundle getBundle(String name) {
        if (pkgAdmin == null) {
            return null;
        }
        PackageAdmin pa = (PackageAdmin) context.getService(pkgAdmin);
        Bundle[] bundles = pa.getBundles(name, null);
        context.ungetService(pkgAdmin);
        return bundles == null ? null : bundles[0];
    }

    /**
     * Load a class from another bundle given its reference as <code>bundleSymbolicName:className</code> If no
     * <code>bundleSymbolicName:</code> prefix is given then a classForName will be done
     *
     * @param ref
     * @return
     */
    public Class<?> loadClass(String ref) throws ReflectiveOperationException {
        int i = ref.indexOf(':');
        if (i == -1) {
            return Class.forName(ref);
        }
        return loadClass(ref.substring(0, i), ref.substring(i + 1));
    }

    public Class<?> loadClass(String bundleName, String className) throws ReflectiveOperationException {
        Bundle bundle = getBundle(bundleName);
        if (bundle == null) {
            throw new ClassNotFoundException("No bundle found with name: " + bundleName + ". Unable to load class "
                    + className);
        }
        return bundle.loadClass(className);
    }

    public Object newInstance(String ref) throws ReflectiveOperationException {
        return loadClass(ref).newInstance();
    }

    public Object newInstance(String bundleName, String className) throws ReflectiveOperationException {
        return loadClass(bundleName, className).newInstance();
    }

    /**
     * Gives a chance to derived classes to initialize them before the runtime is started.
     *
     * @param runtime the current runtime
     */
    protected void initialize(AbstractRuntimeService runtime) {
        // do nothing
    }

    /**
     * Gives a chance to derived classes to uninitialize them after the runtime is stopped.
     *
     * @param runtime the current runtime
     */
    protected void uninitialize(AbstractRuntimeService runtime) {
        // do nothing
    }

}
