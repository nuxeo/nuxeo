/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Mathieu Guillaume
 */
package org.nuxeo.connect.update.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.NuxeoValidationState;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageVisibility;
import org.nuxeo.connect.update.ProductionState;
import org.nuxeo.connect.update.Version;
import org.nuxeo.connect.update.model.PackageDefinition;
import org.nuxeo.connect.update.model.TaskDefinition;
import org.nuxeo.connect.update.xml.FormDefinition;
import org.nuxeo.connect.update.xml.FormsDefinition;
import org.nuxeo.connect.update.xml.PackageDefinitionImpl;
import org.nuxeo.connect.update.xml.TaskDefinitionImpl;
import org.nuxeo.connect.update.xml.XmlSerializer;
import org.nuxeo.runtime.api.Framework;

/**
 * Build an XML representation of a package.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PackageBuilder {

    protected final PackageDefinition def;

    protected final List<FormDefinition> installForms;

    protected final List<FormDefinition> uninstallForms;

    protected final List<FormDefinition> validationForms;

    protected final List<String> platforms;

    protected final List<PackageDependency> dependencies;

    protected final List<PackageDependency> conflicts;

    protected final List<PackageDependency> provides;

    protected final LinkedHashMap<String, InputStream> entries;

    public PackageBuilder() {
        def = new PackageDefinitionImpl();
        platforms = new ArrayList<>();
        dependencies = new ArrayList<>();
        conflicts = new ArrayList<>();
        provides = new ArrayList<>();
        entries = new LinkedHashMap<>();
        installForms = new ArrayList<>();
        validationForms = new ArrayList<>();
        uninstallForms = new ArrayList<>();
    }

    public PackageBuilder name(String name) {
        def.setName(name);
        return this;
    }

    public PackageBuilder version(Version version) {
        def.setVersion(version);
        return this;
    }

    public PackageBuilder version(String version) {
        def.setVersion(new Version(version));
        return this;
    }

    public PackageBuilder type(String type) {
        def.setType(PackageType.getByValue(type));
        return this;
    }

    public PackageBuilder type(PackageType type) {
        def.setType(type);
        return this;
    }

    /**
     * @since 5.6
     */
    public PackageBuilder visibility(String visibility) {
        return visibility(PackageVisibility.valueOf(visibility));
    }

    /**
     * @since 5.6
     */
    public PackageBuilder visibility(PackageVisibility visibility) {
        try {
            def.getClass().getMethod("setVisibility", PackageVisibility.class);
            def.setVisibility(visibility);
        } catch (NoSuchMethodException e) {
            // Ignore visibility with old Connect Client versions
        }
        return this;
    }

    public PackageBuilder title(String title) {
        def.setTitle(title);
        return this;
    }

    public PackageBuilder description(String description) {
        def.setDescription(description);
        return this;
    }

    public PackageBuilder classifier(String classifier) {
        def.setClassifier(classifier);
        return this;
    }

    public PackageBuilder vendor(String vendor) {
        def.setVendor(vendor);
        return this;
    }

    public PackageBuilder homePage(String homePage) {
        def.setHomePage(homePage);
        return this;
    }

    public PackageBuilder installer(TaskDefinition task) {
        def.setInstaller(task);
        return this;
    }

    public PackageBuilder installer(String type, boolean restart) {
        def.setInstaller(new TaskDefinitionImpl(type, restart));
        return this;
    }

    public PackageBuilder uninstaller(TaskDefinition task) {
        def.setUninstaller(task);
        return this;
    }

    public PackageBuilder uninstaller(String type, boolean restart) {
        def.setUninstaller(new TaskDefinitionImpl(type, restart));
        return this;
    }

    public PackageBuilder validationState(NuxeoValidationState validationState) {
        try {
            def.getClass().getMethod("setValidationState", NuxeoValidationState.class);
            def.setValidationState(validationState);
        } catch (NoSuchMethodException e) {
            // Ignore setValidationState with old Connect Client versions
        }
        return this;
    }

    public PackageBuilder productionState(ProductionState productionState) {
        try {
            def.getClass().getMethod("setProductionState", ProductionState.class);
            def.setProductionState(productionState);
        } catch (NoSuchMethodException e) {
            // Ignore setProductionState with old Connect Client versions
        }
        return this;
    }

    public PackageBuilder supported(boolean supported) {
        try {
            def.getClass().getMethod("setSupported", boolean.class);
            def.setSupported(supported);
        } catch (NoSuchMethodException e) {
            // Ignore setSupported with old Connect Client versions
        }
        return this;
    }

    public PackageBuilder hotReloadSupport(boolean hotReloadSupport) {
        try {
            def.getClass().getMethod("setHotReloadSupport", boolean.class);
            def.setHotReloadSupport(hotReloadSupport);
        } catch (NoSuchMethodException e) {
            // Ignore setHotReloadSupport with old Connect Client versions
        }
        return this;
    }

    public PackageBuilder requireTermsAndConditionsAcceptance(boolean requireTermsAndConditionsAcceptance) {
        try {
            def.getClass().getMethod("setRequireTermsAndConditionsAcceptance", boolean.class);
            def.setRequireTermsAndConditionsAcceptance(requireTermsAndConditionsAcceptance);
        } catch (NoSuchMethodException e) {
            // Ignore setRequireTermsAndConditionsAcceptance with old Connect
            // Client versions
        }
        return this;
    }

    public PackageBuilder validator(String validator) {
        def.setValidator(validator);
        return this;
    }

    public PackageBuilder platform(String platform) {
        platforms.add(platform);
        return this;
    }

    public PackageBuilder dependency(String expr) {
        dependencies.add(new PackageDependency(expr));
        return this;
    }

    public PackageBuilder conflict(String expr) {
        conflicts.add(new PackageDependency(expr));
        return this;
    }

    public PackageBuilder provide(String expr) {
        provides.add(new PackageDependency(expr));
        return this;
    }

    public PackageBuilder addInstallForm(FormDefinition form) {
        installForms.add(form);
        return this;
    }

    public PackageBuilder addUninstallForm(FormDefinition form) {
        uninstallForms.add(form);
        return this;
    }

    public PackageBuilder addValidationForm(FormDefinition form) {
        validationForms.add(form);
        return this;
    }

    public PackageBuilder addLicense(String content) {
        return addLicense(new ByteArrayInputStream(content.getBytes()));
    }

    public PackageBuilder addLicense(InputStream in) {
        return addEntry(LocalPackage.LICENSE, in);
    }

    public PackageBuilder addInstallScript(String content) {
        return addInstallScript(new ByteArrayInputStream(content.getBytes()));
    }

    public PackageBuilder addInstallScript(InputStream in) {
        return addEntry(LocalPackage.INSTALL, in);
    }

    public PackageBuilder addUninstallScript(String content) {
        return addUninstallScript(new ByteArrayInputStream(content.getBytes()));
    }

    public PackageBuilder addUninstallScript(InputStream in) {
        return addEntry(LocalPackage.UNINSTALL, in);
    }

    public PackageBuilder addTermsAndConditions(String content) {
        return addTermsAndConditions(new ByteArrayInputStream(content.getBytes()));
    }

    public PackageBuilder addTermsAndConditions(InputStream in) {
        return addEntry(LocalPackage.TERMSANDCONDITIONS, in);
    }

    /**
     * The entry content will be copied into the zip at build time and the given input stream will be closed. (event if
     * an exception occurs) - so you don't need to handle stream closing.
     */
    public PackageBuilder addEntry(String path, InputStream in) {
        entries.put(path, in);
        return this;
    }

    public String buildManifest() {
        if (!platforms.isEmpty()) {
            def.setTargetPlatforms(platforms.toArray(new String[platforms.size()]));
        }
        if (!dependencies.isEmpty()) {
            def.setDependencies(dependencies.toArray(new PackageDependency[dependencies.size()]));
        }
        if (!conflicts.isEmpty()) {
            def.setConflicts(conflicts.toArray(new PackageDependency[conflicts.size()]));
        }
        if (!provides.isEmpty()) {
            def.setProvides(provides.toArray(new PackageDependency[provides.size()]));
        }
        return new XmlSerializer().toXML(def);
    }

    public File build() throws IOException {
        try {
            String mf = buildManifest();
            File file = Framework.createTempFile(def.getId(), ".zip");
            Framework.trackFile(file, file);
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(file));
            try {
                ZipEntry entry = new ZipEntry(LocalPackage.MANIFEST);
                zout.putNextEntry(entry);
                zout.write(mf.getBytes());
                zout.closeEntry();
                for (Map.Entry<String, InputStream> stream : entries.entrySet()) {
                    entry = new ZipEntry(stream.getKey());
                    zout.putNextEntry(entry);
                    IOUtils.copy(stream.getValue(), zout);
                    zout.closeEntry();
                }
                if (!installForms.isEmpty()) {
                    addForms(installForms, LocalPackage.INSTALL_FORMS, zout);
                }
                if (!uninstallForms.isEmpty()) {
                    addForms(uninstallForms, LocalPackage.UNINSTALL_FORMS, zout);
                }
                if (!validationForms.isEmpty()) {
                    addForms(validationForms, LocalPackage.VALIDATION_FORMS, zout);
                }
            } finally {
                zout.close();
            }
            return file;
        } finally { // close streams
            for (InputStream in : entries.values()) {
                IOUtils.closeQuietly(in);
            }
        }
    }

    protected void addForms(List<FormDefinition> formDefs, String path, ZipOutputStream zout) throws IOException {
        int i = 0;
        FormsDefinition forms = new FormsDefinition();
        FormDefinition[] ar = new FormDefinition[formDefs.size()];
        for (FormDefinition form : formDefs) {
            ar[i++] = form;
        }
        forms.setForms(ar);
        String xml = new XmlSerializer().toXML(forms);
        ZipEntry entry = new ZipEntry(path);
        zout.putNextEntry(entry);
        IOUtils.copy(new ByteArrayInputStream(xml.getBytes()), zout);
        zout.closeEntry();
    }

}
