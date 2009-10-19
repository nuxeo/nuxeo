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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;

/**
 * shared attribute is experimental. do not use it yet.
 * (it's purpose is to be able to deploy multiple WARs using the same nuxeo instance but it is not working yet) 
 *  
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoWebappLoader extends WebappLoader implements Constants {

    protected final static Object LOCK = new Object(); 

    protected boolean shared;
    protected int webApps = 0;
    protected String home = "nuxeo";
    protected String data;
    protected String tmp;
    protected String log;
    protected String config;
    protected String systemBundle;
    protected String bundles;
    protected String classPath;
    protected String args;

    public void setShared(boolean shared) {
        this.shared = shared;
    }
    
    public boolean isShared() {
        return shared;
    }
    
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
        //startFramework();
    }

    @Override
    public void stop() throws LifecycleException {
        // stop nuxeo osgi framework
        //stopFramework();
        // stop tomcat webapp loader
        super.stop();
    }

    public int getWebAppsCount() {
        synchronized(LOCK) {
            return webApps;
        }
    }
    
    public boolean isFrameworkStarted() {
        return getWebAppsCount() > 0;
    }
    
    protected void stopFramework() {
        synchronized (LOCK) {
            if (webApps == 1) {
                doStopFramework();
                webApps = 0;
            }
        }
    }
    
    protected void doStopFramework() {
        try {
            System.out.println("Stopping Nuxeo Framework");
            Class<?> clazz = getClassLoader().loadClass("org.nuxeo.osgi.application.loader.Loader");
            Method method = clazz.getMethod("shutdown");
            method.invoke(null);
        } catch (Throwable t) {
            System.err.println("Failed to invoke nuxeo launcher");
            t.printStackTrace();
        }
    }

    protected void checkSharedClassLoader() {
        if (shared) {
            NuxeoWebappClassLoader cl = (NuxeoWebappClassLoader)getClassLoader();
            cl.setParentClassLoader(FrameworkClassLoader.getInstance(cl.getParentClassLoader()));
        }
    }
    
    protected void startFramework() {
        checkSharedClassLoader();
        synchronized (LOCK) {
            if (webApps == 0) {
                doStartFramework();
                webApps++;
            }
        }
    }
    
    private void doStartFramework() {
        File home = resolveHomeDirectory();
        if (home == null) {
            return;
        }
        if (systemBundle == null) {
            System.err.println("The attribute 'systemBundle' is not defined, Check your context.xml file. Nuxeo will not be started.");
            return; // systemBundle is required
        }

        // We need to set the nuxeo home as a system property so external tools like log4j can use it.
        // For example log4j can use variable expansion in log4j.properties like ${org.nuxeo.app.home}/log/server.log
        // to correctly setup the log files.
        // The create environment will also register env properties as system properties
        Properties env = createEnvironment(home);

        // load other system properties from the ${org.nuxeo.app.home}/system.properties if any is defined.
        // The properties in system.properties dir supports variable expansion of existing system properties and
        // of nuxeo environment properties.
        // these can be used in the same manner as ${org.nuxeo.app.home} by external libs.
        // An example is derby that needs to know where in the filesystem to create its databases.
        loadSystemProperties(home);

        try {
            File systemBundleFile = newFile(home, (String)env.get(SYSTEM_BUNDLE));
            NuxeoWebappClassLoader loader = (NuxeoWebappClassLoader)getClassLoader();
            MutableURLClassLoader cl = shared 
                ? (FrameworkClassLoader)loader.getParentClassLoader() 
                        : loader;
            // add system bundle to class path so that we can instantiate the loader without having class not found errors
            cl.addURL(systemBundleFile.toURI().toURL());
            // add any other nuxeo bundles and libs on the class path
            List<File> cp = null;
            if (classPath != null) {
                cp = buildClassPath(cl, home, classPath);
            } else {
                cp = new ArrayList<File>();
            }
            // we are using the webapp class loader an not te shared loader to get the launcher class since
            // the launcher may be put into WEB-INF/lib
            Class<?> clazz = getClassLoader().loadClass("org.nuxeo.osgi.application.loader.Loader");
            Method method = clazz.getMethod("loadFramework", ClassLoader.class, File.class, List.class, Properties.class);
            method.invoke(null, cl, systemBundleFile, cp, env);
        } catch (Throwable t) {
            System.err.println("Failed to invoke nuxeo launcher");
            t.printStackTrace();
        }
    }


    public static File newFile(File home, String path) throws IOException {
        if (path.startsWith("/")) {
            return new File(path).getCanonicalFile();
        } else {
            return new File(home, path).getCanonicalFile();
        }
    }

    public static List<File> buildClassPath(MutableURLClassLoader classLoader, File home, String rawcp) {
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

    /**
     * This will also install env. properties as java system properties.
     */
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
        System.getProperties().putAll(env);
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


    protected void loadSystemProperties(File home) {
        File file = new File(home, "system.properties");
        if (!file.isFile()) { // no system properties to load
            return;
        }
        Properties props = new Properties();
        InputStream in = null;
        try {
            Properties sysProps = System.getProperties();
            in = new FileInputStream(file);
            props.load(in);
            for (Map.Entry<Object,Object> entry : props.entrySet()) {
                String key = entry.getKey().toString();
                String value = expandVars(entry.getValue().toString(), sysProps);
                sysProps.put(key, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ee) { ee.printStackTrace(); }
            }
        }
    }

    /**
     * Copied from org.nuxeo.runtime.api.Framework
     * <p>
     * Expands any variable found in the given expression with the value
     * of the corresponding framework property.
     * <p>
     * The variable format is ${property_key}.
     * <p>
     * System properties are also expanded.
     */
    public static String expandVars(String expression, Map<Object,Object> vars) {
        int p = expression.indexOf("${");
        if (p == -1) {
            return expression; // do not expand if not needed
        }

        char[] buf = expression.toCharArray();
        StringBuilder result = new StringBuilder(buf.length);
        if (p > 0) {
            result.append(expression.substring(0, p));
        }
        StringBuilder varBuf = new StringBuilder();
        boolean dollar = false;
        boolean var = false;
        for (int i = p; i < buf.length; i++) {
            char c = buf[i];
            switch (c) {
            case '$' :
                dollar = true;
                break;
            case '{' :
                if (dollar) {
                    dollar = false;
                    var = true;
                }
                break;
            case '}':
                if (var) {
                  var = false;
                  String varName = varBuf.toString();
                  varBuf.setLength(0);
                  Object varValue = vars.get(varName); // get the variable value
                  if (varValue != null) {
                      result.append(varValue.toString());
                  } else { // let the variable as is
                      result.append("${").append(varName).append('}');
                  }
                }
                break;
            default:
                if (var) {
                  varBuf.append(c);
                } else {
                    result.append(c);
                }
                break;
            }
        }
        return result.toString();
    }

}
