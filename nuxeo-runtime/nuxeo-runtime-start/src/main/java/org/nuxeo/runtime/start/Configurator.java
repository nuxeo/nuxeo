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
package org.nuxeo.runtime.start;

import org.osgi.framework.BundleContext;

/**
 * Hook to configure a nuxeo application before and after starting the runtime.
 * Hooks can be installed using runtime fragments by defining a
 * nuxeo.configurator in a system.properties file in the fragment root.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Configurator {

    /**
     * Runtime about to be started.
     * @param context the runtime bundle context
     * @throws Exception
     */
    public void beforeStart(BundleContext context) throws Exception;

    /**
     * Runtime was started - all available extensions were contributed. Application is started.
     * @param context the runtime context
     * @throws Exception
     */
    public void afterStart(BundleContext context) throws Exception;

    /**
     * Runtime about to be stopped.
     * @param context the runtime bundle context
     * @throws Exception
     */
    public void beforeStop(BundleContext context) throws Exception;

    /**
     * Runtime is stopped. Application is down.
     * @param context
     * @throws Exception
     */
    public void afterStop(BundleContext context) throws Exception;

}
