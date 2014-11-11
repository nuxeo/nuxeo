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

import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiRuntimeContext extends DefaultRuntimeContext {

    protected final Bundle bundle;
    protected String hostBundleId;
    protected Bundle hostBundle;

    public OSGiRuntimeContext(Bundle bundle) {
        this(Framework.getRuntime(), bundle);
    }

    public OSGiRuntimeContext(RuntimeService runtime, Bundle bundle) {
        super(runtime);
        this.bundle = bundle;
        // hack to correctly handle fragment class loaders
        hostBundleId = (String)bundle.getHeaders().get(Constants.FRAGMENT_HOST);
        if (hostBundleId != null) {
            int p = hostBundleId.indexOf(';');
            if (p > -1) { // remove version or other extra information if any
                hostBundleId = hostBundleId.substring(0, p);
            }
        }
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public URL getResource(String name) {
        if (hostBundleId != null) {
            return getHostBundle().getResource(name);
        }
        return bundle.getResource(name);
    }

    @Override
    public URL getLocalResource(String name) {
        if (hostBundleId != null) {
            return getHostBundle().getEntry(name);
        }
        return bundle.getEntry(name);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        if (hostBundleId != null) { // hack to handle fragment bundles that doesn't have class loaders
            return getHostBundle().loadClass(className);
        }
        return bundle.loadClass(className);
    }

    public Bundle getHostBundle() {
        if (hostBundleId != null) {
            if (hostBundle == null && runtime instanceof OSGiRuntimeService) {
                hostBundle = ((OSGiRuntimeService)runtime).findHostBundle(bundle);
            }
        }
        return hostBundle;
    }

}
