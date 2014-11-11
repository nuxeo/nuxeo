/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 *
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.BundleException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class StandaloneApplication {

    public static final String MAIN_TASK = "org.nuxeo.osgi.application.main.task";

    private static final Log log = LogFactory.getLog(StandaloneApplication.class);

    private static StandaloneApplication instance;

    private static CommandLineOptions options;

    private static Runnable mainTask;

    protected boolean isStarted;

    protected File home;

    protected List<File> classPath;

    protected boolean scanForNestedJARs = true; // by default true

    protected OSGiAdapter osgi;

    public static StandaloneApplication getInstance() {
        return instance;
    }

    public static StandaloneApplication createInstance()
            throws IOException {
        if (instance != null) {
            throw new IllegalStateException("Application already instantiated");
        }
        instance = new StandaloneApplication();
        String val = options.getOption("scanForNestedJARs");
        if (val != null) {
            StandaloneApplication.instance.scanForNestedJARs = Boolean.parseBoolean(val);
        }
        return instance;
    }

    public void start(Properties properties) throws IOException, BundleException {
        if (isStarted) {
            throw new IllegalStateException(
                    "OSGi Application is already started");
        }
        osgi = new OSGiAdapter(properties);
        List<File> preBundles = loadUserBundles("pre-bundles");
        List<File> postBundles = loadUserBundles("post-bundles");
        osgi.start();
        // start level 1
        // start bundles that are specified in the osgi.bundles property
        if (preBundles != null) {
            startBundles(preBundles);
        }
        // start level 2
        // if needed install all discovered bundles (the one that are located in
        // bundles dir)
        autoInstallBundles();
        // start level 3
        if (postBundles != null) {
            startBundles(postBundles);
        }
        isStarted = true;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void shutdown() throws IOException, BundleException {
        if (!isStarted) {
            throw new IllegalStateException("OSGi Application was not started");
        }
        try {
            osgi.shutdown();
        } finally {
            isStarted = false;
        }
    }

    protected void startBundles(List<File> files)
            throws BundleException {
        for(File file:files) {
            osgi.install(file.toURI());
        }
    }

    protected List<File> loadUserBundles(String key) throws IOException {
        if (options == null) {
            return null;
        }
        String bundlesString = options.getOption(key);
        if (bundlesString == null) {
            return null; // no bundles to load
        }
        List<File> bundles = new ArrayList<File>();
        String[] ar = StringUtils.split(bundlesString, ':');
        for (String entry : ar) {
            entry = entry.trim();
            File file;
            if (entry.contains("file:")) {
                try {
                    URL url = new URL(entry);
                    file = new File(url.toURI());
                } catch (MalformedURLException e) {
                    throw new IOException(e);
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
            } else {
                file = new File(entry);
            }
            bundles.add(file);
        }
        return bundles;
    }

    public List<File> getClassPath() {
        return classPath;
    }

    public void setClassPath(List<File> classPath) {
        this.classPath = classPath;
    }

    protected void autoInstallBundles() throws IOException, BundleException {
        List<File> cp = getClassPath();
        if (cp == null || cp.isEmpty()) {
            return;
        }
        boolean clear = hasCommandLineOption("clear");
        ClassPath cpath = new ClassPath(osgi);
        File cache = new File(osgi.getDataDir(), "bundles.cache");
        if (!clear && cache.exists()) {
            try {
                cpath.restore(cache);
            } catch (IOException e) { // rebuild cache
                cpath.scan(classPath);
                cpath.store(cache);
            }
        } else {
            cpath.scan(classPath);
            cpath.store(cache);
        }
    }


    public void installAll(List<File> bundles) throws BundleException {
        ClassPath cpath = new ClassPath(osgi);
        cpath.scan(bundles);
    }


    public static CommandLineOptions getComandLineOptions() {
        return options;
    }

    public static boolean hasCommandLineOption(String option) {
        return options != null && options.hasOption(option);
    }

    public static void setMainTask(Runnable mainTask) {
        StandaloneApplication.mainTask = mainTask;
    }

    public static Runnable getMainTask() {
        return mainTask;
    }

    public static void main(List<File> classPath,
            String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        // parse command line args
        options = new CommandLineOptions(args);
        // start framework
        StandaloneApplication app = createInstance();
        // start level 0
        app.setClassPath(classPath);
        // start level 1
        app.start(options.getProperties());
        log.info("Framework started in "
                + ((System.currentTimeMillis() - startTime) / 1000) + " sec.");
        if (mainTask != null) {
            mainTask.run(); // load main task in system class loader (framework started)
        }
    }

}
