/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.core.opencmis.bindings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * This bundle activator ensures that the init sequence happens in the right
 * order.
 */
public class Activator implements BundleActivator, FrameworkListener {

    private static final Log log = LogFactory.getLog(Activator.class);

    /*
     * Called when our bundle is started. All we do is ask for an event when the
     * entire Framework is ready.
     *
     * @see
     * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
     * )
     */
    @Override
    public void start(BundleContext context) throws Exception {
        context.addFrameworkListener(this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }

    /*
     * This is the point where the initialization actually occurs. This is
     * called by the framework when it's finished initializing and we echo that
     * message to the object that we have delayed.
     */
    @Override
    public void frameworkEvent(FrameworkEvent event) {
        try {
            if (event.getType() == FrameworkEvent.STARTED) {
                ContextListenerDelayer.activate(event);
            }
        } catch (Exception e) {
            log.error("Unable to initialize/configure the Cmis service", e);
        }
    }

}
