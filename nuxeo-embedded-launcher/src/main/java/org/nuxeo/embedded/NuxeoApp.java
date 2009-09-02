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
 */
package org.nuxeo.embedded;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.SystemBundle;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;


/**
 * The system property nuxeo.home can be used to specify the nuxoe working
 * directory. If not specified ${user.home}/.nxserver will be used.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class NuxeoApp {

    private static final Log log = LogFactory.getLog(NuxeoAppActivator.class);

    private static Pattern NUXEO_JAR_PATTERN = Pattern.compile(".+/nuxeo-[^/]+\\.jar");
    private static NuxeoApp instance;

    public static NuxeoApp getInstance() {
        return instance;
    }

    protected static void syntaxError() {
        System.err.println("Syntax error. Usage: <app> [-server [localhost:8080]] [-d home]");
        System.exit(1);
    }

    protected static String getDefaultHome() {
        String home = System.getProperty("nuxeo.home");
        return home != null ? home : System.getProperty("user.home")
                + "/.nxserver";
    }

    public static void main(String[] args) throws Exception {
        NuxeoApp.class.getClassLoader().loadClass(
                "org.nuxeo.runtime.api.Framework");
        String home = null;
        String host = null;
        String remote = null;
        String exec = null;
        boolean interactive = true;
        String lastKey = null;
        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (key.equals("-noshell")) {
                interactive  = false;
                continue;
            }
            if (key.startsWith("-")) {
                if (key.equals("-http")) {
                    host = "localhost:8080";
                } else if (key.equals("-connect")) {
                    remote = "localhost:62474";
                } else if (key.equals("-exec")) {
                    exec = "help";
                }
                lastKey = key;
            } else if (key != null) {
                if (lastKey.equals("-server")) {
                    host = key;
                } else if (lastKey.equals("-d")) {
                    home = key;
                } else if (lastKey.equals("-exec")) {
                    exec = key;
                } else if (lastKey.equals("-connect")) {
                    remote = key;
                }
                key = null;
            } else {
                syntaxError();
            }
        }

        String h = null;
        int p = 0;
        if (host != null) {
            int i = host.lastIndexOf(':');
            if (i > -1) {
                h = host.substring(0, i);
                p = Integer.valueOf(host.substring(i + 1));
            }
        }
        if (home == null) {
            home = getDefaultHome();
        }

        NuxeoApp app = new NuxeoApp(new File(home));

        if (exec != null) {
            app.setBatchCommand(exec);
        }
        if (remote != null) {
            app.setRemoteServer(remote);
        }
        app.start(h, p, interactive);
        // TODO stop on exit
    }

    protected OSGiAdapter osgi;

    protected URLClassLoader cl;

    protected Set<String> blacklist;

    protected String remoteServerHost;
    protected int remoteServerPort;
    protected String batchCommand;
    
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = NuxeoApp.class.getClassLoader();
        }
        return cl;
    }

    public NuxeoApp() {
        this(new File(getDefaultHome()));
    }

    public NuxeoApp(File home) {
        this(home, getDefaultClassLoader());
    }

    public NuxeoApp(ClassLoader cl) {
        this(new File(getDefaultHome()), cl);
    }

    public NuxeoApp(File home, ClassLoader cl) {
        if (instance != null) {
            throw new IllegalStateException("You already have a nuxeo running");
        }
        if (!(cl instanceof URLClassLoader)) {
            throw new IllegalArgumentException(
                    "Invalid class loader: not supported. Send email to bs@nuxeo.com");
        }
        instance = this;
        this.cl = (URLClassLoader) cl;
        Environment env = new Environment(home);
        Environment.setDefault(env);
        blacklist = new HashSet<String>();
    }

    public void setRemoteServer(String address) {
        int p = address.indexOf(':');
        if (p > -1) {
            remoteServerHost = address.substring(0, p);
            remoteServerPort = Integer.parseInt(address.substring(p+1));
        } else {
            remoteServerHost = address;
        }
    }
    
    public void setBatchCommand(String cmd) {
        //TODO test this
        if (cmd.startsWith("\"") || cmd.startsWith("'")) {
            cmd = cmd.substring(1, cmd.length()-1);
        }
        batchCommand = cmd;
    }
    
    public void blacklistComponent(String component) {
        blacklist.add(component);
    }

    public void start() throws Exception {
        start(null, 0, false);
    }

    public void start(int port) throws Exception {
        start(null, port, false);
    }

    public void start(String host, int port, boolean interactive)
            throws Exception {

        if (osgi != null) {
            throw new IllegalStateException("NuxeoApp is already started");
        }
        setHttpServerAddress(host, port);
        File home = Environment.getDefault().getHome();
        Environment.getDefault().setCommandLineArguments(
                interactive ? new String[] { "-console" } : new String[] {});

        URL[] urls = cl.getURLs();
        cl.loadClass("org.nuxeo.runtime.api.Framework"); // force loading
        URL nxurl = null;
        for (URL url : urls) {
            Matcher m = NUXEO_JAR_PATTERN.matcher(url.getFile());
            if (m.matches()) {
                nxurl = url;
                break;
            }
        }
        if (nxurl == null) {
            throw new IllegalArgumentException(
                    "Cannot find nuxeo on class path");
        }
        File jar = url2file(nxurl);

        initializeHome(jar, home);
        osgi = new OSGiAdapter(home);
        SystemBundle bundle = new SystemBundle(osgi, new JarBundleFile(jar), cl);
        osgi.setSystemBundle(bundle);
        startFramework(bundle.getBundleContext(), home, jar, blacklist);
        osgi.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED,
                bundle, null));

        Runnable task = (Runnable) Environment.getDefault().getProperties().remove(
                "mainTask");
        if (task != null) {
            task.run();
        }
    }

    public void stop() throws Exception {
        if (osgi == null) {
            throw new IllegalStateException("NuxeoApp is not started");
        }
        stopFramework();
        osgi.shutdown();
        osgi = null;
    }

    protected static void initializeHome(File bundleJar, File home)
            throws IOException {
        if (!new File(home, "config").isDirectory()) {
            home.mkdirs();
            ZipUtils.unzip("META-INF/nuxeo/nxserver", bundleJar, home);
            // create the log dir - otherwise jetty will fail to start
            new File(home, "log").mkdir();
        }
    }

    protected static void setHttpServerAddress(String host, int port) {
        if (port > 0) {
            System.setProperty("jetty.port", String.valueOf(port));
        }
        if (host != null) {
            System.setProperty("jetty.host", host);
        }
    }

    /**
     * Must be called to diable the http server. Has effect only when called
     * before starting the application
     */
    public static void disableHttpServer() {

        System.setProperty("jetty.disable", "true");
    }

    protected static void startFramework(BundleContext context, File home,
            File bundleJar, Set<String> blacklist) throws Exception {
        long startTime = System.currentTimeMillis();

        // --------> needed to be able to work in both main and plugin mode
        Environment env = Environment.getDefault();
        if (env == null) {
            env = new Environment(home);
            Environment.setDefault(env);
        }
        initializeHome(bundleJar, home);
        // <------------

        String homePath = home.getAbsolutePath();
        System.setProperty("nuxeo.home", homePath);
        if (System.getProperty("jetty.home") == null) {
            System.setProperty("jetty.home", homePath);
        }
        if (System.getProperty("jetty.logs") == null) {
            System.setProperty("jetty.logs", homePath + "/log");
        }
        if (System.getProperty("derby.system.home") == null) {
            System.setProperty("derby.system.home",
                    Environment.getDefault().getData().getAbsolutePath()
                            + "/derby");
        }

        Bundle bundle = context.getBundle();
        OSGiRuntimeService runtime = new OSGiRuntimeService(context);
        Framework.initialize(runtime);
        if (blacklist != null) {
            ComponentManager cmgr = runtime.getComponentManager();
            Set<String> set = cmgr.getBlacklist();
            if (set == null) {
                cmgr.setBlacklist(blacklist);
            } else {
                set.addAll(blacklist);
            }
        }

        RuntimeContext rc = runtime.createContext(bundle);

        // deploy activators
        URL url = bundle.getEntry("META-INF/nuxeo/activators");
        if (url != null) {
            InputStream in = url.openStream();
            List<String> lines = null;
            try {
                lines = FileUtils.readLines(in);
            } finally {
                in.close();
            }
            for (String line : lines) {
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        Class<?> clazz = (Class<?>) bundle.loadClass(line);
                        log.debug("Found bundle activator: " + line);
                        // TODO wrap the context to be able to handle correctly
                        // framework start event
                        ((BundleActivator) clazz.newInstance()).start(context);
                    } catch (Exception e) {
                        System.err.println("Failed to start activator:" + line);
                        e.printStackTrace();
                    }
                }
            }
        }

        // deploy components
        url = bundle.getEntry("META-INF/nuxeo/components");
        if (url != null) {
            InputStream in = url.openStream();
            List<String> lines = null;
            try {
                lines = FileUtils.readLines(in);
            } finally {
                in.close();
            }
            for (String line : lines) {
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        URL entry = bundle.getEntry(line);
                        if (entry != null) {
                            log.debug("Found component " + line);
                            rc.deploy(entry);
                        } else {
                            log.warn("Component definition not found for "
                                    + line);
                        }
                    } catch (Exception e) {
                        log.error("Failed to start activator:" + line, e);
                    }
                }
            }
        }

        // deploy webengine modules

        File modules = new File(Environment.getDefault().getWeb(), "modules");
        File[] files = modules.listFiles();
        if (files != null) {
            WebEngine we = Framework.getLocalService(WebEngine.class);
            for (File file : files) {
                File cfg = new File(file, "module.xml");
                if (cfg.isFile()) {
                    log.debug("Register web module: " + file.getName());
                    we.registerModule(cfg);
                }
            }
        }

        System.out.println("Framework started in "
                + ((System.currentTimeMillis() - startTime) / 1000) + " sec.");
    }

    protected static void stopFramework() {
        // TODO
    }

    public static File url2file(URL url) {
        File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException e) {
            f = new File(url.getPath());
        }
        return f;
    }

}
