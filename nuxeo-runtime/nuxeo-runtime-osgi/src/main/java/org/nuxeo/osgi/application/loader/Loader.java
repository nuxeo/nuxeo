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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.nuxeo.common.Environment;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.DirectoryBundleFile;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.SystemBundle;
import org.nuxeo.osgi.application.SharedClassLoader;

/**
 * This class is used to load StandaloneApplication2
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Loader {

    protected static StandaloneApplication2 app;

    public static Environment createEnvironment() throws IOException {
        return createEnvironment(null);
    }

    public static Environment createEnvironment(Properties props) throws IOException {
        if (props != null) {
            String val = (String) props.get(Constants.HOME_DIR);
            if (val == null) {
                val = System.getProperty(Environment.HOME_DIR);
                if (val == null) {
                    val = ".";
                }
            }
            File home = new File(val);
            home = home.getCanonicalFile();
            Environment env = new Environment(home, props);
            val = (String) props.get(Constants.SYSTEM_BUNDLE);
            if (val != null) {
                env.setProperty(Constants.SYSTEM_BUNDLE, val);
            }
            String[] args = (String[]) props.get(Constants.COMMAND_LINE_ARGS);
            if (args != null) {
                env.setCommandLineArguments(args);
            } else {
                env.setCommandLineArguments(new String[0]);
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
            val = (String) props.get(Constants.TMP_DIR);
            if (val != null) {
                env.setHostApplicationName(val);
            } else {
                env.setHostApplicationName("NXLauncher");
            }
            val = (String) props.get(Constants.TMP_DIR);
            if (val != null) {
                env.setHostApplicationVersion(val);
            } else {
                env.setHostApplicationVersion("1.0.0");
            }
            env.getData().mkdirs();
            env.getLog().mkdirs();
            env.getTemp().mkdirs();
            return env;
        } else {
            return new Environment(new File("").getCanonicalFile());
        }
    }

    public static void loadFramework(SharedClassLoader cl, File systemBundle, List<File> classPath, Properties properties) throws Exception {
        long startTime = System.currentTimeMillis();
        Environment env = createEnvironment(properties);
        Environment.setDefault(env);
        String[] args = env.getCommandLineArguments();
        StandaloneApplication2 app = new StandaloneApplication2(cl, env);
        StandaloneApplication2.instance = app;
        if (args != null) {
            if (hasArgument(args, "-scanForNestedJars")) {
                app.setScanForNestedJARs(true);
            }
            if (hasArgument(args, "-clear")) {
                app.setFlushCache(true);
            }
        }
        File home = env.getHome();
//        String rawcp = env.getProperty(CLASS_PATH);
//        if (rawcp != null) {
//            List<File> classPath = buildClassPath(cl, home, rawcp);
//            app.setClassPath(classPath);
//        }
        app.setClassPath(classPath);
        String sb = env.getProperty(Constants.SYSTEM_BUNDLE);
        if (sb == null) {
            throw new IllegalStateException("Property systemBundle was not set");
        }

        System.out.println("======================================================================");
        System.out.println("= Starting Nuxeo Framework");
        System.out.println("======================================================================");
        System.out.println("  * Home Directory = "+home);
        System.out.println("  * Data Directory = "+env.getData());
        System.out.println("  * Log Directory = "+env.getLog());
        System.out.println("  * Configuration Directory = "+env.getConfig());
        System.out.println("  * Temp Directory = "+env.getTemp());
        System.out.println("  * System Bundle = "+systemBundle);
        System.out.println("  * Command Line Args = "+Arrays.asList(env.getCommandLineArguments()));
        System.out.println("======================================================================");

        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            // start level 0
            app.setSystemBundle(new SystemBundle(app, createSystemBundle(systemBundle), cl));
            // start level 1
            app.start();
            System.out.println("Framework started in "+((System.currentTimeMillis()-startTime)/1000)+" sec.");
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    public static void shutdown() {
        if (app != null) {
            try {
                app.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean hasArgument(String[] args, String arg) {
        if (args != null) {
            for (String el : args) {
                if (el.equals(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static File newFile(File home, String path) throws IOException {
        if (path.startsWith("/")) {
            return new File(path).getCanonicalFile();
        } else {
            return new File(home, path).getCanonicalFile();
        }
    }

    public static List<File> buildClassPath(SharedClassLoader classLoader, File home, String rawcp) {
        List<File> result = new ArrayList<File>();
        try {
            String[] cp = rawcp.split(":");
            for (String entry : cp) {
                File entryFile;
                if (entry.endsWith("/.")) {
                    entryFile = newFile(home, entry.substring(0, entry.length() - 2));
                    File[] files = entryFile.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            result.add(file);
                            classLoader.addURL(file.toURI().toURL());
                        }
                    }
                } else {
                    entryFile = newFile(home, entry);
                    result.add(entryFile);
                    classLoader.addURL(entryFile.toURI().toURL());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return result;
    }

    public static BundleFile createSystemBundle(File file) throws IOException {
        BundleFile sysbf;
        if (file.isFile()) {
            sysbf = new JarBundleFile(file);
        } else {
            sysbf = new DirectoryBundleFile(file);
        }
        return sysbf;
    }

}
