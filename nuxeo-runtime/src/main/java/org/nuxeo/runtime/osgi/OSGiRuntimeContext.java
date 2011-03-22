/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        URL url = null;
        if (hostBundleId != null) {
            url = getHostBundle().getResource(name);
        } else {
            url = bundle.getResource(name);
        }
        if (url == null) {
            url = Framework.getResourceLoader().getResource(name);
        }
        return url;
    }

    @Override
    public URL getLocalResource(String name) {
        URL url = null;
        if (hostBundleId != null) {
            url = getHostBundle().getEntry(name);
        } else {
            url = bundle.getEntry(name);
        }
        if (url == null) {
            url = Framework.getResourceLoader().getResource(name);
        }
        return url;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        try {
            if (hostBundleId != null) { // hack to handle fragment bundles that doesn't have class loaders
                return getHostBundle().loadClass(className);
            }
            return bundle.loadClass(className);
        } catch (ClassNotFoundException e) {
            return Framework.getResourceLoader().loadClass(className);
        }
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
