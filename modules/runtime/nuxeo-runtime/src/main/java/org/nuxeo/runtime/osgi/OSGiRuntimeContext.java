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

import org.nuxeo.runtime.AbstractRuntimeService;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
        // workaround to correctly handle fragment class loaders
        hostBundleId = (String) bundle.getHeaders().get(Constants.FRAGMENT_HOST);
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
            if (hostBundleId != null) { // workaround to handle fragment bundles that doesn't have class loaders
                return getHostBundle().loadClass(className);
            }
            return bundle.loadClass(className);
        } catch (ClassNotFoundException e) {
            return Framework.getResourceLoader().loadClass(className);
        }
    }

    public Bundle getHostBundle() {
        if (hostBundleId != null) {
            if (hostBundle == null && runtime instanceof AbstractRuntimeService) {
                hostBundle = ((OSGiRuntimeService) runtime).findHostBundle(bundle);
            }
        }
        return hostBundle;
    }

}
