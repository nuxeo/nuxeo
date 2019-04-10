/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.snapshot;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.introspection.OperationInfoImpl;

public class SnapshotFilter {

    protected final String bundleGroupName;

    protected final List<String> bundlePrefixes = new ArrayList<String>();

    protected final List<String> packagesPrefixes = new ArrayList<String>();

    public SnapshotFilter(String groupName) {
        bundleGroupName = groupName;
    }

    public String getBundleGroupName() {
        return bundleGroupName;
    }

    public List<String> getBundlePrefixes() {
        return bundlePrefixes;
    }

    public void addBundlePrefix(String bundlePrefix) {
        bundlePrefixes.add(bundlePrefix);
    }

    public List<String> getPackagesPrefixes() {
        return packagesPrefixes;
    }

    public void addPackagesPrefix(String packagesPrefix) {
        packagesPrefixes.add(packagesPrefix);
    }

    public boolean includeBundleId(String bundleId) {
     for (String bprefix : bundlePrefixes) {
         if (bundleId.startsWith(bprefix)) {
             return true;
         }
     }
     return false;
    }

    public boolean includeSeamComponent(SeamComponentInfo seamComponent) {

        for (String iface : seamComponent.getInterfaceNames()) {
            for (String pprefix : packagesPrefixes) {
                if (iface.startsWith(pprefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean includeOperation(OperationInfoImpl op) {
        for (String pprefix : packagesPrefixes) {
            if (op.getOperationClass().startsWith(pprefix)) {
                return true;
            }
        }
        return false;
    }
}

