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
package org.nuxeo.connect.update.task.update;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.FileVersion;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.update.JarUtils.Match;

/**
 * Manage jar versions update.
 * <p>
 * To manipulate the jar version registry you need to create a new instance of
 * this class.
 * <p>
 * If you want to modify the registry then you may want to synchronize the
 * entire update process. This is how is done in the Task run method.
 * <p>
 * Only reading the registry is thread safe.
 * <p>
 * TODO backup md5 are not really used since we rely on versions - we can remove
 * md5
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class UpdateManager {

    private static final Log log = LogFactory.getLog(UpdateManager.class);

    protected Task task;

    protected Map<String, Entry> registry;

    protected File file;

    protected File backupRoot;

    protected File serverRoot;

    public UpdateManager(File serverRoot, File regFile) {
        this.file = regFile;
        this.backupRoot = new File(file.getParentFile(), "backup");
        backupRoot.mkdirs();
        this.serverRoot = serverRoot;
    }

    @SuppressWarnings("hiding")
    public UpdateOptions createUpdateOptions(String pkgId, File file,
            File targetDir) {
        return UpdateOptions.newInstance(pkgId, file, targetDir);
    }

    public RollbackOptions createRollbackOptions(String pkgId, String key,
            String version) {
        return new RollbackOptions(pkgId, key, version);
    }

    public RollbackOptions createRollbackOptions(UpdateOptions opt, String key) {
        RollbackOptions r = new RollbackOptions(opt.pkgId, key, opt.version);
        r.setDeleteOnExit(opt.deleteOnExit);
        return r;
    }

    public File getServerRoot() {
        return serverRoot;
    }

    public File getBackupRoot() {
        return backupRoot;
    }

    public Task getTask() {
        return task;
    }

    public Map<String, Entry> getRegistry() {
        return registry;
    }

    public synchronized void load() throws PackageException {
        if (!file.isFile()) {
            registry = new HashMap<String, Entry>();
            return;
        }
        try {
            registry = RegistrySerializer.load(file);
        } catch (PackageException e) {
            throw e;
        } catch (IOException e) {
            throw new PackageException(
                    "IOException while trying to load the registry", e);
        }
    }

    public synchronized void store() throws PackageException {
        try {
            RegistrySerializer.store(registry, file);
        } catch (IOException e) {
            throw new PackageException(
                    "IOException while trying to write the registry", e);
        }
    }

    public String getVersionPath(UpdateOptions opt) {
        return getServerRelativePath(opt.getTargetFile());
    }

    public String getKey(UpdateOptions opt) {
        String key = getServerRelativePath(opt.getTargetDir());
        if (key.endsWith(File.separator)) {
            key = key.concat(opt.nameWithoutVersion);
        } else {
            key = key.concat(File.separator).concat(opt.nameWithoutVersion);
        }
        return key;
    }

    /**
     * @param opt
     * @throws Exception, VersionAlreadyExistException
     */
    public RollbackOptions update(UpdateOptions opt) throws PackageException {
        String key = getKey(opt);
        Entry entry = registry.get(key);
        if (entry == null) {
            entry = createEntry(key);
        }
        Version v = entry.getVersion(opt.version);
        if (v != null && !opt.isSnapshotVersion()) {
            // for snapshot version we will continue
            // to overwrite previous version
            v.addPackage(opt.getPackageId());
            return createRollbackOptions(opt, key);
        }
        if (v == null) {
            v = entry.addVersion(new Version(opt.getVersion()));
            v.setPath(getVersionPath(opt));
        }
        backupFile(opt.getFile(), v.getPath());
        v.addPackage(opt.getPackageId());
        // JAR update will be performed only if needed
        doUpdate(key, v, opt);
        return createRollbackOptions(opt, key);
    }

    /**
     * Perform a rollback.
     *
     * TODO the deleteOnExit is inherited from the current rollback command ...
     * may be it should be read from the version that is rollbacked.
     * (deleteOnExit should be an attribute of the entry not of the version)
     *
     * @param opt
     * @throws PackageException
     */
    public void rollback(RollbackOptions opt) throws PackageException {
        Entry entry = registry.get(opt.getKey());
        if (entry == null) {
            return;
        }
        Version v = entry.getVersion(opt.getVersion());
        if (v == null) {
            return;
        }
        // store current last version
        Version lastVersion = entry.getLastVersion();
        boolean removeBackup = false;

        v.removePackage(opt.getPackageId());
        if (!v.hasPackages()) {
            // remove this version
            entry.removeVersion(v);
            removeBackup = true;
        }

        Version versionToRollback = entry.getLastVersion();
        if (versionToRollback == null) {
            // no more versions - remove entry and rollback base version if any
            registry.remove(entry.getKey());
            rollbackBaseVersion(entry, opt);
        } else if (versionToRollback != lastVersion) {
            // we removed the current installed version so we need to rollback
            rollbackVersion(entry, versionToRollback, opt);
        } else {
            // handle jars that were blocked using allowDowngrade or onlyUpgrade
            Match<File> m = findInstalledJar(opt.getKey());
            if (m != null) {
                if (entry.getVersion(m.version) == null) {
                    // the current installed version is no more in registry
                    // should be the one we just removed
                    Version greatest = entry.getGreatestVersion();
                    if (greatest != null) {
                        // rollback to the greatest version
                        rollbackVersion(entry, greatest, opt);
                    }
                }
            }
        }

        if (removeBackup) {
            removeBackup(v.getPath());
        }

    }

    protected void rollbackBaseVersion(Entry entry, RollbackOptions opt)
            throws PackageException {
        Version base = entry.getBaseVersion();
        if (base != null) {
            rollbackVersion(entry, base, opt);
            removeBackup(base.getPath());
        } else {
            // simply remove the installed file if exists
            Match<File> m = JarUtils.findJar(serverRoot, entry.getKey());
            if (m != null) {
                if (opt.isDeleteOnExit()) {
                    m.object.deleteOnExit();
                } else {
                    m.object.delete();
                }
            }
        }
    }

    protected void rollbackVersion(Entry entry, Version version,
            RollbackOptions opt) throws PackageException {
        File versionFile = getBackup(version.getPath());
        if (!versionFile.isFile()) {
            log.error("Could not rollback version " + version.getPath()
                    + " since the backup file was not found");
            return;
        }
        Match<File> m = findInstalledJar(entry.getKey());
        File oldFile = m != null ? m.object : null;
        File targetFile = getTargetFile(version.getPath());
        deleteOldFile(targetFile, oldFile, opt.deleteOnExit);
        copy(versionFile, targetFile);
    }

    public String getServerRelativePath(File someFile) {
        String path = someFile.getAbsolutePath();
        String serverPath = serverRoot.getAbsolutePath();
        if (!serverPath.endsWith(File.separator)) {
            serverPath = serverPath.concat(File.separator);
        }
        if (path.startsWith(serverPath)) {
            return path.substring(serverPath.length());
        }
        return path;
    }

    /**
     * Create a new entry in the registry given the entry key. A base version
     * will be automatically created if needed.
     *
     * @param key
     * @throws Exception
     */
    public Entry createEntry(String key) throws PackageException {
        Entry entry = new Entry(key);
        Match<File> m = JarUtils.findJar(serverRoot, key);
        if (m != null) {
            String path = getServerRelativePath(m.object);
            Version base = new Version(m.version);
            base.setPath(path);
            entry.setBaseVersion(base);
            backupFile(m.object, path);
        }
        registry.put(key, entry);
        return entry;
    }

    /**
     * Backup the given file in the registry storage.
     *
     * @param fileToBackup
     * @param path
     */
    protected void backupFile(File fileToBackup, String path)
            throws PackageException {
        try {
            File dst = new File(backupRoot, path);
            copy(fileToBackup, dst);
            // String md5 = IOUtils.createMd5(dst);
            // FileUtils.writeFile(new
            // File(dst.getAbsolutePath().concat(".md5")),
            // md5);
        } catch (Exception e) {
            throw new PackageException("Failed to backup file: " + path, e);
        }
    }

    /**
     * Remove the backup given its path. This is also removing the md5.
     *
     * @param path
     */
    protected void removeBackup(String path) {
        File dst = new File(backupRoot, path);
        if (!dst.delete()) {
            dst.deleteOnExit();
        }
        // new File(dst.getAbsolutePath().concat(".md5")).delete();
    }

    protected File getBackup(String path) {
        return new File(backupRoot, path);
    }

    // protected String getBackupMd5(String path) {
    // File file = new File(backupRoot, path.concat(".md5"));
    // try {
    // return FileUtils.readFile(file);
    // } catch (Exception e) {
    // return "";
    // }
    // }

    protected File getTargetFile(String path) {
        return new File(serverRoot, path);
    }

    protected void copy(File src, File dst) throws PackageException {
        try {
            dst.getParentFile().mkdirs();
            File tmp = new File(dst.getPath() + ".tmp");
            // File tmp = new File(dst.getParentFile(), dst.getName() + ".tmp");
            FileUtils.copy(src, tmp);
            if (!tmp.renameTo(dst)) {
                tmp.delete();
                FileUtils.copy(src, dst);
            }
        } catch (IOException e) {
            throw new PackageException("Failed to copy file: " + src + " to "
                    + dst, e);
        }
    }

    protected void deleteOldFile(File targetFile, File oldFile,
            boolean deleteOnExit) {
        if (oldFile == null || !oldFile.exists()) {
            return;
        }
        if (deleteOnExit) {
            if (targetFile.getName().equals(oldFile.getName())) {
                oldFile.delete();
            } else {
                oldFile.deleteOnExit();
            }
        } else {
            oldFile.delete();
        }
    }

    public Match<File> findInstalledJar(String key) {
        return JarUtils.findJar(serverRoot, key);
    }

    public Match<File> findBackupJar(String key) {
        return JarUtils.findJar(backupRoot, key);
    }

    public void doUpdate(String key, Version version, UpdateOptions opt)
            throws PackageException {
        Match<File> existingJar = findInstalledJar(key);
        if (opt.upgradeOnly && existingJar == null) {
            return;
        }
        if (!opt.allowDowngrade && existingJar != null) {
            FileVersion newVersion = new FileVersion(opt.version);
            FileVersion oldVersion = new FileVersion(existingJar.version);
            if (newVersion.lessThan(oldVersion)) {
                return;
            }
        }

        File oldFile = existingJar != null ? existingJar.object : null;
        deleteOldFile(opt.targetFile, oldFile, opt.deleteOnExit);
        copy(opt.file, opt.targetFile);
    }

}
