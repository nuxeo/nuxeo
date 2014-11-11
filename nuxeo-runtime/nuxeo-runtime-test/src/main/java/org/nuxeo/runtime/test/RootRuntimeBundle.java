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
 *     gracinet
 *
 * $Id$
 */

package org.nuxeo.runtime.test;

import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.runtime.launcher.BundleFile;
import org.osgi.framework.BundleActivator;

/**
 * @author gracinet
 *
 */
public class RootRuntimeBundle extends BundleImpl {

    public RootRuntimeBundle(OSGiAdapter osgi, BundleFile file,
            ClassLoader loader) {
        super(osgi, file, loader);
    }

    public RootRuntimeBundle(OSGiAdapter osgi, BundleFile file,
            ClassLoader loader, boolean isSystemBundle) {
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
