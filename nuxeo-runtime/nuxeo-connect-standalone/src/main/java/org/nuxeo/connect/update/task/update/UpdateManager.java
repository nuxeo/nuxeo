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
package org.nuxeo.connect.update.task.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
 * To manipulate the jar version registry you need to create a new instance of this class.
 * <p>
 * If you want to modify the registry then you may want to synchronize the entire update process. This is how is done in
 * the Task run method.
 * <p>
 * Only reading the registry is thread safe.
 * <p>
 * TODO backup md5 are not really used since we rely on versions - we can remove md5
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class UpdateManager {

    private static final Log log = LogFactory.getLog(UpdateManager.class);

    public static final String STUDIO_SNAPSHOT_VERSION = "0.0.0-SNAPSHOT";

    protected Task task;

    protected Map<String, Entry> registry;

    protected File file;

    protected File backupRoot;

    protected File serverRoot;

    public UpdateManager(File serverRoot, File regFile) {
        file = regFile;
        backupRoot = new File(file.getParentFile(), "backup");
        backupRoot.mkdirs();
        this.serverRoot = serverRoot;
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
            registry = new HashMap<>();
            return;
        }
        try {
            registry = RegistrySerializer.load(file);
        } catch (PackageException e) {
            throw e;
        } catch (IOException e) {
            throw new PackageException("IOException while trying to load the registry", e);
        }
    }

    public synchronized void store() throws PackageException {
        try {
            RegistrySerializer.store(registry, file);
        } catch (IOException e) {
            throw new PackageException("IOException while trying to write the registry", e);
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

    public RollbackOptions update(UpdateOptions opt) throws PackageException {
        String key = getKey(opt);
        Entry entry = registry.get(key);
        if (entry == null) { // New Entry
            entry = createEntry(key);
        } else if (!entry.hasBaseVersion() && entry.getLastVersion(false) == null) {
            // Existing Entry but all versions provided only by packages with upgradeOnly => check missing base
            // version...
            if (createBaseVersion(entry)) {
                log.warn("Registry repaired: JAR introduced without corresponding entry in the registry (copy task?) : "
                        + key);
            }
        }
        Version v = entry.getVersion(opt.version);
        boolean newVersion = v == null;
        if (v == null) {
            v = entry.addVersion(new Version(opt.getVersion()));
            v.setPath(getVersionPath(opt));
        }
        v.addPackage(opt);
        if (newVersion || opt.isSnapshotVersion()) {
            // Snapshots "backup" are overwritten by new versions
            backupFile(opt.getFile(), v.getPath());
        }

        Match<File> currentJar = findInstalledJar(key);
        UpdateOptions optToUpdate = shouldUpdate(key, opt, currentJar);
        if (optToUpdate != null) {
            File currentFile = currentJar != null ? currentJar.object : null;
            doUpdate(currentFile, optToUpdate);
        }

        return new RollbackOptions(key, opt);
    }

    /**
     * Look if an update is required, taking into account the given UpdateOptions, the currently installed JAR and the
     * other available JARs.
     *
     * @since 5.7
     * @param key
     * @param opt
     * @param currentJar
     * @return null if no update required, else the right UpdateOptions
     * @throws PackageException
     */
    protected UpdateOptions shouldUpdate(String key, UpdateOptions opt, Match<File> currentJar)
            throws PackageException {
        log.debug("Look for updating " + opt.file.getName());
        if (opt.upgradeOnly && currentJar == null) {
            log.debug("=> don't update (upgradeOnly)");
            return null;
        }
        if (opt.allowDowngrade) {
            log.debug("=> update (allowDowngrade)");
            return opt;
        }

        // !opt.allowDowngrade && (!opt.upgradeOnly || currentJar != null) ...
        UpdateOptions optToUpdate = null;
        Version packageVersion = registry.get(key).getVersion(opt.version);
        Version greatestVersion = registry.get(key).getGreatestVersion();
        if (packageVersion.equals(greatestVersion)) {
            optToUpdate = opt;
        } else { // we'll use the greatest available JAR instead
            optToUpdate = UpdateOptions.newInstance(opt.pkgId, new File(backupRoot, greatestVersion.path),
                    opt.targetDir);
        }
        FileVersion greatestFileVersion = greatestVersion.getFileVersion();
        if (currentJar == null) {
            log.debug("=> update (new) " + greatestFileVersion);
            return optToUpdate;
        }

        // !opt.allowDowngrade && currentJar != null ...
        FileVersion currentVersion = new FileVersion(currentJar.version);
        log.debug("=> comparing " + greatestFileVersion + " with " + currentVersion);
        if (greatestFileVersion.greaterThan(currentVersion)) {
            log.debug("=> update (greater)");
            return optToUpdate;
        } else if (greatestFileVersion.equals(currentVersion)) {
            if (greatestFileVersion.isSnapshot()) {
                FileInputStream is1 = null;
                FileInputStream is2 = null;
                try {
                    is1 = new FileInputStream(new File(backupRoot, greatestVersion.path));
                    is2 = new FileInputStream(currentJar.object);
                    if (IOUtils.contentEquals(is1, is2)) {
                        log.debug("=> don't update (already installed)");
                        return null;
                    } else {
                        log.debug("=> update (newer SNAPSHOT)");
                        return optToUpdate;
                    }
                } catch (IOException e) {
                    throw new PackageException(e);
                } finally {
                    IOUtils.closeQuietly(is1);
                    IOUtils.closeQuietly(is2);
                }
            } else {
                log.debug("=> don't update (already installed)");
                return null;
            }
        } else {
            log.debug("Don't update (lower)");
            return null;
        }
    }

    /**
     * Ugly method to know what file is going to be deleted before it is, so that it can be undeployed for hotreload.
     * <p>
     * FIXME: will only handle simple cases for now (ignores version, etc...), e.g only tested with the main Studio
     * jars. Should use version from RollbackOptions
     *
     * @since 5.6
     */
    public File getRollbackTarget(RollbackOptions opt) {
        String entryKey = opt.getKey();
        Match<File> m = findInstalledJar(entryKey);
        if (m != null) {
            return m.object;
        } else {
            log.trace("Could not find jar with key: " + entryKey);
            return null;
        }
    }

    /**
     * Perform a rollback.
     * <p>
     * TODO the deleteOnExit is inherited from the current rollback command ... may be it should be read from the
     * version that is rollbacked. (deleteOnExit should be an attribute of the entry not of the version)
     *
     * @param opt
     * @throws PackageException
     */
    public void rollback(RollbackOptions opt) throws PackageException {
        Entry entry = registry.get(opt.getKey());
        if (entry == null) {
            log.debug("Key not found in registry for: " + opt);
            return;
        }
        Version v = entry.getVersion(opt.getVersion());
        if (v == null) {
            // allow empty version for Studio snapshot...
            v = entry.getVersion(STUDIO_SNAPSHOT_VERSION);
        }
        if (v == null) {
            log.debug("Version not found in registry for: " + opt);
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

        // Include upgradeOnly versions only if there is a base version or a non-upgradeOnly version
        boolean includeUpgradeOnly = entry.hasBaseVersion() || entry.getLastVersion(false) != null;
        Version versionToRollback = entry.getLastVersion(includeUpgradeOnly);
        if (versionToRollback == null) {
            // no more versions - remove entry and rollback base version if any
            if (entry.isEmpty()) {
                registry.remove(entry.getKey());
            }
            rollbackBaseVersion(entry, opt);
        } else if (versionToRollback != lastVersion) {
            // we removed the currently installed version so we need to rollback
            rollbackVersion(entry, versionToRollback, opt);
        } else {
            // handle jars that were blocked using allowDowngrade or
            // upgradeOnly
            Match<File> m = findInstalledJar(opt.getKey());
            if (m != null) {
                if (entry.getVersion(m.version) == null) {
                    // the currently installed version is no more in registry
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

    protected void rollbackBaseVersion(Entry entry, RollbackOptions opt) throws PackageException {
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

    protected void rollbackVersion(Entry entry, Version version, RollbackOptions opt) throws PackageException {
        File versionFile = getBackup(version.getPath());
        if (!versionFile.isFile()) {
            log.warn("Could not rollback version " + version.getPath() + " since the backup file was not found");
            return;
        }
        Match<File> m = findInstalledJar(entry.getKey());
        File oldFile = m != null ? m.object : null;
        File targetFile = getTargetFile(version.getPath());
        if (deleteOldFile(targetFile, oldFile, opt.deleteOnExit)) {
            copy(versionFile, targetFile);
        }
    }

    public String getServerRelativePath(File someFile) {
        String path;
        String serverPath;
        try {
            path = someFile.getCanonicalPath();
            serverPath = serverRoot.getCanonicalPath();
        } catch (IOException e) {
            log.error("Failed to get a canonical path. " + "Fall back to absolute paths...", e);
            path = someFile.getAbsolutePath();
            serverPath = serverRoot.getAbsolutePath();
        }
        if (!serverPath.endsWith(File.separator)) {
            serverPath = serverPath.concat(File.separator);
        }
        if (path.startsWith(serverPath)) {
            return path.substring(serverPath.length());
        }
        return path;
    }

    /**
     * Create a new entry in the registry given the entry key. A base version will be automatically created if needed.
     *
     * @param key
     * @throws PackageException
     */
    public Entry createEntry(String key) throws PackageException {
        Entry entry = new Entry(key);
        createBaseVersion(entry);
        registry.put(key, entry);
        return entry;
    }

    /**
     * Create a base version for the given entry if needed.
     *
     * @param entry
     * @return true if a base version was actually created, false otherwise
     * @throws PackageException
     * @since 1.4.26
     */
    public boolean createBaseVersion(Entry entry) throws PackageException {
        Match<File> m = JarUtils.findJar(serverRoot, entry.getKey());
        if (m != null) {
            String path = getServerRelativePath(m.object);
            Version base = new Version(m.version);
            base.setPath(path);
            entry.setBaseVersion(base);
            backupFile(m.object, path);
            return true;
        }
        return false;
    }

    /**
     * Backup the given file in the registry storage. Backup is not a backup performed on removed files: it is rather
     * like a uniformed storage of all libraries potentially installed by packages (whereas each package can have its
     * own directory structure). So SNAPSHOT will always be overwritten. Backup of original SNAPSHOT can be found in the
     * backup directory of the stored package.
     *
     * @param fileToBackup
     * @param path
     */
    protected void backupFile(File fileToBackup, String path) throws PackageException {
        try {
            File dst = new File(backupRoot, path);
            copy(fileToBackup, dst);
            // String md5 = IOUtils.createMd5(dst);
            // FileUtils.writeFile(new
            // File(dst.getAbsolutePath().concat(".md5")),
            // md5);
        } catch (PackageException e) {
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
    }

    protected File getBackup(String path) {
        return new File(backupRoot, path);
    }

    protected File getTargetFile(String path) {
        return new File(serverRoot, path);
    }

    protected void copy(File src, File dst) throws PackageException {
        try {
            dst.getParentFile().mkdirs();
            File tmp = new File(dst.getPath() + ".tmp");
            // File tmp = new File(dst.getParentFile(), dst.getName() +
            // ".tmp");
            FileUtils.copy(src, tmp);
            if (!tmp.renameTo(dst)) {
                tmp.delete();
                FileUtils.copy(src, dst);
            }
        } catch (IOException e) {
            throw new PackageException("Failed to copy file: " + src + " to " + dst, e);
        }
    }

    protected boolean deleteOldFile(File targetFile, File oldFile, boolean deleteOnExit) {
        if (oldFile == null || !oldFile.exists()) {
            return false;
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
        return true;
    }

    public Match<File> findInstalledJar(String key) {
        return JarUtils.findJar(serverRoot, key);
    }

    public Match<File> findBackupJar(String key) {
        return JarUtils.findJar(backupRoot, key);
    }

    /**
     * Update oldFile with file pointed by opt
     *
     * @throws PackageException
     */
    public void doUpdate(File oldFile, UpdateOptions opt) throws PackageException {
        deleteOldFile(opt.targetFile, oldFile, opt.deleteOnExit);
        copy(opt.file, opt.targetFile);
        log.trace("Updated " + opt.targetFile);
    }

}
