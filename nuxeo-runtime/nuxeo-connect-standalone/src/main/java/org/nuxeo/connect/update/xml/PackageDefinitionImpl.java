/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     jcarsique
 *     Yannis JULIENNE
 */
package org.nuxeo.connect.update.xml;

import org.apache.commons.lang.mutable.MutableObject;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.connect.data.PackageDescriptor;
import org.nuxeo.connect.update.NuxeoValidationState;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageVisibility;
import org.nuxeo.connect.update.ProductionState;
import org.nuxeo.connect.update.Validator;
import org.nuxeo.connect.update.Version;
import org.nuxeo.connect.update.model.PackageDefinition;
import org.nuxeo.connect.update.model.TaskDefinition;
import org.nuxeo.connect.update.task.Task;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("package")
public class PackageDefinitionImpl implements PackageDefinition {

    @XNode("@name")
    protected String name;

    @XNode("@version")
    protected Version version;

    @XNode("@type")
    protected PackageType type;

    @XNode("visibility")
    protected PackageVisibility visibility;

    @XNode("title")
    protected String title;

    @XNode("description")
    protected String description;

    @XNode("classifier")
    protected String classifier;

    @XNode("vendor")
    protected String vendor;

    @XNode("home-page")
    protected String homePage;

    @XNode("supported")
    protected boolean supported = false;

    @XNode("hotreload-support")
    protected boolean hotReloadSupport = false;

    @XNode("require-terms-and-conditions-acceptance")
    protected boolean requireTermsAndConditionsAcceptance = false;

    protected NuxeoValidationState validationState = NuxeoValidationState.NONE;

    protected ProductionState productionState = ProductionState.TESTING;

    /**
     * The license name. E.g. LGPL, BSD etc.
     */
    @XNode("license")
    protected String license;

    /**
     * A license URL. If no specified the license.txt file in the package is the license content
     */
    @XNode("license-url")
    protected String licenseUrl;

    /**
     * The target platforms where this package may be installed.
     */
    protected String[] platforms;

    /**
     * The dependency value format is: <code>package_name[:package_min_version[:package_max_version]]</code> if no min
     * and max version are specified the the last version should be used.
     */
    @XNodeList(value = "dependencies/package", type = PackageDependency[].class, componentType = PackageDependency.class)
    protected PackageDependency[] dependencies;

    /**
     * The optional dependencies are defined for ordering purpose, to make sure that if they are being installed along
     * with the current package, they will be ordered first.
     */
    @XNodeList(value = "optional-dependencies/package", type = PackageDependency[].class, componentType = PackageDependency.class)
    protected PackageDependency[] optionalDependencies;

    /**
     * The conflict value format is: <code>package_name[:package_min_version[:package_max_version]]</code> if no min and
     * max version are specified the the last version should be used.
     */
    @XNodeList(value = "conflicts/package", type = PackageDependency[].class, componentType = PackageDependency.class)
    protected PackageDependency[] conflicts;

    /**
     * The provides value format is: <code>package_name[:package_min_version[:package_max_version]]</code> if no min and
     * max version are specified the the last version should be used.
     */
    @XNodeList(value = "provides/package", type = PackageDependency[].class, componentType = PackageDependency.class)
    protected PackageDependency[] provides;

    /**
     * A class implementing {@link Task}. if not specified the default implementation will be used
     */
    @XNode("installer")
    protected TaskDefinitionImpl installer;

    /**
     * A class implementing {@link Task}. if not specified the default implementation will be used
     */
    @XNode("uninstaller")
    protected TaskDefinitionImpl uninstaller;

    /**
     * A class implementing {@link Validator}. If not specified not post install validation will be done
     */
    @XNode("validator")
    protected String validator;

    @XNode("nuxeo-validation")
    protected void initNuxeoValidationState(String value) {
        NuxeoValidationState targetState = NuxeoValidationState.getByValue(value);
        if (targetState != null) {
            validationState = targetState;
        }
    }

    @XNode("production-state")
    protected void initProductionState(String value) {
        ProductionState targetState = ProductionState.getByValue(value);
        if (targetState != null) {
            productionState = targetState;
        }
    }

