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
package org.nuxeo.runtime.jtajca;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * If this bundle is present in the running platform it should automatically install
 * the NuxeoContainer.
 *
 * TODO: enable this activator when other distributions are tested and works correctly or use a framework property to activate it.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Activator implements BundleActivator {

    public final static String AUTO_ACTIVATION = "NuxeoContainer.autoactivation";

    @Override
    public void start(BundleContext context) throws Exception {
        // instantiate it only on demand through the AUTO_ACTIVATION runtime property since
        // it may break the application in test mode or in other non osgi distributions
        // where the container is explicitly activated
        // TODO: use this activation method in all distributions too.
        if ("true".equalsIgnoreCase(Framework.getProperty(AUTO_ACTIVATION))) {
            // if no InitialContext exists install the dummy one.
            try {
                new InitialContext();
            } catch (NamingException e) {
                NamingContextFactory.install();
            }
            NuxeoContainer.install();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        NuxeoContainer.uninstall();
    }

}
