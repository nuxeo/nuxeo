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

package org.nuxeo.osgi.application.loader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.application.ClassPath;
import org.nuxeo.osgi.application.SharedClassLoader;
import org.nuxeo.osgi.application.StandaloneApplication;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;

/**
 * Was copied and improved from
 * TODO: should refactor implementation from nuxeo-osgi and use this one since it
 * is better decoupled from the launcher.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StandaloneApplication2 extends OSGiAdapter {

    public static final String MAIN_TASK = "org.nuxeo.osgi.application.main.task";

    static StandaloneApplication2 instance;
    private static Runnable mainTask;

    protected final SharedClassLoader classLoader;
    protected final Environment env;
    protected boolean isStarted;
    protected File home;
    protected List<File> classPath;
    protected boolean scanForNestedJARs = true; // by default true
    protected boolean flushCache = false;

    public static StandaloneApplication2 getInstance() {
        return instance;
    }

    public static StandaloneApplication2 createInstance(SharedClassLoader cl) throws IOException {
        if (instance != null) {
            throw new IllegalStateException("Application already instantiated");
        }
        // create application environment
        Environment env = createEnvironment();
        Environment.setDefault(env);
        instance = new StandaloneApplication2(cl, env);
        return instance;
    }

    public StandaloneApplication2(SharedClassLoader cl, Environment env) {
        super(env.getHome(), env.getData(), env.getProperties());
        classLoader = cl;
        this.env = env;
    }

    public void setScanForNestedJARs(boolean value) {
        this.scanForNestedJARs = value;
    }

    /**
     * @param flushCache the flushCache to set.
     */
    public void setFlushCache(boolean flushCache) {
        this.flushCache = flushCache;
    }

    /**
     * @return the scanForNestedJARs.
     */
    public boolean isScanForNestedJARs() {
        return scanForNestedJARs;
    }

    public ClassLoader getClassLoader() {
        return classLoader.getLoader();
    }

    public Environment getEnvironment() {
        return env;
    }

    public void start() throws Exception {
        if (isStarted) {
            throw new IllegalStateException("OSGi Application is already started");
        }
        // start level 1
        // start bundles that are specified in the osgi.bundles property
        startBundles();
        // start level 2
        // if needed install all discovered bundles (the one that are located in bundles dir)
        autoInstallBundles();
        // start level 3
        //TODO
        fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, getSystemBundle(), null));
        if (mainTask != null) {
            mainTask.run();
        } else {
            // for compatibility with StandaloneApplicaiton which is used by nxshell
            //TODO must move main task in environment
            Field f = StandaloneApplication.class.getDeclaredField("mainTask");
            f.setAccessible(true);
            mainTask = (Runnable)f.get(null);
            if (mainTask != null) {
                mainTask.run();
            }
        }
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

    //TODO: this method doesn't work - it must be sync with the one from
    // StandaloneApplication
    protected void startBundles() throws Exception {
        String bundlesString = env.getProperty(Environment.BUNDLES);
        if (bundlesString == null) {
            return; // no bundles to start
        }
        Iterable<BundleFile> bundles = new ArrayList<BundleFile>();
        String[] ar = StringUtils.split(bundlesString, ',', true);
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
        }
        for (BundleFile bf : bundles) {
            this.install(new BundleImpl(this, bf, classLoader.getLoader()));
        }
    }

    public List<File> getClassPath() {
        return classPath;
    }

    public void setClassPath(List<File> classPath) {
        this.classPath = classPath;
    }
    
    protected String[] getBundleBlackList() throws IOException {
        // hack to avoid deploying all jars in classpath as bundles        
        String javaLibsProp = System.getProperty("org.nuxeo.launcher.libdirs");
        if (javaLibsProp != null) {
            String[] ar = StringUtils.split(javaLibsProp, ',', false);
            if (ar.length > 0) {
                String[] blackList = ar;
                File wd = instance.getWorkingDir();
                for (int i=0; i<ar.length; i++) {
                    if (!ar[i].startsWith("/")) {
                        blackList[i] = new File(wd, ar[i]).getCanonicalFile().getAbsolutePath();
                    }
                }
                return blackList;
            }
        }
        return null;
        // end hack
    }

    protected void autoInstallBundles() throws Exception {        
        List<File> cp = getClassPath();
        if (cp == null || cp.isEmpty()) {
            return;
        }
        ClassPath cpath = new ClassPath(classLoader, new File(env.getData(), "nested-jars"));
        File cache = new File(env.getData(), "bundles.cache");
        if (!flushCache && cache.exists()) {
            try {
                cpath.restore(cache);
            } catch (BundleException e) { // rebuild cache
                cpath.scan(classPath, scanForNestedJARs, getBundleBlackList());
                cpath.store(cache);
            }
        } else {
            cpath.scan(classPath, scanForNestedJARs, getBundleBlackList());
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

    public static Environment createEnvironment() throws IOException {
        return createEnvironment(null);
    }

    public static Environment createEnvironment(Properties props) throws IOException {
        if (props != null) {
            String val = (String) props.get("home");
            if (val == null) {
                val = System.getProperty(Environment.HOME_DIR);
                if (val == null) {
                    val = ".";
                }
            }
            File home = new File(val);
            home = home.getCanonicalFile();
            Environment env = new Environment(home);
            String[] args = (String[]) props.get(Constants.COMMAND_LINE_ARGS);
            if (args != null) {
                env.setCommandLineArguments(args);
            }
            val = (String) props.get(Constants.DATA_DIR);
            if (val != null) {
                env.setData(new File(val).getCanonicalFile());
            }
            val = (String) props.get(Constants.LOG_DIR);
            if (val != null) {
                env.setLog(new File(val).getCanonicalFile());
            }
            val = (String) props.get(Constants.CONFIG_DIR);
            if (val != null) {
                env.setConfig(new File(val).getCanonicalFile());
            }
            val = (String) props.get(Constants.WEB_DIR);
            if (val != null) {
                env.setWeb(new File(val).getCanonicalFile());
            }
            val = (String) props.get(Constants.TMP_DIR);
            if (val != null) {
                env.setTemp(new File(val).getCanonicalFile());
            }
            val = (String) props.get("bundles");
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
        StandaloneApplication2.mainTask = mainTask;
    }

}