    @Override
    public String getId() {
        if (version == null) {
            return name;
        } else {
            return name + "-" + version.toString();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        dependencies = PackageDescriptor.fixDependencies(name, dependencies);
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public void setVersion(Version version) {
        this.version = version;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public PackageType getType() {
        return type;
    }

    @Override
    public void setType(PackageType type) {
        this.type = type;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    @Override
    public String getHomePage() {
        return homePage;
    }

    @Override
    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }

    @Deprecated
    @Override
    public String getLicense() {
        return getLicenseType();
    }

    @Override
    public String getLicenseType() {
        return license;
    }

    @Deprecated
    @Override
    public void setLicense(String license) {
        setLicenseType(license);
    }

    @Override
    public void setLicenseType(String license) {
        this.license = license;
    }

    @Override
    public String getLicenseUrl() {
        return licenseUrl;
    }

    @Override
    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    @Deprecated
    @Override
    public String[] getPlatforms() {
        return getTargetPlatforms();
    }

    @Override
    public String[] getTargetPlatforms() {
        return platforms;
    }

    @Deprecated
    @Override
    public void setPlatforms(String[] platforms) {
        setTargetPlatforms(platforms);
    }

    @XNodeList(value = "platforms/platform", type = String[].class, componentType = String.class)
    @Override
    public void setTargetPlatforms(String[] platforms) {
        MutableObject packageDependencies = new MutableObject();
        this.platforms = PackageDescriptor.fixTargetPlatforms(name, platforms, packageDependencies);
        setDependencies((PackageDependency[]) packageDependencies.getValue());
    }

    @Override
    public PackageDependency[] getDependencies() {
        return dependencies;
    }

    @Override
    public PackageDependency[] getOptionalDependencies() {
        return optionalDependencies;
    }

    @Override
    public void setDependencies(PackageDependency[] dependencies) {
        this.dependencies = PackageDescriptor.addPackageDependencies(this.dependencies, dependencies);
    }

    @Override
    public PackageDependency[] getConflicts() {
        return conflicts;
    }

    @Override
    public void setConflicts(PackageDependency[] conflicts) {
        this.conflicts = conflicts;
    }

    @Override
    public PackageDependency[] getProvides() {
        return provides;
    }

    @Override
    public void setProvides(PackageDependency[] provides) {
        this.provides = provides;
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    @Override
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    @Override
    public TaskDefinition getInstaller() {
        return installer;
    }

    @Override
    public void setInstaller(TaskDefinition installer) {
        if (installer instanceof TaskDefinitionImpl) {
            this.installer = (TaskDefinitionImpl) installer;
        } else {
            this.installer = new TaskDefinitionImpl(installer.getType(), installer.getRequireRestart());
        }
    }

    @Override
    public TaskDefinition getUninstaller() {
        return uninstaller;
    }

    @Override
    public void setUninstaller(TaskDefinition uninstaller) {
        if (uninstaller instanceof TaskDefinitionImpl) {
            this.uninstaller = (TaskDefinitionImpl) uninstaller;
        } else {
            this.uninstaller = new TaskDefinitionImpl(uninstaller.getType(), uninstaller.getRequireRestart());
        }
    }

    @Override
    public String getValidator() {
        return validator;
    }

    @Override
    public void setValidator(String validator) {
        this.validator = validator;
    }

    @Override
    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    @Override
    public void setHotReloadSupport(boolean hotReloadSupport) {
        this.hotReloadSupport = hotReloadSupport;
    }

    @Override
    public void setValidationState(NuxeoValidationState validationState) {
        this.validationState = validationState;
    }

    @Override
    public void setProductionState(ProductionState productionState) {
        this.productionState = productionState;
    }

    @Deprecated
    public void write(XmlWriter writer) {
        writer.writeXmlDecl();

        writer.start("package");
        writer.attr("name", name);
        if (version != null) {
            writer.attr("version", version.toString());
        }
        if (type != null) {
            writer.attr("type", type.getValue());
        }
        if (visibility != null) {
            writer.attr("visibility", visibility.toString());
        }
        writer.startContent();
        writer.element("title", title);
        writer.element("description", description);
        writer.element("classifier", classifier);
        writer.element("vendor", vendor);
        writer.element("home-page", homePage);
        writer.element("license", license);
        writer.element("license-url", licenseUrl);
        writer.element("hotreload-support", Boolean.valueOf(hotReloadSupport).toString());
        writer.element("supported", Boolean.valueOf(supported).toString());
        writer.element("require-terms-and-conditions-acceptance",
                Boolean.valueOf(requireTermsAndConditionsAcceptance).toString());
        writer.element("production-state", productionState.toString());
        writer.element("nuxeo-validation", validationState.toString());
        if (platforms != null) {
            writer.start("platforms");
            writer.startContent();
            for (String platform : platforms) {
                writer.element("platform", platform);
            }
            writer.end("platforms");
        }
        if (dependencies != null) {
            writer.start("dependencies");
            writer.startContent();
            for (PackageDependency dep : dependencies) {
                writer.element("package", dep.toString());
            }
            writer.end("dependencies");
        }

        if (installer != null) {
            writer.start("installer");
            writer.attr("class", installer.getType());
            writer.attr("restart", String.valueOf(installer.getRequireRestart()));
            writer.end();
        }
        if (uninstaller != null) {
            writer.start("uninstaller");
            writer.attr("class", uninstaller.getType());
            writer.attr("restart", String.valueOf(uninstaller.getRequireRestart()));
            writer.end();
        }
        writer.element("validator", validator);
        writer.end("package");
    }

    @Override
    public ProductionState getProductionState() {
        return productionState;
    }

    @Override
    public NuxeoValidationState getValidationState() {
        return validationState;
    }

    @Override
    public boolean isSupported() {
        return supported;
    }

    @Override
    public boolean supportsHotReload() {
        return hotReloadSupport;
    }

    @Override
    public void setRequireTermsAndConditionsAcceptance(boolean requireTermsAndConditionsAcceptance) {
        this.requireTermsAndConditionsAcceptance = requireTermsAndConditionsAcceptance;
    }

    @Override
    public boolean requireTermsAndConditionsAcceptance() {
        return requireTermsAndConditionsAcceptance;
    }

    @Override
    public String toXML() {
        return new XmlSerializer().toXML(this);
    }

    @Deprecated
    @Override
    public int getState() {
        return PackageState.UNKNOWN.getValue();
    }

    @Override
    public PackageState getPackageState() {
        return PackageState.UNKNOWN;
    }

    @Override
    public boolean isLocal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PackageVisibility getVisibility() {
        return visibility;
    }

    @Override
    public void setVisibility(PackageVisibility visibility) {
        this.visibility = visibility;
    }

}
