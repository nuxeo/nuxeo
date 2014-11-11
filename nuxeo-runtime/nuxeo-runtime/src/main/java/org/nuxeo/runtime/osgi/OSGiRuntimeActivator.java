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

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The default BundleActivator for NXRuntime over an OSGi comp. platform.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiRuntimeActivator implements BundleActivator {

    private static final Log log = LogFactory.getLog(OSGiRuntimeActivator.class);

    protected OSGiRuntimeService runtime;
    protected OSGiComponentLoader componentLoader;


    public void start(BundleContext context) throws Exception {
        log.info("Starting Runtime Activator");
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

    public void stop(BundleContext context) throws Exception {
        log.info("Stopping Runtime Activator");
        // remove component loader
        componentLoader.uninstall();
        componentLoader = null;
        // unregister
        Framework.shutdown();
        uninitialize(runtime);
        runtime = null;
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
