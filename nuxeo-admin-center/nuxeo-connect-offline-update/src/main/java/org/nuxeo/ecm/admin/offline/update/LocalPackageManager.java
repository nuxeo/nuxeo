/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.admin.offline.update;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.osgi.application.loader.FrameworkLoader;
import org.nuxeo.runtime.api.Framework;

/**
 * Offline Marketplace packages manager.
 *
 * See {@link #printHelp()} for the usage.
 *
 * The target directory is set from System property "nuxeo.runtime.home".
 *
 * <p>
 * The environment used by Nuxeo runtime can be specified as System properties.
 * <p>
 * All the bundles and the third parties must be on the boot classpath. You
 * should have at least these bundles:
 * <ul>
 * <li>nuxeo-common
 * <li>nuxeo-connect-client
 * <li>nuxeo-connect-client-wrapper
 * <li>nuxeo-connect-update
 * <li>nuxeo-connect-offline-update
 * <li>nuxeo-runtime
 * <li>nuxeo-runtime-osgi
 * <li>nuxeo-runtime-reload
 * </ul>
 * and these libraries:
 * <ul>
 * <li>commons-io
 * <li>groovy-all
 * <li>osgi-core
 * <li>xercesImpl
 * <li>commons-logging
 * <li>log4j
 * </ul>
 *
 */
public class LocalPackageManager {

    static final Log log = LogFactory.getLog(LocalPackageManager.class);

    protected File home;

    protected File wd;

    protected File bundlesDir;

    protected List<File> bundles;

    protected File config;

    protected Map<String, Object> env;

    protected Environment targetEnv;

    protected List<String> packages;

    protected PackageUpdateService pus;

    private String command;

    private int errorValue = 0;

    public static void main(String[] args) throws Exception {
        LocalPackageManager main = null;
        try {
            main = new LocalPackageManager(args);
            main.initializeFramework();
            main.startFramework();
            main.run(args);
        } catch (Throwable e) {
            log.error(e);
            main.errorValue = 2;
        } finally {
            if (main != null) {
                main.stopFramework();
            }
        }
        System.exit(main.errorValue);
    }

    public LocalPackageManager(String[] args) throws FileNotFoundException {
        if (args.length < 2) {
            printHelp();
            System.exit(1);
        }
        wd = new File(args[0]);
        if (!wd.isDirectory()) {
            throw new IllegalStateException(wd + " is not a directory!");
        }
        command = args[1];

        home = new File(System.getProperty("nuxeo.runtime.home"));
        if (home == null) {
            throw new IllegalStateException(
                    "Syntax Error: You must provide the runtime home "
                            + "as a System property (\""
                            + Environment.NUXEO_RUNTIME_HOME + "\").");
        }
        bundlesDir = new File(wd, "bundles");
        initBundleFiles();
        initEnvironment();
        targetEnv = createTargetEnvironment();
    }

    public void run(String[] args) {
        Environment defaultEnv = Environment.getDefault();
        try {
            Environment.setDefault(targetEnv);
            if ("install".equalsIgnoreCase(command)) {
                config = new File(args[2]);
                update();
            } else if ("installpkg".equalsIgnoreCase(command)) {
                String packageParam = args[2];
                if (new File(packageParam).exists()) {
                    // packageParam is a file
                    update(packageParam);
                } else {
                    // packageParam maybe an ID
                    updatePackage(packageParam);
                }
            } else if ("uninstall".equalsIgnoreCase(command)) {
                String pkgId = args[2];
                uninstall(pkgId);
            } else if ("list".equalsIgnoreCase(command)) {
                listPackages();
            } else {
                printHelp();
                return;
            }
        } catch (PackageException e) {
            log.error(e);
            errorValue = 1;
        } finally {
            Environment.setDefault(defaultEnv);
        }
    }

    public void printHelp() {
        log.error("\nLocalPackageManager usage: working_directory command [parameters]");
        log.error("\n\t Commands:");
        log.error("\t\t list\t\t\tLists local packages and their status.");
        log.error("\t\t install /path/to/upgrade/file\t\t\tReads the given upgrade file and performs install.");
        log.error("\t\t installpkg [/path/to/package|packageId]\t\t\tInstalls the given package (as a file or its ID).");
        log.error("\t\t uninstall packageId\t\t\tUninstalls the specified package.");
    }

    protected void initEnvironment() {
        env = new HashMap<String, Object>();
    }

    protected Environment createTargetEnvironment() {
        Environment environment = new Environment(home);
        environment.init();
        return environment;
    }

