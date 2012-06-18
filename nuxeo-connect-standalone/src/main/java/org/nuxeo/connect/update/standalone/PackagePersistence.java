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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.connect.update.standalone;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.connect.update.AlreadyExistsPackageException;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.task.Task;

/**
 * The file {@code nxserver/data/packages/.packages} stores the state of all
 * local features.
 * <p>
 * Each local package have a corresponding directory in
 * {@code nxserver/data/features/store} which is named: {@code <package_uid>}
 * ("id-version")
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PackagePersistence {

    private static final String FEATURES_DIR = "packages";

    protected final File root;

    protected final File store;

    protected final File temp;

    protected final Random random = new Random();

    protected Map<String, Integer> states;

    private PackageUpdateService service;

    public PackagePersistence(PackageUpdateService pus) throws IOException {
        // check if we should use a custom dataDir - useful for offline update
        String dataDir = System.getProperty("org.nuxeo.connect.update.dataDir");
        if (dataDir != null) {
            root = new File(new File(dataDir), FEATURES_DIR);
        } else {
            root = new File(Environment.getDefault().getData(), FEATURES_DIR);
        }
        root.mkdirs();
        store = new File(root, "store");
        store.mkdirs();
        temp = new File(root, "tmp");
        temp.mkdirs();
        this.service = pus;
        states = loadStates();
    }

    public File getRoot() {
        return root;
    }

    public synchronized Map<String, Integer> getStates() {
        return new HashMap<String, Integer>(states);
    }

    protected Map<String, Integer> loadStates() throws IOException {
        Map<String, Integer> result = new HashMap<String, Integer>();
        File file = new File(root, ".packages");
        if (file.isFile()) {
            List<String> lines = FileUtils.readLines(file);
            for (String line : lines) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                int i = line.indexOf('=');
                String key = line.substring(0, i).trim();
                Integer val = null;
                try {
                    val = Integer.valueOf(line.substring(i + 1).trim());
                } catch (NumberFormatException e) { // silently ignore
                    val = new Integer(PackageState.REMOTE);
                }
                result.put(key, val);
            }
        }
        return result;
    }

    protected void writeStates() throws IOException {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, Integer> entry : states.entrySet()) {
            buf.append(entry.getKey()).append('=').append(
                    entry.getValue().toString()).append("\n");
        }
        File file = new File(root, ".packages");
        FileUtils.writeFile(file, buf.toString());
    }

    public LocalPackage getPackage(String id) throws PackageException {
        File file = new File(store, id);
        if (file.isDirectory()) {
            return new LocalPackageImpl(file, getState(id), service);
        }
        return null;
    }

    public synchronized LocalPackage addPackage(File file)
            throws PackageException {
        if (file.isDirectory()) {
            return addPackageFromDir(file);
        } else if (file.isFile()) {
            File tmp = newTempDir(file.getName());
            try {
                ZipUtils.unzip(file, tmp);
                return addPackageFromDir(tmp);
            } catch (IOException e) {
                throw new PackageException("Failed to unzip package: "
                        + file.getName());
            } finally {
                // cleanup if tmp still exists (should not happen)
                org.apache.commons.io.FileUtils.deleteQuietly(tmp);
            }
        } else {
            throw new PackageException("Not found: " + file);
        }
    }

    protected LocalPackage addPackageFromDir(File file) throws PackageException {
        LocalPackageImpl pkg = new LocalPackageImpl(file,
                PackageState.DOWNLOADED, service);
        File dir = new File(store, pkg.getId());
        if (dir.exists()) {
            // FIXME: refactor this way of handling Studio packages to be
            // consistent with other packages
            if (PackageType.STUDIO.equals(pkg.getType())
                    && pkg.getId().endsWith("-0.0.0-SNAPSHOT")) {
                // FIXME: maybe check for pkg#supportsHotReload and
                // Framework.isDevModeSet() or Framework.isDebugModeSet()?
                // this is a special case - reload a studio snapshot package
                // 1. first we need to uninstall the existing package
                LocalPackage oldpkg = getPackage(pkg.getId());
                if (oldpkg.getState() >= PackageState.INSTALLED) {
                    Task utask = oldpkg.getUninstallTask();
                    try {
                        utask.run(new HashMap<String, String>());
                    } catch (Throwable t) {
                        utask.rollback();
                        throw new PackageException(
                                "Failed to uninstall snapshot. Abort reloading: "
                                        + pkg.getId(), t);
                    }
                }
                // 2. remove the package data
                org.apache.commons.io.FileUtils.deleteQuietly(dir);
            } else {
                throw new AlreadyExistsPackageException("Package "
                        + pkg.getId() + " already exists");
            }
        }
        try {
            org.apache.commons.io.FileUtils.moveDirectory(file, dir);
        } catch (IOException e) {
            throw new PackageException(String.format("Failed to move %s to %s",
                    file, dir), e);
        }
        pkg.data.setRoot(dir);
        updateState(pkg.getId(), pkg.getState());
        return pkg;
    }

    public synchronized int getState(String featureId) {
        Integer state = states.get(featureId);
        if (state == null) {
            return 0;
        }
        return state;
    }

    /**
     * Get the local package having the given name and which is in either one
     * of the following states:
     * <ul>
     * <li> {@link PackageState#INSTALLING}
     * <li> {@link PackageState#INSTALLED}
     * <li> {@link PackageState#STARTED}
     * </ul>
     *
     * @param name
     */
    public LocalPackage getActivePackage(String name) throws PackageException {
        String pkgId = getActivePackageId(name);
        if (pkgId == null) {
            return null;
        }
        return getPackage(pkgId);
    }

    public synchronized String getActivePackageId(String name) {
        name = name + '-';
        for (Map.Entry<String, Integer> entry : states.entrySet()) {
            if (entry.getKey().startsWith(name)
                    && entry.getValue() >= PackageState.INSTALLING) {
                return entry.getKey();
            }
        }
        return null;
    }

    public synchronized List<LocalPackage> getPackages()
            throws PackageException {
        File[] list = store.listFiles();
        if (list != null) {
            List<LocalPackage> pkgs = new ArrayList<LocalPackage>(list.length);
            for (File file : list) {
                pkgs.add(new LocalPackageImpl(file, getState(file.getName()),
                        service));
            }
            return pkgs;
        }
        return new ArrayList<LocalPackage>();
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

    public synchronized void updateState(String id, int state)
            throws PackageException {
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
                tmp = new File(temp, id + "-" + random.nextInt());
            } while (tmp.exists());
            tmp.mkdirs();
        }
        return tmp;
    }
}
