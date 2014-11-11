/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu, jcarsique
 */
package org.nuxeo.connect.update.standalone;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.XValueFactory;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.Version;
import org.nuxeo.connect.update.model.PackageDefinition;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.standalone.InstallTask;
import org.nuxeo.connect.update.task.standalone.UninstallTask;
import org.nuxeo.connect.update.task.standalone.commands.Append;
import org.nuxeo.connect.update.task.standalone.commands.Config;
import org.nuxeo.connect.update.task.standalone.commands.Copy;
import org.nuxeo.connect.update.task.standalone.commands.Delete;
import org.nuxeo.connect.update.task.standalone.commands.DeployConfigPlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.DeployPlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.FlushCoreCachePlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.FlushJaasCachePlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.FlushPlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.InstallPlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.LoadJarPlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.ParameterizedCopy;
import org.nuxeo.connect.update.task.standalone.commands.ReloadPropertiesPlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.UnAppend;
import org.nuxeo.connect.update.task.standalone.commands.UndeployConfigPlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.UndeployPlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.UninstallPlaceholder;
import org.nuxeo.connect.update.task.standalone.commands.UnloadJarPlaceholder;
import org.nuxeo.connect.update.task.update.Rollback;
import org.nuxeo.connect.update.task.update.Update;
import org.nuxeo.connect.update.xml.FormsDefinition;
import org.nuxeo.connect.update.xml.PackageDefinitionImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StandaloneUpdateService implements PackageUpdateService {

    protected static XMap xmap;

    protected PackagePersistence persistence;

    protected Map<String, Class<? extends Command>> commands;

    public static XMap getXmap() {
        return xmap;
    }

    public StandaloneUpdateService(Environment env) throws IOException {
        // TODO NXP-9086: Add some checks on the environment
        Environment.setDefault(env);
        persistence = new PackagePersistence(this);
        commands = new HashMap<String, Class<? extends Command>>();
    }

    @Override
    public File getDataDir() {
        return persistence.getRoot();
    }

    public PackagePersistence getPersistence() {
        return persistence;
    }

    @Override
    public LocalPackage addPackage(File file) throws PackageException {
        return persistence.addPackage(file);
    }

    @Override
    public void removePackage(String id) throws PackageException {
        persistence.removePackage(id);
    }

    @Override
    public LocalPackage getPackage(String id) throws PackageException {
        return persistence.getPackage(id);
    }

    @Override
    public List<LocalPackage> getPackages() throws PackageException {
        return persistence.getPackages();
    }

    public static XMap createXmap() {
        @SuppressWarnings("hiding")
        XMap xmap = new XMap();
        xmap.setValueFactory(PackageType.class, new XValueFactory<PackageType>() {
            @Override
            public String serialize(Context arg0, PackageType arg1) {
                return arg1.getValue();
            }

            @Override
            public PackageType deserialize(Context arg0, String arg1) {
                return PackageType.getByValue(arg1);
            }


			@Override
			public Class<PackageType> getType() {
				return PackageType.class;
			}
        });
        xmap.setValueFactory(new XValueFactory<Version>() {
            @Override
            public String serialize(Context arg0, Version arg1) {
                return arg1.toString();
            }

            @Override
            public Version deserialize(Context arg0, String arg1) {
                return new Version(arg1);
            }

			@Override
			public Class<Version> getType() {
				return Version.class;
			}
        });
        xmap.setValueFactory(new XValueFactory<PackageDependency>() {

            @Override
            public String serialize(Context arg0, PackageDependency arg1) {
                return arg1.toString();
            }

            @Override
            public PackageDependency deserialize(Context arg0, String arg1) {
                return new PackageDependency(arg1);
            }

			@Override
			public Class<PackageDependency> getType() {
				return PackageDependency.class;
			}
        });
        xmap.register(PackageDefinitionImpl.class);
        xmap.register(FormsDefinition.class);
        return xmap;
    }

    @Override
    public void initialize() throws PackageException {
        xmap = createXmap();
        addCommands();
        startInstalledPackages();
    }

    protected void addCommands() {
        addCommand(Copy.ID, Copy.class);
        addCommand(Append.ID, Append.class);
        addCommand(UnAppend.ID, UnAppend.class);
        addCommand(ParameterizedCopy.ID, ParameterizedCopy.class);
        addCommand(Delete.ID, Delete.class);
        addCommand(InstallPlaceholder.ID, InstallPlaceholder.class);
        addCommand(UninstallPlaceholder.ID, UninstallPlaceholder.class);
        addCommand(FlushCoreCachePlaceholder.ID,
                FlushCoreCachePlaceholder.class);
        addCommand(FlushJaasCachePlaceholder.ID,
                FlushJaasCachePlaceholder.class);
        addCommand(FlushPlaceholder.ID, FlushPlaceholder.class);
        addCommand(ReloadPropertiesPlaceholder.ID,
                ReloadPropertiesPlaceholder.class);
        addCommand(DeployPlaceholder.ID, DeployPlaceholder.class);
        addCommand(UndeployPlaceholder.ID, UndeployPlaceholder.class);
        addCommand(DeployConfigPlaceholder.ID, DeployConfigPlaceholder.class);
        addCommand(UndeployConfigPlaceholder.ID,
                UndeployConfigPlaceholder.class);
        addCommand(LoadJarPlaceholder.ID, LoadJarPlaceholder.class);
        addCommand(UnloadJarPlaceholder.ID, UnloadJarPlaceholder.class);
        addCommand(Config.ID, Config.class);
        addCommand(Update.ID, Update.class);
        addCommand(Rollback.ID, Rollback.class);
    }

    @Deprecated
    @Override
    public void setPackageState(LocalPackage pkg, int state)
            throws PackageException {
        persistence.updateState(pkg.getId(), state);
        pkg.setState(state);
    }

    @Override
    public void setPackageState(LocalPackage pkg, PackageState state)
            throws PackageException {
        persistence.updateState(pkg.getId(), state);
        pkg.setState(state);
    }

    @Override
    public void shutdown() throws PackageException {
        xmap = null;
    }

    @Override
    public Command getCommand(String id) throws PackageException {
        Class<? extends Command> type = commands.get(id);
        if (type != null) {
            try {
                return type.getConstructor().newInstance();
            } catch (Exception e) {
                throw new PackageException("Failed to load command " + id, e);
            }
        }
        return null;
    }

    @Override
    public String getDefaultInstallTaskType() {
        return InstallTask.class.getName();
    }

    @Override
    public String getDefaultUninstallTaskType() {
        return UninstallTask.class.getName();
    }

    public void addCommand(String id, Class<? extends Command> cmd) {
        commands.put(id, cmd);
    }

    public void removeCommand(String id) {
        commands.remove(id);
    }

    @Override
    public LocalPackage getActivePackage(String name) throws PackageException {
        return persistence.getActivePackage(name);
    }

    @Override
    public PackageDefinition loadPackageFromZip(File file)
            throws PackageException {
        ZipFile zip = null;
        try {
            zip = new ZipFile(file);
            ZipEntry mfEntry = zip.getEntry(LocalPackage.MANIFEST);
            InputStream mfStream = zip.getInputStream(mfEntry);
            return loadPackage(mfStream);
        } catch (PackageException e) {
            throw e;
        } catch (Exception e) {
            throw new PackageException(
                    "Failed to load package definition from zip file: " + file,
                    e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    throw new PackageException("Failed to close package zip: "
                            + file, e);
                }
            }
        }
    }

    @Override
    public PackageDefinition loadPackage(File file) throws PackageException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return loadPackage(in);
        } catch (PackageException e) {
            throw e;
        } catch (Exception e) {
            throw new PackageException(
                    "Failed to load XML package definition from file: " + file,
                    e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                throw new PackageException("Failed to close input stream for "
                        + file, e);
            }
        }
    }

    @Override
    public PackageDefinition loadPackage(InputStream in)
            throws PackageException {
        try {
            return (PackageDefinition) xmap.load(in);
        } catch (Exception e) {
            throw new PackageException(
                    "Failed to parse XML package definition", e);
        }
    }

    protected void startInstalledPackages() throws PackageException {
        for (Entry<String, PackageState> entry : persistence.getStates().entrySet()) {
            if (entry.getValue() == PackageState.INSTALLED) {
                persistence.updateState(entry.getKey(), PackageState.STARTED);
            }
        }
    }

    @Override
    public void reset() throws PackageException {
        persistence.reset();
    }

    @Override
    public void restart() throws PackageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStarted(String pkgId) {
        return persistence.getState(pkgId) == PackageState.STARTED;
    }

    @Override
    public File getRegistry() {
        return new File(getDataDir(), "registry.xml");
    }

    @Override
    public File getBackupDir() {
        return new File(getDataDir(), "backup");
    }

    @Override
    public FileTime getInstallDate(String id) {
        return getPersistence().getInstallDate(id);
    }

}
