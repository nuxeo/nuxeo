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
 *     gracinet
 *
 * $Id$
 */

package org.nuxeo.runtime.test;

import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;

/**
 * @author gracinet
 */
public class RootRuntimeBundle extends BundleImpl {

    public RootRuntimeBundle(OSGiAdapter osgi, BundleFile file, ClassLoader loader) throws BundleException {
        super(osgi, file, loader);
    }

    public RootRuntimeBundle(OSGiAdapter osgi, BundleFile file, ClassLoader loader, boolean isSystemBundle)
            throws BundleException {
        super(osgi, file, loader, isSystemBundle);
    }

    @Override
    public BundleActivator getActivator() {
        if (activator == null) {
            activator = new OSGIRuntimeTestActivator();
        }
        return activator;
    }

}
