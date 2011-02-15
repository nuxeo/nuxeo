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

package org.nuxeo.opensocial.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * We cannot have two bundle activators so we use this single bundle activator.
 * Further, this insures that the init sequence happens in the right order.
 *
 * @author Ian Smith<ismith@nuxeo.com>
 *
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
    public void start(BundleContext context) throws Exception {
        context.addFrameworkListener(this);
    }

    public void stop(BundleContext context) throws Exception {

    }

    /*
     * This is the point where the initialization actually occurs. This is
     * called by the framework when it's finished initializing and we echo that
     * message first to the initialization of the OS service, then to the two
     * objects that we have delayed.
     */
    public void frameworkEvent(FrameworkEvent event) {
        try {
            if (event.getType() == FrameworkEvent.STARTED) {
                Framework.getService(OpenSocialService.class).setupOpenSocial();
                ContextListenerDelayer.activate(event);
                AuthenticationFilterDelayer.activate(event);
            }
        } catch (Exception e) {
            log.error("Unable to initialize/configure the open social "
                    + "service", e);
        }
    }

}
