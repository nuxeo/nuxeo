/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.SystemBundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StandaloneApplication extends OSGiAdapter {

    public static final String MAIN_TASK = "org.nuxeo.osgi.application.main.task";

    private static StandaloneApplication instance;
    private static CommandLineOptions options; // TODO should be remove
    private static String[] args;
    private static Runnable mainTask;

    protected final SharedClassLoader classLoader;
    protected final Environment env;
    protected boolean isStarted;
    protected File home;
    protected List<File> classPath;
    protected boolean scanForNestedJARs = true; // by default true

    // a list of class path prefixes that contains JARS that should not be treated as bundles.
    protected String[] libdirs;


    public static StandaloneApplication getInstance() {
        return instance;
    }

    public static StandaloneApplication createInstance(SharedClassLoader cl) throws IOException {
        if (instance != null) {
            throw new IllegalStateException("Application already instantiated");
        }
        // create application environment
        Environment env = createEnvironment();
        Environment.setDefault(env);
        instance = new StandaloneApplication(cl, env);
        String val = options.getOption("scanForNestedJARs");
        if (val != null) {
            StandaloneApplication.instance.scanForNestedJARs = Boolean.parseBoolean(val);
        }
        // hack to avoid deploying all jars in classpath as bundles
        String javaLibsProp = System.getProperty("org.nuxeo.launcher.libdirs");
        if (javaLibsProp != null) {
            String[] ar = StringUtils.split(javaLibsProp, ',', false);
            if (ar.length > 0) {
                instance.libdirs = ar;
                File wd = instance.getWorkingDir();
                for (int i=0; i<ar.length; i++) {
                    if (!ar[i].startsWith("/")) {
                        instance.libdirs[i] = new File(wd, ar[i]).getCanonicalFile().getAbsolutePath();
                    }
                }
            }
        }
        // end hack
        return instance;
    }

    private StandaloneApplication(SharedClassLoader cl, Environment env) {
        super(env.getHome(), env.getData(), env.getProperties());
        classLoader = cl;
        this.env = env;
    }

    public SharedClassLoader getSharedClassLoader() {
        return classLoader;
    }

    public Environment getEnvironment() {
        return env;
    }

    public void start() throws Exception {
        if (isStarted) {
            throw new IllegalStateException("OSGi Application is already started");
        }
        List<BundleFile> preBundles = loadUserBundles("pre-bundles");
        List<BundleFile> postBundles = loadUserBundles("post-bundles");
        // start level 1
        // start bundles that are specified in the osgi.bundles property
        if (preBundles != null) {
            startBundles(preBundles);
        }
        // start level 2
        // if needed install all discovered bundles (the one that are located in bundles dir)
        autoInstallBundles();
        // start level 3
        if (postBundles != null) {
            startBundles(postBundles);
        }
        fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, getSystemBundle(), null));
        isStarted = true;
    }

    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void shutdown() throws IOException {
        if (!isStarted) {
            throw new IllegalStateException("OSGi Application was not started");
        }
        try {
            super.shutdown();
        } finally {
            isStarted = false;
        }
    }

    protected void startBundles(List<BundleFile> bundles) throws Exception {
        for (BundleFile bf : bundles) {
            this.install(new BundleImpl(this, bf, classLoader.getLoader()));
        }
    }

    protected List<BundleFile> loadUserBundles(String key) throws Exception {
        if (options == null) {
            return null;
        }
        String bundlesString = options.getOption(key);
        if (bundlesString == null) {
            return null; // no bundles to load
        }
        List<BundleFile> bundles = new ArrayList<BundleFile>();
        String[] ar = StringUtils.split(bundlesString, ':', true);
        for (String entry : ar) {
            File file;
            if (entry.contains("file:")) {
                URL url = new URL(entry);
                file = new File(url.toURI());
            } else {
                file = new File(entry);
            }
            BundleFile bf;
            if (file.isDirectory()) {
                bf = new DirectoryBundleFile(file);
            } else {
                bf = new JarBundleFile(file);
            }
            classLoader.addURL(bf.getURL());
            bundles.add(bf);
        }
        return bundles;
    }

    public List<File> getClassPath() {
        return classPath;
    }

    public void setClassPath(List<File> classPath) {
        this.classPath = classPath;
    }

    protected void autoInstallBundles() throws Exception {
        List<File> cp = getClassPath();
        if (cp == null || cp.isEmpty()) {
            return;
        }
        boolean clear = hasCommandLineOption("clear");
        ClassPath cpath = new ClassPath(classLoader, new File(env.getData(), "nested-jars"));
        File cache = new File(env.getData(), "bundles.cache");
        if (!clear && cache.exists()) {
            try {
                cpath.restore(cache);
            } catch (BundleException e) { // rebuild cache
                cpath.scan(classPath, scanForNestedJARs, libdirs);
                cpath.store(cache);
            }
        } else {
            cpath.scan(classPath, scanForNestedJARs, libdirs);
            cpath.store(cache);
        }
        installAll(cpath.getBundles());
        //new ApplicationBundleLoader(this, !clear).loadBundles(classPath);
    }

    public void install(BundleFile bf) throws BundleException {
        install(new BundleImpl(this, bf, classLoader.getLoader()));
    }

    public void installAll(List<BundleFile> bundles) throws BundleException {
        for (BundleFile bf : bundles) {
            install(new BundleImpl(this, bf, classLoader.getLoader()));
        }
    }

    /**
     * Creates the system bundle from the jar specified by the
     * nuxeo.osgi.system.bundle property.
     */
    public static BundleFile createSystemBundle(URL systemBundle) throws URISyntaxException, IOException {
        File file = new File(systemBundle.toURI());
        BundleFile sysbf = null;
        if (file.isFile()) {
            sysbf = new JarBundleFile(file);
        } else {
            sysbf = new DirectoryBundleFile(file);
        }
        return sysbf;
    }

    public static CommandLineOptions getComandLineOptions() {
        return options;
    }

    public static boolean hasCommandLineOption(String option) {
        return options != null && options.hasOption(option);
    }

    public static Environment createEnvironment() throws IOException {
        if (options != null) {
            String val = options.getOption("home");
            if (val == null) {
                val = System.getProperty(Environment.HOME_DIR);
                if (val == null) {
                    val = ".";
                }
            }
            File home = new File(val);
            home = home.getCanonicalFile();
            Environment env = new Environment(home);
            env.setCommandLineArguments(args);
            val = options.getOption("data");
            if (val != null) {
                env.setData(new File(val).getCanonicalFile());
            }
            val = options.getOption("log");
            if (val != null) {
                env.setLog(new File(val).getCanonicalFile());
            }
            val = options.getOption("config");
            if (val != null) {
                env.setConfig(new File(val).getCanonicalFile());
            }
            val = options.getOption("web");
            if (val != null) {
                env.setWeb(new File(val).getCanonicalFile());
            }
            val = options.getOption("tmp");
            if (val != null) {
                env.setTemp(new File(val).getCanonicalFile());
            }
            val = options.getOption("bundles");
            if (val != null) {
                env.setProperty(Environment.BUNDLES, val);
            }
            env.setHostApplicationName("NXLauncher");
            env.setHostApplicationVersion("1.0.0");
            env.getData().mkdirs();
            env.getLog().mkdirs();
            env.getTemp().mkdirs();
            return env;
        } else {
            return new Environment(new File("").getCanonicalFile());
        }
    }

    public static void setMainTask(Runnable mainTask) {
        StandaloneApplication.mainTask = mainTask;
    }

    public static Runnable getMainTask() {
        return mainTask;
    }

    public static void main(URL systemBundle, List<File> classPath, String[] args) {
        SharedClassLoader classLoader = (SharedClassLoader)Thread.currentThread().getContextClassLoader();
        long startTime = System.currentTimeMillis();
        // parse command line args
        StandaloneApplication.args = args;
        options = new CommandLineOptions(args);
        // start framework
        StandaloneApplication app = null;
        try {
            app = createInstance(classLoader);
            // start level 0
            app.setClassPath(classPath);
            app.setSystemBundle(new SystemBundle(app, createSystemBundle(systemBundle), classLoader.getLoader()));
            // start level 1
            app.start();
            System.out.println("Framework started in "+((System.currentTimeMillis()-startTime)/1000)+" sec.");
            if (mainTask != null) {
                mainTask.run();
            }
        } catch (Throwable  e) {
            e.printStackTrace();
            System.exit(13);
// this should not be called here because it will stop the server if the main thread is not blocking
//        } finally {
//            try {
//                if (app != null && app.isStarted()) {
//                    app.shutdown();
//                }
//            } catch (Exception e) {
//                System.err.println("Failed to stop framework");
//                e.printStackTrace();
//            }
        }
    }

}
