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
package org.nuxeo.ecm.admin.offline.update;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * The main method takes 2 arguments:
 * <ol>
 * <li> the path to the home directory (where nxserver or nuxeo.ear is located)
 * <li> the bundles directory.
 * <li> the upgrade configuration file
 * </ol>
 *
 * Example:
 * <code>Main "../nxserver" "bundlesDir" "configFile"
 *
 * <p>
 * The environment used by Nuxeo runtime can be specified as Java system properties.
 * <p>
 * All the bundles and the third parties should be on the boot classpath.
 * You should have at least these bundles:
 * <ul>
 * <li> nuxeo-runtime-osgi
 * <li> nuxeo-runtime
 * <li> nuxeo-common
 * <li> nuxeo-connect-update
 * <li> nuxeo-connect-client
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {


    public static void main(String[] args) throws Exception {
        Main main = null;
        try {
            main = new Main(args);
            main.initialize();
            main.start();
            main.update();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(2);
        } finally {
            if (main != null) {
                main.stop();
            }
        }
    }


    protected File home;
    protected File bundlesDir;
    protected List<File> bundles;
    protected File config;
    protected Map<String,Object> env;
    protected List<String> packages;
    protected PackageUpdateService pus;

    public Main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Syntax Error: You must specify the home directory and the list of bundles as arguments of the Main class");
            System.exit(1);
        }
        home = new File(args[0]);
        initBundleFiles(args[1]);
        env = initEnvironment();
        config = new File(args[2]);
        packages = readPackages();
        if (packages.isEmpty()) {
            System.out.println("Syntax Error: No bundles found in "+config);
            System.exit(1);
        }
    }

    protected Map<String, Object> initEnvironment() {
        HashMap<String, Object> env = new HashMap<String, Object>();

        return env;
    }

    protected void initBundleFiles(String path) throws Exception {
        bundles = new ArrayList<File>();
        bundlesDir = new File(path);
        if (!bundlesDir.isDirectory()) {
            throw new FileNotFoundException("File "+path+" is not a directory");
        }
        File[] list = bundlesDir.listFiles();
        if (list == null) {
            throw new FileNotFoundException("No bundles found in "+path);
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
        FrameworkLoader.initialize(Main.class.getClassLoader(), home, bundles, env);
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
            if (bundlesDir != null) {
                FileUtils.deleteTree(bundlesDir);
            }
        }
    }


    public void update() throws Exception {
        System.out.println("Performing update ...");
        for (String pkgId : packages) {
            updatePackage(pkgId);
        }
        System.out.println("Done.");
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
            throw new IllegalStateException("No package found: "+pkgId);
        }

        Task installTask = pkg.getInstallTask();
        ValidationStatus status = installTask.validate();

        if (status.hasErrors()) {
            System.out.println("Failed to install package "+pkgId+" -> "+status.getErrors());
            System.exit(3);
        }

        //TODO: save user prefs in a .properties file and load the map from that file
        installTask.run(new HashMap<String, String>());

        Map<String,String> params = getTaskParams(pkgId);
        try {
            installTask.run(params);
        } catch (Throwable e) {
            installTask.rollback();
            System.out.println("Install failed for package: "+pkgId);
            e.printStackTrace();
        }

    }

    protected Map<String,String> getTaskParams(String pkgId) {
        // TODO should return the install params from the file
        return new HashMap<String, String>();
    }
}

