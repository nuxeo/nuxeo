/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.connect.update;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.XmlWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class PackageDef {

    private File pkgFile;

    protected LocalPackage pkg;

    protected PackageBuilder builder;

    protected boolean allowDowngrade = false;

    protected boolean upgradeOnly = false;

    public PackageDef(String name, String version) throws Exception {
        this(name, version, PackageType.ADDON);
    }

    public PackageDef(String name, String version, PackageType type)
            throws Exception {
        this(name, version, type, "dm-" + version);
    }

    public PackageDef(String name, String version, PackageType type,
            String targetVersion) {
        builder = new PackageBuilder();
        builder.name(name).version(version).type(type);
        builder.platform(targetVersion);
        builder.title("Test Package: " + name);
        builder.description("A test package");
        builder.classifier("Open Source");
        builder.vendor("Nuxeo");
        builder.addLicense("My test license. All rights reserved.");
    }

    public void setUpgradeOnly(boolean upgradeOnly) {
        this.upgradeOnly = upgradeOnly;
    }

    public void setAllowDowngrade(boolean allowDowngrade) {
        this.allowDowngrade = allowDowngrade;
    }

    public File getPackageFile() throws Exception {
        if (pkgFile == null) {
            updatePackage(builder);
            XmlWriter writer = new XmlWriter();
            writer.start("install");
            writer.startContent();
            writeInstallCommands(writer);
            writer.end("install");
            builder.addInstallScript(writer.toString());
            pkgFile = builder.build();
            pkgFile.deleteOnExit();
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

    public LocalPackage download(PackageUpdateService service) throws Exception {
        if (pkg != null) {
            return pkg;
        }
        pkg = service.addPackage(getPackageFile());
        return pkg;
    }

    public void install(PackageUpdateService service) throws Exception {
        if (pkg == null) {
            download(service);
        }
        Task task = pkg.getInstallTask();
        ValidationStatus status = task.validate();
        if (status.hasErrors()) {
            throw new PackageValidationException(status);
        }
        try {
            task.run(new HashMap<String, String>());
        } catch (Throwable t) {
            task.rollback();
        }
    }

    public void uninstall(PackageUpdateService service) throws Exception {
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
        } catch (Throwable t) {
            task.rollback();
        }
    }

    protected abstract void updatePackage(PackageBuilder builder)
            throws Exception;

    protected abstract void writeInstallCommands(XmlWriter writer)
            throws Exception;

    public void addFile(String name, URL url) throws Exception {
        builder.addEntry(name, url.openStream());
    }

    public void addFile(String name, String content) throws Exception {
        builder.addEntry(name, new ByteArrayInputStream(content.getBytes()));
    }

}
