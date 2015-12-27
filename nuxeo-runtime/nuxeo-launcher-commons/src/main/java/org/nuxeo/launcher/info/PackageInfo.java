/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mguillaume
 */

package org.nuxeo.launcher.info;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.nuxeo.connect.update.NuxeoValidationState;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageVisibility;
import org.nuxeo.connect.update.ProductionState;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "package")
@XmlType(propOrder = { "id", "state", "version", "name", "type", "visibility", "targetPlatforms", "vendor",
        "supportsHotReload", "supported", "productionState", "validationState", "provides", "dependencies",
        "conflicts", "title", "description", "homePage", "licenseType", "licenseUrl" })
public class PackageInfo {

    public String name;

    public String version;

    public String id;

    public PackageState state;

    public String title;

    public String description;

    public String homePage;

    public String licenseType;

    public String licenseUrl;

    public ProductionState productionState;

    public NuxeoValidationState validationState;

    public String[] targetPlatforms;

    public PackageType type;

    public String vendor;

    public PackageVisibility visibility;

    public PackageDependency[] provides;

    public PackageDependency[] dependencies;

    public PackageDependency[] conflicts;

    public boolean supportsHotReload;

    public boolean supported;

    public PackageInfo() {
    }

    /**
     * @deprecated since 5.7
     */
    @Deprecated
    public PackageInfo(String name, String version, String id, int state) {
        this.name = name;
        this.version = version;
        this.id = id;
        this.state = PackageState.getByValue(state);
    }

    /**
     * @since 5.7
     */
    public PackageInfo(Package pkg) {
        name = pkg.getName();
        version = pkg.getVersion().toString();
        id = pkg.getId();
        state = pkg.getPackageState();
        title = pkg.getTitle();
        description = pkg.getDescription();
        homePage = pkg.getHomePage();
        licenseType = pkg.getLicenseType();
        licenseUrl = pkg.getLicenseUrl();
        productionState = pkg.getProductionState();
        validationState = pkg.getValidationState();
        targetPlatforms = pkg.getTargetPlatforms();
        type = pkg.getType();
        vendor = pkg.getVendor();
        visibility = pkg.getVisibility();
        if (visibility == null) {
            visibility = PackageVisibility.UNKNOWN;
        }
        provides = pkg.getProvides();
        dependencies = pkg.getDependencies();
        conflicts = pkg.getConflicts();
        supportsHotReload = pkg.supportsHotReload();
        supported = pkg.isSupported();
    }

}
