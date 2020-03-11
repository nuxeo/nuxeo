/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.update;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.XmlWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class PackageDef {

    private static final Log log = LogFactory.getLog(PackageDef.class);

    private File pkgFile;

    protected LocalPackage pkg;

    protected PackageBuilder builder;

    protected boolean allowDowngrade = false;

    protected boolean upgradeOnly = false;

    private PackageUpdateService service;

    public PackageDef(String name, String version, PackageUpdateService service) {
        this(name, version, PackageType.ADDON, service);
    }

    public PackageDef(String name, String version, PackageType type, PackageUpdateService service) {
        this(name, version, type, "dm-" + version, service);
    }

    public PackageDef(String name, String version, PackageType type, String targetVersion, PackageUpdateService service) {
        builder = new PackageBuilder();
        builder.name(name).version(version).type(type);
        builder.platform(targetVersion);
        builder.title("Test Package: " + name);
        builder.description("A test package");
        builder.classifier("Open Source");
        builder.vendor("Nuxeo");
        builder.addLicense("My test license. All rights reserved.");
        this.service = service;
    }

    public void setUpgradeOnly(boolean upgradeOnly) {
        this.upgradeOnly = upgradeOnly;
    }

    public void setAllowDowngrade(boolean allowDowngrade) {
        this.allowDowngrade = allowDowngrade;
    }

    public File getPackageFile() throws Exception {
        if (pkgFile == null) {
            updatePackage();
            XmlWriter writer = new XmlWriter();
            writer.start("install");
            writer.startContent();
            writeInstallCommands(writer);
            writer.end("install");
            builder.addInstallScript(writer.toString());
            pkgFile = builder.build();
        }
        return pkgFile;
    }

    public String getId() {
        if (pkg == null) {
            throw new IllegalStateException("package was not installed");
        }
        return pkg.getId();
    }

    public LocalPackage getPackage() {
        return pkg;
    }

    public LocalPackage download() throws Exception {
        if (pkg != null) {
            return pkg;
        }
        pkg = service.addPackage(getPackageFile());
        return pkg;
    }

    public void install() throws Exception {
        if (pkg == null) {
            download();
        }
        Task task = pkg.getInstallTask();
        ValidationStatus status = task.validate();
        if (status.hasErrors()) {
            throw new PackageValidationException(status);
        }
        try {
            task.run(new HashMap<String, String>());
        } catch (PackageException e) {
            log.error(e, e);
            task.rollback();
        }
    }

    public void uninstall() throws Exception {
        if (pkg == null) {
            throw new IllegalStateException("Package was not installed");
        }
        Task task = pkg.getUninstallTask();
        ValidationStatus status = task.validate();
        if (status.hasErrors()) {
            throw new PackageValidationException(status);
        }
        try {
            task.run(new HashMap<String, String>());
        } catch (PackageException e) {
            log.error(e, e);
            task.rollback();
        }
    }

    protected abstract void updatePackage() throws Exception;

    protected abstract void writeInstallCommands(XmlWriter writer) throws Exception;

    public void addFile(String name, URL url) throws Exception {
        builder.addEntry(name, url.openStream());
    }

    public void addFile(String name, String content) throws Exception {
        builder.addEntry(name, new ByteArrayInputStream(content.getBytes()));
    }

}
