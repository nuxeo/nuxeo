/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.connect.update.task.guards;

import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;

/**
 * Helper to access the package update service from JEXL.
 *
 * @since 8.4
 */
public class PackagesHelper {

    private final PackageUpdateService service;

    public PackagesHelper(PackageUpdateService service) {
        this.service = service;
    }

    public boolean contains(String name) {
        try {
            if (name.contains(":")) {
                // exact version
                name = name.replace(':', '-');
                Package pkg = service.getPackage(name);
                return pkg != null && isPackageInstalled(pkg);
            } else {
                // any version
                for (Package pkg : service.getPackages()) {
                    // multiple packages can have the same name (not id),
                    // iterate until an installed one is found.
                    if (pkg.getName().equals(name) && isPackageInstalled(pkg)) {
                        return true;
                    }
                }
                return false;
            }
        } catch (PackageException e) {
            return false;
        }
    }

    protected boolean isPackageInstalled(Package pkg) {
        PackageState state = pkg.getPackageState();
        switch (state) {
        case INSTALLING:
        case INSTALLED:
        case STARTED:
            return true;
        default:
            return false;
        }
    }

}
