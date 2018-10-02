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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.connect.update.AlreadyExistsPackageException;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;

/**
 * The file {@code nxserver/data/packages/.packages} stores the state of all local features.
 * <p>
 * Each local package have a corresponding directory in {@code nxserver/data/features/store} which is named:
 * {@code <package_uid>} ("id-version")
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PackagePersistence {

    private static final Log log = LogFactory.getLog(PackagePersistence.class);

    protected final File root;

    protected final File store;

    protected final File temp;

    protected static final Random RANDOM = new SecureRandom();

    protected Map<String, PackageState> states;

    private PackageUpdateService service;

    public PackagePersistence(PackageUpdateService pus) throws IOException {
        Environment env = Environment.getDefault();
        root = env.getPath(Environment.NUXEO_MP_DIR, Environment.DEFAULT_MP_DIR);
        if (!root.isAbsolute()) {
            throw new RuntimeException();
        }
        root.mkdirs();
        store = new File(root, "store");
        store.mkdirs();
        temp = new File(root, "tmp");
        temp.mkdirs();
        service = pus;
        states = loadStates();
    }

    public File getRoot() {
        return root;
    }

    /**
     * @since 7.1
     */
    public File getStore() {
        return store;
    }

    public synchronized Map<String, PackageState> getStates() {
        return new HashMap<>(states);
    }

    protected Map<String, PackageState> loadStates() throws IOException {
        Map<String, PackageState> result = new HashMap<>();
        File file = new File(root, ".packages");
        if (file.isFile()) {
            List<String> lines = FileUtils.readLines(file, UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                int i = line.indexOf('=');
                String pkgId = line.substring(0, i).trim();
                String value = line.substring(i + 1).trim();
                PackageState state = PackageState.getByLabel(value);
                if (state == PackageState.UNKNOWN) {
                    try {
                        // Kept for backward compliance with int instead of enum
                        state = PackageState.getByValue(value);
                    } catch (NumberFormatException e) {
                        // Set as REMOTE if undefined/unreadable
                        state = PackageState.REMOTE;
                    }
                }
                result.put(pkgId, state);
            }
        }
        return result;
    }

    protected void writeStates() throws IOException {
        StringBuilder buf = new StringBuilder();
        for (Entry<String, PackageState> entry : states.entrySet()) {
            buf.append(entry.getKey()).append('=').append(entry.getValue()).append("\n");
        }
        File file = new File(root, ".packages");
        FileUtils.writeStringToFile(file, buf.toString(), UTF_8);
    }

    public LocalPackage getPackage(String id) throws PackageException {
        File file = new File(store, id);
        if (file.isDirectory()) {
            return new LocalPackageImpl(file, getState(id), service);
        }
        return null;
    }

    public synchronized LocalPackage addPackage(File file) throws PackageException {
        if (file.isDirectory()) {
            return addPackageFromDir(file);
        } else if (file.isFile()) {
            File tmp = newTempDir(file.getName());
            try {
                ZipUtils.unzip(file, tmp);
                return addPackageFromDir(tmp);
            } catch (IOException e) {
                throw new PackageException("Failed to unzip package: " + file.getName());
            } finally {
                // cleanup tmp if exists
                org.apache.commons.io.FileUtils.deleteQuietly(tmp);
            }
        } else {
            throw new PackageException("Not found: " + file);
        }
    }

    /**
     * Add unzipped packaged to local cache. It replaces SNAPSHOT packages if not installed
     *
     * @throws PackageException
     * @throws AlreadyExistsPackageException If not replacing a SNAPSHOT or if the existing package is installed
     */
    protected LocalPackage addPackageFromDir(File file) throws PackageException {
        LocalPackageImpl pkg = new LocalPackageImpl(file, PackageState.DOWNLOADED, service);
        File dir = null;
        try {
            dir = new File(store, pkg.getId());
            if (dir.exists()) {
                LocalPackage oldpkg = getPackage(pkg.getId());
                if (!pkg.getVersion().isSnapshot()) {
                    throw new AlreadyExistsPackageException("Package " + pkg.getId() + " already exists");
                }
                if (oldpkg.getPackageState().isInstalled()) {
                    throw new AlreadyExistsPackageException("Package " + pkg.getId() + " is already installed");
                }
                log.info(String.format("Replacement of %s in local cache...", oldpkg));
                org.apache.commons.io.FileUtils.deleteQuietly(dir);
            }
            org.apache.commons.io.FileUtils.copyDirectory(file, dir);
            pkg.getData().setRoot(dir);
            updateState(pkg.getId(), pkg.state);
            return pkg;
        } catch (IOException e) {
            throw new PackageException(String.format("Failed to move %s to %s", file, dir), e);
        }
    }

    public synchronized PackageState getState(String packageId) {
        PackageState state = states.get(packageId);
        if (state == null) {
            return PackageState.REMOTE;
        }
        return state;
    }

    /**
     * Get the local package having the given name and which is in either one of the following states:
     * <ul>
     * <li>{@link PackageState#INSTALLING}
     * <li>{@link PackageState#INSTALLED}
     * <li>{@link PackageState#STARTED}
     * </ul>
     */
    public LocalPackage getActivePackage(String name) throws PackageException {
        String pkgId = getActivePackageId(name);
        if (pkgId == null) {
            return null;
        }
        return getPackage(pkgId);
    }

    public synchronized String getActivePackageId(String name) throws PackageException {
        for (Entry<String, PackageState> entry : states.entrySet()) {
            String pkgId = entry.getKey();
            if (pkgId.startsWith(name) && entry.getValue().isInstalled() && getPackage(pkgId).getName().equals(name)) {
                return pkgId;
            }
        }
        return null;
    }

    public synchronized List<LocalPackage> getPackages() throws PackageException {
        File[] list = store.listFiles();
        if (list != null) {
            List<LocalPackage> pkgs = new ArrayList<>(list.length);
            for (File file : list) {
                if (!file.isDirectory()) {
                    log.warn("Ignoring file '" + file.getName() + "' in package store");
                    continue;
                }
                pkgs.add(new LocalPackageImpl(file, getState(file.getName()), service));
            }
            return pkgs;
        }
        return new ArrayList<>();
    }

    public synchronized void removePackage(String id) throws PackageException {
        states.remove(id);
        try {
            writeStates();
        } catch (IOException e) {
            throw new PackageException("Failed to write package states", e);
        }
        File file = new File(store, id);
        org.apache.commons.io.FileUtils.deleteQuietly(file);
    }

    /**
     * @deprecated Since 5.7. Use {@link #updateState(String, PackageState)} instead.
     */
    @Deprecated
    public synchronized void updateState(String id, int state) throws PackageException {
        states.put(id, PackageState.getByValue(state));
        try {
            writeStates();
        } catch (IOException e) {
            throw new PackageException("Failed to write package states", e);
        }
    }

    /**
     * @since 5.7
     */
    public synchronized void updateState(String id, PackageState state) throws PackageException {
        states.put(id, state);
        try {
            writeStates();
        } catch (IOException e) {
            throw new PackageException("Failed to write package states", e);
        }
    }

    public synchronized void reset() throws PackageException {
        String[] keys = states.keySet().toArray(new String[states.size()]);
        for (String key : keys) {
            states.put(key, PackageState.DOWNLOADED);
        }
        try {
            writeStates();
        } catch (IOException e) {
            throw new PackageException("Failed to write package states", e);
        }
    }

    protected File newTempDir(String id) {
        File tmp;
        synchronized (temp) {
            do {
                tmp = new File(temp, id + "-" + RANDOM.nextInt());
            } while (tmp.exists());
            tmp.mkdirs();
        }
        return tmp;
    }

    /**
     * @since 5.8
     */
    public FileTime getInstallDate(String id) {
        File file = new File(store, id);
        if (file.isDirectory()) {
            Path path = file.toPath();
            try {
                FileTime lastModifiedTime = Files.readAttributes(path, BasicFileAttributes.class).lastModifiedTime();
                return lastModifiedTime;
            } catch (IOException e) {
                log.error(e);
            }
        }
        return null;
    }
}