    protected void initBundleFiles() throws FileNotFoundException {
        bundles = new ArrayList<File>();
        if (!bundlesDir.isDirectory()) {
            throw new FileNotFoundException("File " + bundlesDir
                    + " is not a directory");
        }
        File[] list = bundlesDir.listFiles();
        if (list == null) {
            throw new FileNotFoundException("No bundles found in " + bundlesDir);
        }
        for (File file : list) {
            String name = file.getName();
            if (name.endsWith(".jar") && name.contains("nuxeo-")) { // a bundle
                if (!name.contains("osgi")) { // avoid loading the system bundle
                    bundles.add(file);
                }
            }
        }
    }

    public void initializeFramework() {
        System.setProperty("org.nuxeo.connect.update.dataDir",
                targetEnv.getData().getAbsolutePath());
        FrameworkLoader.initialize(LocalPackageManager.class.getClassLoader(),
                wd, bundles, env);
    }

    public void startFramework() throws Exception {
        FrameworkLoader.start();
        pus = Framework.getLocalService(PackageUpdateService.class);
        if (pus == null) {
            throw new IllegalStateException("PackagUpdateService not found");
        }
    }

    public void stopFramework() throws Exception {
        try {
            FrameworkLoader.stop();
        } finally {
            if (config != null) {
                config.delete();
            }
            if (wd != null) {
                FileUtils.deleteTree(wd);
            }
        }
    }

    public void update() throws PackageException {
        readPackages();
        log.info("Performing update ...");
        for (String pkgId : packages) {
            if (pkgId.startsWith("file:")) {
                String packageFileName = pkgId.substring(5);
                log.info("Getting Installation package " + packageFileName);
                LocalPackage pkg = pus.addPackage(new File(packageFileName));
                pkgId = pkg.getId();
            }
            updatePackage(pkgId);
        }
        log.info("Done.");
    }

    protected void readPackages() throws PackageException {
        packages = new ArrayList<String>();
        List<String> lines;
        try {
            lines = FileUtils.readLines(config);
            for (String line : lines) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    packages.add(line);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        if (packages.isEmpty()) {
            throw new PackageException("No bundles found in " + config);
        }
    }

    /**
     * @param pkgId
     * @throws PackageException
     * @since 5.5
     */
    public void update(String packageFileName) throws PackageException {
        LocalPackage pkg = pus.addPackage(new File(packageFileName));
        String pkgId = pkg.getId();
        updatePackage(pkgId);
    }

    protected void updatePackage(String pkgId) throws PackageException {
        LocalPackage pkg = pus.getPackage(pkgId);
        if (pkg == null) {
            throw new IllegalStateException("No package found: " + pkgId);
        }
        log.info("Updating " + pkgId);
        Task installTask = pkg.getInstallTask();
        try {
            performTask(installTask);
        } catch (Throwable e) {
            installTask.rollback();
            errorValue = 1;
            log.error("Failed to install package: " + pkgId, e);
        }
    }

    /**
     * Validate and run given task
     *
     * @since 5.5
     * @param task
     * @throws PackageException
     */
    public void performTask(Task task) throws PackageException {
        ValidationStatus status = task.validate();

        if (status.hasErrors()) {
            errorValue = 3;
            throw new PackageException("Failed to validate package "
                    + task.getPackage().getId() + " -> " + status.getErrors());
        }

        task.run(null);
    }

    /**
     * @param pkgId Marketplace package id
     * @throws PackageException
     * @since 5.5
     */
    private void uninstall(String pkgId) throws PackageException {
        LocalPackage pkg = pus.getPackage(pkgId);
        if (pkg == null) {
            throw new IllegalStateException("No package found: " + pkgId);
        }
        log.info("Uninstalling " + pkgId);
        Task uninstallTask = pkg.getUninstallTask();
        try {
            performTask(uninstallTask);
        } catch (Throwable e) {
            uninstallTask.rollback();
            errorValue = 1;
            log.error("Failed to uninstall package: " + pkgId, e);
        }
    }

    /**
     * @throws PackageException
     * @since 5.5
     */
    private void listPackages() throws PackageException {
        log.info(PackageState.REMOTE + "=remote " + PackageState.DOWNLOADING
                + "=downloading " + PackageState.DOWNLOADED + "=downloaded "
                + PackageState.INSTALLING + "=installing "
                + PackageState.INSTALLED + "=installed " + PackageState.STARTED
                + "=started ");
        List<LocalPackage> localPackages = pus.getPackages();
        for (LocalPackage localPackage : localPackages) {
            log.info("Package id=" + localPackage.getId() + " name="
                    + localPackage.getName() + " state="
                    + localPackage.getState());
        }
    }

}
