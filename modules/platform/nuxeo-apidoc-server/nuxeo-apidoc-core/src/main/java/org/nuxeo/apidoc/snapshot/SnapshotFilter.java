/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.snapshot;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.introspection.OperationInfoImpl;

public class SnapshotFilter {

    protected final String bundleGroupName;

    protected final List<String> bundlePrefixes = new ArrayList<>();

    protected final List<String> javaPackagePrefixes = new ArrayList<>();

    protected final List<String> nxpackagePrefixes = new ArrayList<>();

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
        return javaPackagePrefixes;
    }

    public void addPackagesPrefix(String packagesPrefix) {
        javaPackagePrefixes.add(packagesPrefix);
    }

    /** @since 11.1 */
    public List<String> getNuxeoPackagesPrefixes() {
        return nxpackagePrefixes;
    }

    /** @since 11.1 */
    public void addNuxeoPackagePrefix(String packagePrefix) {
        nxpackagePrefixes.add(packagePrefix);
    }

    public boolean includeBundleId(String bundleId) {
        for (String bprefix : bundlePrefixes) {
            if (bundleId.startsWith(bprefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @implNote checks the operation class against package prefixes. Note some operations are actually not classes, so
     *           it could be possible to filter on contributing component, retrieving its bundle, and checking bundle
     *           prefixes too... (not done).
     */
    public boolean includeOperation(OperationInfoImpl op) {
        for (String pprefix : javaPackagePrefixes) {
            if (op.getOperationClass().startsWith(pprefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the Nuxeo packages bundles against Nuxeo packages prefix, as well as included bundles against bundle
     * prefixes.
     *
     * @since 11.1
     */
    public boolean includePackage(PackageInfo pkg) {
        for (String pprefix : nxpackagePrefixes) {
            if (pkg.getId().startsWith(pprefix)) {
                return true;
            }
        }
        for (String bundle : pkg.getBundles()) {
            for (String bprefix : bundlePrefixes) {
                if (bundle.startsWith(bprefix)) {
                    return true;
                }
            }
        }
        return false;
    }

}
