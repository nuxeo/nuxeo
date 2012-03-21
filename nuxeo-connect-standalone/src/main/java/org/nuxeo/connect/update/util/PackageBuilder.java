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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.NuxeoValidationState;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.ProductionState;
import org.nuxeo.connect.update.Version;
import org.nuxeo.connect.update.model.TaskDefinition;
import org.nuxeo.connect.update.standalone.StandaloneUpdateService;
import org.nuxeo.connect.update.xml.FormDefinition;
import org.nuxeo.connect.update.xml.FormsDefinition;
import org.nuxeo.connect.update.xml.PackageDefinitionImpl;
import org.nuxeo.connect.update.xml.TaskDefinitionImpl;
import org.nuxeo.connect.update.xml.XmlSerializer;

/**
 * Build an XML representation of a package.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PackageBuilder {

    protected final PackageDefinitionImpl def;

    protected final List<FormDefinition> installForms;

    protected final List<FormDefinition> uninstallForms;

    protected final List<FormDefinition> validationForms;

    protected final List<String> platforms;

    protected final List<PackageDependency> dependencies;

    protected final LinkedHashMap<String, InputStream> entries;

    public PackageBuilder() {
        def = new PackageDefinitionImpl();
        platforms = new ArrayList<String>();
        dependencies = new ArrayList<PackageDependency>();
        entries = new LinkedHashMap<String, InputStream>();
        installForms = new ArrayList<FormDefinition>();
        validationForms = new ArrayList<FormDefinition>();
        uninstallForms = new ArrayList<FormDefinition>();
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
        def.setValidationState(validationState);
        return this;
    }

    public PackageBuilder productionState(ProductionState productionState) {
        def.setProductionState(productionState);
        return this;
    }

    public PackageBuilder supported(boolean supported) {
        def.setSupported(supported);
        return this;
    }

    public PackageBuilder hotReloadSupport(boolean hotReloadSupport) {
        def.setHotReloadSupport(hotReloadSupport);
        return this;
    }

    public PackageBuilder requireTermsAndConditionsAcceptance(
            boolean requireTermsAndConditionsAcceptance) {
        def.setRequireTermsAndConditionsAcceptance(requireTermsAndConditionsAcceptance);
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
        return addTermsAndConditions(new ByteArrayInputStream(
                content.getBytes()));
    }

    public PackageBuilder addTermsAndConditions(InputStream in) {
        return addEntry(LocalPackage.TERMSANDCONDITIONS, in);
    }

    /**
     * The entry content will be copied into the zip at build time and the given
     * input stream will be closed. (event if an exception occurs) - so you
     * don't need to handle stream closing.
     */
    public PackageBuilder addEntry(String path, InputStream in) {
        entries.put(path, in);
        return this;
    }

    public String buildManifest() {
        if (!platforms.isEmpty()) {
            def.setPlatforms(platforms.toArray(new String[platforms.size()]));
        }
        if (!dependencies.isEmpty()) {
            def.setDependencies(dependencies.toArray(new PackageDependency[dependencies.size()]));
        }
        return new XmlSerializer().toXML(def);
    }

    public File build() throws IOException {
        try {
            String mf = buildManifest();
            File file = File.createTempFile(def.getId(), ".zip");
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(
                    file));
            try {
                ZipEntry entry = new ZipEntry(LocalPackage.MANIFEST);
                zout.putNextEntry(entry);
                zout.write(mf.getBytes());
                zout.closeEntry();
                for (Map.Entry<String, InputStream> stream : entries.entrySet()) {
                    entry = new ZipEntry(stream.getKey());
                    zout.putNextEntry(entry);
                    FileUtils.copy(stream.getValue(), zout);
                    zout.closeEntry();
                }
                if (!installForms.isEmpty()) {
                    addForms(installForms, LocalPackage.INSTALL_FORMS, zout);
                }
                if (!uninstallForms.isEmpty()) {
                    addForms(uninstallForms, LocalPackage.UNINSTALL_FORMS, zout);
                }
                if (!validationForms.isEmpty()) {
                    addForms(validationForms, LocalPackage.VALIDATION_FORMS,
                            zout);
                }
            } finally {
                zout.close();
            }
            return file;
        } finally { // close streams
            for (InputStream in : entries.values()) {
                try {
                    in.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    protected void addForms(List<FormDefinition> formDefs, String path,
            ZipOutputStream zout) throws IOException {
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
        FileUtils.copy(new ByteArrayInputStream(xml.getBytes()), zout);
        zout.closeEntry();
    }

    // TODO NXP-9086: make it a unit test
    public static void main(String[] args) throws Exception {
        PackageBuilder builder = new PackageBuilder();
        builder.name("nuxeo-automation").version("5.3.2").type(
                PackageType.ADDON);
        builder.title("Nuxeo Automation").description(
                "The automation framework");
        builder.platform("dm-5.3.2");
        builder.dependency("nuxeo-core:5.3.2");
        builder.classifier("OpenSource");
        builder.installer("MyInstaller", true);
        builder.addLicense("My License");

        String xml = builder.buildManifest();
        System.out.println(xml);

        XMap xmap = StandaloneUpdateService.createXmap();
        PackageDefinitionImpl pdef = (PackageDefinitionImpl) xmap.load(new ByteArrayInputStream(
                xml.getBytes()));
        System.out.println(pdef);

        File file = builder.build();
        System.out.println(file);
        file.delete();
    }
}
