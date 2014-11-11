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
package org.nuxeo.connect.update.xml;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.connect.update.NuxeoValidationState;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.ProductionState;
import org.nuxeo.connect.update.Validator;
import org.nuxeo.connect.update.Version;
import org.nuxeo.connect.update.model.PackageDefinition;
import org.nuxeo.connect.update.model.TaskDefinition;
import org.nuxeo.connect.update.task.Task;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("package")
public class PackageDefinitionImpl implements PackageDefinition {

    @XNode("@name")
    protected String name;

    @XNode("@version")
    protected Version version;

    @XNode("@type")
    protected PackageType type;

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
    protected boolean supported=false;

    @XNode("hotreload-support")
    protected boolean hotReloadSupport=false;

    @XNode("require-terms-and-conditions-acceptance")
    protected boolean requireTermsAndConditionsAcceptance=false;

    protected NuxeoValidationState validationState = NuxeoValidationState.NONE;

    protected ProductionState productionState = ProductionState.TESTING;

    /**
     * The license name. E.g. LGPL, BSD etc.
     */
    @XNode("license")
    protected String license;

    /**
     * A license URL. If no specified the license.txt file in the package is the
     * license content
     */
    @XNode("license-url")
    protected String licenseUrl;

    /**
     * The target platforms where this package may be installed.
     */
    @XNodeList(value = "platforms/platform", type = String[].class, componentType = String.class)
    protected String[] platforms;

    /**
     * The dependency value format is:
     * <code>package_name[:package_min_version[:package_max_version]]</code> if
     * no min and max version are specified the the last version should be used.
     */
    @XNodeList(value = "dependencies/package", type = PackageDependency[].class, componentType = PackageDependency.class)
    protected PackageDependency[] dependencies;

    /**
     * A class implementing {@link Task}. if not specified the default
     * implementation will be used
     */
    @XNode("installer")
    protected TaskDefinitionImpl installer;

    /**
     * A class implementing {@link Task}. if not specified the default
     * implementation will be used
     */
    @XNode("uninstaller")
    protected TaskDefinitionImpl uninstaller;

    /**
     * A class implementing {@link Validator}. If not specified not post install
     * validation will be done
     */
    @XNode("validator")
    protected String validator;

    @XNode("nuxeo-validation")
    protected void initNuxeoValidationState(String value) {
        NuxeoValidationState targetState = NuxeoValidationState.getByValue(value);
        if (targetState!=null) {
            validationState = targetState;
        }
    }

    @XNode("production-state")
    protected void initProductionState(String value) {
        ProductionState targetState = ProductionState.getByValue(value);
        if (targetState!=null) {
            productionState = targetState;
        }
    }


    public String getId() {
        if (version == null) {
            return name;
        } else {
            return name + "-" + version.toString();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PackageType getType() {
        return type;
    }

    public void setType(PackageType type) {
        this.type = type;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getHomePage() {
        return homePage;
    }

    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public String[] getPlatforms() {
        return platforms;
    }

    public void setPlatforms(String[] platforms) {
        this.platforms = platforms;
    }

    public PackageDependency[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(PackageDependency[] dependencies) {
        this.dependencies = dependencies;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public TaskDefinition getInstaller() {
        return installer;
    }

    public void setInstaller(TaskDefinition installer) {
        if (installer instanceof TaskDefinitionImpl) {
            this.installer = (TaskDefinitionImpl) installer;
        } else {
            this.installer = new TaskDefinitionImpl(installer.getType(),
                    installer.getRequireRestart());
        }
    }

    public TaskDefinition getUninstaller() {
        return uninstaller;
    }

    public void setUninstaller(TaskDefinition uninstaller) {
        if (uninstaller instanceof TaskDefinitionImpl) {
            this.uninstaller = (TaskDefinitionImpl) uninstaller;
        } else {
            this.uninstaller = new TaskDefinitionImpl(uninstaller.getType(),
                    uninstaller.getRequireRestart());
        }
    }

    public String getValidator() {
        return validator;
    }

    public void setValidator(String validator) {
        this.validator = validator;
    }

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    public void setHotReloadSupport(boolean hotReloadSupport) {
        this.hotReloadSupport = hotReloadSupport;
    }

    public void setValidationState(NuxeoValidationState validationState) {
        this.validationState = validationState;
    }

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
        writer.element("require-terms-and-conditions-acceptance", Boolean.valueOf(requireTermsAndConditionsAcceptance).toString());
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
            writer.attr("restart",
                    String.valueOf(installer.getRequireRestart()));
            writer.end();
        }
        if (uninstaller != null) {
            writer.start("uninstaller");
            // FIXME: I think this should be 'uninstaller' below, not 'installer'
            writer.attr("class", installer.getType());
            writer.attr("restart",
                    String.valueOf(installer.getRequireRestart()));
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

    public void setRequireTermsAndConditionsAcceptance(
            boolean requireTermsAndConditionsAcceptance) {
        this.requireTermsAndConditionsAcceptance = requireTermsAndConditionsAcceptance;
    }

    @Override
    public boolean requireTermsAndConditionsAcceptance() {
        return requireTermsAndConditionsAcceptance;
    }

    public String toXML() {
        return new XmlSerializer().toXML(this);
    }


}
