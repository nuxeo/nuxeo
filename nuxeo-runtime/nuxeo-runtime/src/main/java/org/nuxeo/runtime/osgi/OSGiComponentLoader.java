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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;


/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiComponentLoader implements SynchronousBundleListener {

    private final OSGiRuntimeService runtime;
    private static final Log log = LogFactory.getLog(OSGiComponentLoader.class);

    public OSGiComponentLoader(OSGiRuntimeService runtime) {
        this.runtime = runtime;
        install();
    }

    public void install() {
        BundleContext ctx = runtime.getBundleContext();
        ctx.addBundleListener(this);
        Bundle[] bundles = ctx.getBundles();
        int mask = Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE;
        for (Bundle bundle : bundles) {
            int state = bundle.getState();
            //System.out.println("bundle: "+bundle.getSymbolicName()+" = "+state);
            if ((state & mask) != 0) { // check only resolved bundles
                if (OSGiRuntimeService.getComponentsList(bundle) != null) {
                    // check only bundles containing nuxeo comp.
                    try {
                        runtime.createContext(bundle);
                    } catch (Throwable e) {
                        log.warn("Failed to load components for bundle - "
                                + bundle.getSymbolicName(),e);
                    }
                }
            }
        }
    }

    public void uninstall() {
        runtime.getBundleContext().removeBundleListener(this);
    }

    public void bundleChanged(BundleEvent event) {
        //System.out.println("bundleEvent: "+event.getBundle().getSymbolicName()+" = "+event.getType());
        try {
            Bundle bundle = event.getBundle();
            int type = event.getType();
            switch (type) {
            case BundleEvent.RESOLVED:
                if (OSGiRuntimeService.getComponentsList(bundle) != null) {
                    runtime.createContext(bundle);
                }
                break;
            case BundleEvent.UNRESOLVED:
                if (OSGiRuntimeService.getComponentsList(bundle) != null) {
                    runtime.destroyContext(bundle);
                }
                break;
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

}
