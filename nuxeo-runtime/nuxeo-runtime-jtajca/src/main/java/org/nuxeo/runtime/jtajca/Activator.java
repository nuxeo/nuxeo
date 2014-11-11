/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 */
public class Activator implements BundleActivator {

    public static final String AUTO_ACTIVATION = "NuxeoContainer.autoactivation";

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
