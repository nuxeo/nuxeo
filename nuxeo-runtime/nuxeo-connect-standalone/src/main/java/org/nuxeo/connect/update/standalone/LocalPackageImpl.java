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
package org.nuxeo.connect.update.standalone;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.Validator;
import org.nuxeo.connect.update.Version;
import org.nuxeo.connect.update.model.Form;
import org.nuxeo.connect.update.model.TaskDefinition;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.xml.FormsDefinition;
import org.nuxeo.connect.update.xml.PackageDefinitionImpl;
import org.nuxeo.connect.update.xml.TaskDefinitionImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LocalPackageImpl implements LocalPackage {

    protected String id;

    protected PackageState state = PackageState.UNKNOWN;

    protected LocalPackageData data;

    protected PackageDefinitionImpl def;

    private PackageUpdateService service;

    /**
     * @deprecated Since 5.8. Use {@link #LocalPackageImpl(File, PackageState, PackageUpdateService)} instead.
     */
    @Deprecated
    public LocalPackageImpl(File file, int state, PackageUpdateService pus) throws PackageException {
        this(null, file, state, pus);
    }

    /**
     * @deprecated Since 5.8. Use {@link #LocalPackageImpl(ClassLoader, File, PackageState, PackageUpdateService)}
     *             instead.
     */
    @Deprecated
    public LocalPackageImpl(ClassLoader parent, File file, int state, PackageUpdateService pus)
            throws PackageException {
        this(parent, file, PackageState.getByValue(state), pus);
    }

    /**
     * @since 5.7
     */
    public LocalPackageImpl(File file, PackageState state, PackageUpdateService pus) throws PackageException {
        this(null, file, state, pus);
    }

    /**
     * @since 5.8
     */
    public LocalPackageImpl(ClassLoader parent, File file, PackageState state, PackageUpdateService pus)
            throws PackageException {
        this.state = state;
        service = pus;
        XMap xmap = StandaloneUpdateService.getXmap();
        if (xmap == null) { // for tests
            xmap = StandaloneUpdateService.createXmap();
        }
        try {
            data = new LocalPackageData(parent, file);
            InputStream in = new FileInputStream(data.getManifest());
            def = (PackageDefinitionImpl) xmap.load(in);
        } catch (FileNotFoundException e) {
            throw new PackageException("Invalid package - no package.xml file found in package " + file.getName());
        } catch (IOException e) {
            throw new PackageException("Failed to load package.xml descriptor for package " + file.getName(), e);
        }
        id = def.getId();
    }

    @Deprecated
    @Override
    public void setState(int state) {
        this.state = PackageState.getByValue(state);
    }

    @Override
    public void setState(PackageState state) {
        this.state = state;
    }

    @Override
    public LocalPackageData getData() {
        return data;
    }

    @Override
    public File getInstallFile() {
        return data.getEntry(LocalPackage.INSTALL);
    }

    @Override
    public File getUninstallFile() {
        return data.getEntry(LocalPackage.UNINSTALL);
    }

    @Override
    public String getLicenseType() {
        return def.getLicenseType();
    }

    @Override
    public String getLicenseUrl() {
        return def.getLicenseUrl();
    }

    @Override
    public String getLicenseContent() throws PackageException {
        File file = data.getEntry(LocalPackage.LICENSE);
        if (file.isFile()) {
            try {
                return FileUtils.readFileToString(file, UTF_8);
            } catch (IOException e) {
                throw new PackageException("Failed to read license.txt file for package: " + getId());
            }
        }
        return null;
    }

    @Override
    public String getClassifier() {
        return def.getClassifier();
    }

    @Override
    public String getDescription() {
        return def.getDescription();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return def.getName();
    }

    @Override
    public String getTitle() {
        return def.getTitle();
    }

    @Deprecated
    @Override
    public int getState() {
        return state.getValue();
    }

    @Override
    public PackageState getPackageState() {
        return state;
    }

    @Override
    public String[] getTargetPlatforms() {
        return def.getTargetPlatforms();
    }

    @Override
    public String getTargetPlatformRange() {
        return def.getTargetPlatformRange();
    }

    @Override
    public PackageDependency[] getDependencies() {
        return def.getDependencies();
    }

    @Override
    public PackageDependency[] getOptionalDependencies() {
        return def.getOptionalDependencies();
    }

    @Override
    public PackageDependency[] getConflicts() {
        return def.getConflicts();
    }

    @Override
    public PackageDependency[] getProvides() {
        return def.getProvides();
    }

    @Override
    public PackageType getType() {
        return def.getType();
    }

    @Override
    public Version getVersion() {
        return def.getVersion();
    }

    @Override
    public String getVendor() {
        return def.getVendor();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public Task getInstallTask() throws PackageException {
        if (def.getInstaller() == null) {
            def.setInstaller(new TaskDefinitionImpl(service.getDefaultInstallTaskType(), false));
        } else if (def.getInstaller().getType() == null) {
            def.getInstaller().setType(service.getDefaultInstallTaskType());
        }
        return getTask(def.getInstaller());
    }

    @Override
    public Task getUninstallTask() throws PackageException {
        if (def.getUninstaller() == null) {
            def.setUninstaller(new TaskDefinitionImpl(service.getDefaultUninstallTaskType(), false));
        } else if (def.getUninstaller().getType() == null) {
            def.getUninstaller().setType(service.getDefaultUninstallTaskType());
        }
        return getTask(def.getUninstaller());
    }

    protected Task getTask(TaskDefinition tdef) throws PackageException {
        Task task;
        try {
            task = (Task) data.loadClass(tdef.getType())
                              .getConstructor(PackageUpdateService.class)
                              .newInstance(service);
        } catch (ReflectiveOperationException e) {
            throw new PackageException(
                    "Could not instantiate custom task " + tdef.getType() + " for package " + getId(), e);
        }
        task.initialize(this, tdef.getRequireRestart());
        return task;
    }

    @Override
    public Validator getValidator() throws PackageException {
        if (def.getValidator() != null) {
            try {
                return (Validator) data.loadClass(def.getValidator()).getConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new PackageException(
                        "Could not instantiate custom validator " + def.getValidator() + " for package " + getId(), e);
            }
        }
        return null;
    }

    public Form[] getForms(String path) throws PackageException {
        File file = data.getEntry(path);
        if (file.isFile()) {
            try (FileInputStream in = new FileInputStream(file)) {
                FormsDefinition forms = (FormsDefinition) StandaloneUpdateService.getXmap().load(in);
                return forms.getForms();
            } catch (IOException e) {
                throw new PackageException("Failed to load forms file: " + file);
            }
        }
        return null;
    }

    @Override
    public Form[] getValidationForms() throws PackageException {
        return getForms(LocalPackage.VALIDATION_FORMS);
    }

    @Override
    public Form[] getInstallForms() throws PackageException {
        return getForms(LocalPackage.INSTALL_FORMS);
    }

    @Override
    public Form[] getUninstallForms() throws PackageException {
        return getForms(LocalPackage.UNINSTALL_FORMS);
    }

    @Override
    public String getTermsAndConditionsContent() throws PackageException {
        File file = data.getEntry(LocalPackage.TERMSANDCONDITIONS);
        if (file.isFile()) {
            try {
                return FileUtils.readFileToString(file, UTF_8);
            } catch (IOException e) {
                throw new PackageException("Failed to read license.txt file for package: " + getId());
            }
        }
        return null;
    }

    @Override
    public boolean requireTermsAndConditionsAcceptance() {
        return def.requireTermsAndConditionsAcceptance();
    }

    @Override
    public boolean supportsHotReload() {
        return def.supportsHotReload();
    }

    @Override
    public String toString() {
        return getId();
    }

}
