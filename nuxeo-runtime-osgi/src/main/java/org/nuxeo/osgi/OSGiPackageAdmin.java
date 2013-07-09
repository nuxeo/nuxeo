/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.osgi;

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
public class OSGiPackageAdmin implements PackageAdmin {

    protected OSGiSystemContext osgi;

    public OSGiPackageAdmin(OSGiSystemContext osgi) {
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
        return  osgi.registry.getFragments(bundle.getSymbolicName());
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
