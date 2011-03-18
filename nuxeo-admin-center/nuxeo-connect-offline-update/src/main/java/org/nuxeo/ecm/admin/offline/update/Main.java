/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.osgi.application.loader.FrameworkLoader;
import org.nuxeo.runtime.api.Framework;

/**
 * Main class to start the offline update application.
 *
 * The main method takes two arguments:
 * <ol>
 * <li>the working directory.
 * <li>the upgrade configuration file
 * </ol>
 *
 * Example: <code>Main "workingdir" "configFile"
 *
 * The target directory is set from System property "nuxeo.runtime.home".
 *
 * <p>
 * The environment used by Nuxeo runtime can be specified as System properties.
 * <p>
 * All the bundles and the third parties should be on the boot classpath.
 * You should have at least these bundles:
 * <ul>
 * <li> nuxeo-common
 * <li> nuxeo-connect-client
 * <li> nuxeo-connect-client-wrapper
 * <li> nuxeo-connect-update
 * <li> nuxeo-connect-offline-update
 * <li> nuxeo-runtime
 * <li> nuxeo-runtime-osgi
 * <li> nuxeo-runtime-reload
 * </ul>
 * and these libraries:
 * <ul>
 * <li>commons-io
 * <li>groovy-all
 * <li>osgi-core
 * <li>xercesImpl
 * <li>commons-logging
 * <li>log4j
 *
 */
public class Main {

    static final Log log = LogFactory.getLog(Main.class);

    public static void main(String[] args) throws Exception {
        Main main = null;
        try {
            main = new Main(args);
            main.initialize();
            main.start();
            main.update();
        } catch (Throwable e) {
            log.error(e);
            System.exit(2);
        } finally {
            if (main != null) {
                main.stop();
            }
        }
    }

    protected File home;

    protected File wd;

    protected File bundlesDir;

    protected List<File> bundles;

    protected File config;

    protected Map<String, Object> env;

    protected Environment targetEnv;

    protected List<String> packages;

    protected PackageUpdateService pus;

    public Main(String[] args) throws Exception {
        if (args.length != 2) {
            log.error("Syntax Error: You must specify the working directory "
                    + "and path to upgrade file as arguments of the Main class");
            System.exit(1);
        }
        wd = new File(args[0]);
        config = new File(args[1]);

        home = new File(System.getProperty("nuxeo.runtime.home"));
        if (home == null) {
            log.error("Syntax Error: You must specify the runtime home "
                    + "as a System property (\"nuxeo.runtime.home\").");
            System.exit(1);
        }
        if (!wd.isDirectory()) {
            throw new IllegalStateException(
                    "working directory is not a directory: " + wd);
        }
        bundlesDir = new File(wd, "bundles");
        initBundleFiles();
        env = initEnvironment();
        packages = readPackages();
        if (packages.isEmpty()) {
            log.error("Syntax Error: No bundles found in " + config);
            System.exit(1);
        }
        targetEnv = createTargetEnvironment();
    }

    protected Map<String, Object> initEnvironment() {
        HashMap<String, Object> env = new HashMap<String, Object>();
        return env;
    }

    protected Environment createTargetEnvironment() {
        Environment environment = new Environment(home);
        environment.init();
        return environment;
    }

    protected void initBundleFiles() throws Exception {
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

    public void initialize() throws Exception {
        System.setProperty("org.nuxeo.connect.update.dataDir",
                targetEnv.getData().getAbsolutePath());
        FrameworkLoader.initialize(Main.class.getClassLoader(), wd, bundles,
                env);
    }

    public void start() throws Exception {
        FrameworkLoader.start();
        pus = Framework.getLocalService(PackageUpdateService.class);
        if (pus == null) {
            throw new IllegalStateException("PackagUpdateService not found");
        }
    }

    public void stop() throws Exception {
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

    public void update() throws Exception {
        log.info("Performing update ...");
        Environment env = Environment.getDefault();
        try {
            Environment.setDefault(targetEnv);
            for (String pkgId : packages) {
                updatePackage(pkgId);
            }
        } finally {
            Environment.setDefault(env);
        }
        log.info("Done.");
    }

    protected List<String> readPackages() throws IOException {
        ArrayList<String> files = new ArrayList<String>();
        List<String> lines = FileUtils.readLines(config);
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 0 && !line.startsWith("#")) {
                files.add(line);
            }
        }
        return files;
    }

    protected void updatePackage(String pkgId) throws PackageException {
        LocalPackage pkg = pus.getPackage(pkgId);
        if (pkg == null) {
            throw new IllegalStateException("No package found: " + pkgId);
        }
        log.info("Updating " + pkgId);
        Task installTask = pkg.getInstallTask();
        ValidationStatus status = installTask.validate();

        if (status.hasErrors()) {
            log.error("Failed to install package " + pkgId + " -> "
                    + status.getErrors());
            System.exit(3);
        }

        Map<String, String> params = getTaskParams(pkgId);
        try {
            installTask.run(params);
        } catch (Throwable e) {
            installTask.rollback();
            log.error("Install failed for package: " + pkgId, e);
        }

    }

    protected Map<String, String> getTaskParams(String pkgId) {
        // TODO should return the install params from the file
        return new HashMap<String, String>();
    }
}
