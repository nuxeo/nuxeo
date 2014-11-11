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
package org.nuxeo.osgi.services;

import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

/**
 * Dummy implementation of {@link PackageAdmin} service.
 * Only {@link PackageAdmin#getBundles(String, String)} is implemented
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PackageAdminImpl implements PackageAdmin {

    protected OSGiAdapter osgi;

    public PackageAdminImpl(OSGiAdapter osgi) {
        this.osgi = osgi;
    }

    @Override
    public Bundle[] getBundles(String symbolicName, String versionRange) {
        return new Bundle[] {osgi.getBundle(symbolicName)};
    }

    @Override
    public ExportedPackage[] getExportedPackages(Bundle bundle) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ExportedPackage[] getExportedPackages(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ExportedPackage getExportedPackage(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void refreshPackages(Bundle[] bundles) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean resolveBundles(Bundle[] bundles) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public RequiredBundle[] getRequiredBundles(String symbolicName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Bundle[] getFragments(Bundle bundle) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Bundle[] getHosts(Bundle bundle) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Bundle getBundle(Class clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getBundleType(Bundle bundle) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
