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

package org.nuxeo.runtime.tomcat;



import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.nuxeo.osgi.application.SharedClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoWebappLoader extends WebappLoader implements Constants {

    protected boolean isStarted = false;
    protected String home = "nuxeo";
    protected String data;
    protected String tmp;
    protected String log;
    protected String config;
    protected String systemBundle;
    protected String bundles;
    protected String classPath;
    protected String args;

    public void setHome(String root) {
        home = root;
    }

    public String getHome() {
        return home;
    }

    public void setTmp(String tmp) {
        this.tmp = tmp;
    }

    public String getTmp() {
        return tmp;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public void setBundles(String bundles) {
        this.bundles = bundles;
    }

    public String getBundles() {
        return bundles;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getConfig() {
        return config;
    }

    public void setSystemBundle(String systemBundle) {
        this.systemBundle = systemBundle;
    }

    public String getSystemBundle() {
        return systemBundle;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getArgs() {
        return args;
    }


    @Override
    public void start() throws LifecycleException {
         // start tomcat webapp loader
        super.start();
        // start nuxeo osgi framework
        startFramework();
    }

    @Override
    public void stop() throws LifecycleException {
        // stop nuxeo osgi framework
        stopFramework();
        // stop tomcat webapp loader
        super.stop();
    }

    protected void stopFramework() {
        if (!isStarted) {
            return;
        }
        System.out.println("Stopping Nuxeo Framework");
    }

    protected void startFramework() {
        File home = resolveHomeDirectory();
        if (home == null) {
            return;
        }
        if (systemBundle == null) {
            System.err.println("The attribute 'systemBundle' is not defined, Check your context.xml file. Nuxeo will not be started.");
            return; // systemBundle is required
        }

        Properties env = createEnvironment(home);
        try {
            File systemBundleFile = newFile(home, (String)env.get(SYSTEM_BUNDLE));
            SharedClassLoader loader = (SharedClassLoader)getClassLoader().getParent();
            // add system bundle to class path so that we can instantiate the loader without having class not found errors
            loader.addURL(systemBundleFile.toURI().toURL());
            // add any other nuxeo bundles and libs on the class path
            List<File> cp = null;
            if (classPath != null) {
                cp = buildClassPath(loader, home, classPath);
            } else {
                cp = new ArrayList<File>();
            }
            // we are using the webapp class loader an not te shared loader to get the launcher class since
            // the launcher may be put into WEB-INF/lib
            Class<?> clazz = getClassLoader().loadClass("org.nuxeo.osgi.application.loader.Loader");
            Method method = clazz.getMethod("loadFramework", SharedClassLoader.class, File.class, List.class, Properties.class);
            method.invoke(null, loader, systemBundleFile, cp, env);
        } catch (Throwable t) {
            System.err.println("Failed to invoke nuxeo launcher");
            t.printStackTrace();
        }

        isStarted = true;
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

    protected Properties createEnvironment(File homeDir) {
        Properties env = new Properties();
        env.put(SYSTEM_BUNDLE, systemBundle);
        env.put(HOST_NAME, "Tomcat");
        env.put(HOST_VERSION, "6.0.18");
        env.put(HOME_DIR, homeDir.getAbsolutePath());
        if (log != null) {
            env.put(LOG_DIR, log);
        }
        if (config != null) {
            env.put(CONFIG_DIR, config);
        }
        if (data != null) {
            env.put(DATA_DIR, data);
        }
        if (tmp != null) {
            env.put(TMP_DIR, tmp);
        }
        if (bundles != null) {
            env.put(BUNDLES, bundles);
        }
//        if (classPath != null) {
//            env.put(CLASS_PATH, classPath);
//        }
        if (args != null) {
            env.put(COMMAND_LINE_ARGS, args.split("\\s+"));
        }
        return env;
    }

    protected File resolveHomeDirectory() {
        String path = null;
        Container container = getContainer();
        if (home.startsWith("/")) {
            path = home;
        } else {
            try {
                Method method = StandardContext.class.getDeclaredMethod("getBasePath");
                method.setAccessible(true);
                path = (String)method.invoke(container);
            } catch (Throwable t) {
                t.printStackTrace();
                System.err.println("Cannot find base path from context "+container.getClass()+". Trying to use 'nuxeo.path' system property");
                path = System.getProperty("nuxeo.path");
                if (path == null) {
                    System.err.println("'nuxeo.path' not set. Giving up. Nuxeo will not be started");
                    return null;
                }
            }
          path = path+"/"+home;
        }
        return new File(path);
    }

}
