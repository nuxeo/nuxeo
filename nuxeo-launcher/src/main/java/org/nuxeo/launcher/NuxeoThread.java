/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.launcher;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.commons.MutableClassLoader;
import org.nuxeo.launcher.commons.MutableClassLoaderDelegate;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.Environment;

/**
 * Main Nuxeo server thread
 *
 * @author jcarsique
 * @since 5.4.1
 */
public class NuxeoThread extends Thread {
    private static final Log log = LogFactory.getLog(NuxeoThread.class);

    private static final String TOMCAT_STARTUP_CLASS = "org.apache.catalina.startup.Bootstrap";

    private static final String JBOSS_STARTUP_CLASS = "org.jboss.Main";

    private static final String JETTY_STARTUP_CLASS = "org.nuxeo.osgi.application.Main";

    private ConfigurationGenerator configurationGenerator;

    // private MutableClassLoader loader;

    @SuppressWarnings("rawtypes")
    private Class startupClass;

    public NuxeoThread(ConfigurationGenerator configurationGenerator) {
        super("Nuxeo");
        this.configurationGenerator = configurationGenerator;
    }

    @Override
    public void run() {
        if (configurationGenerator.isJBoss) {
            throw new UnsupportedOperationException();
        } else if (configurationGenerator.isJetty) {
            throw new UnsupportedOperationException();
        } else if (configurationGenerator.isTomcat) {
            // setTomcatLogs();
            startTomcat();
        }
    }

    private void startTomcat() {
        MutableClassLoader loader = new MutableClassLoaderDelegate(
                getClass().getClassLoader());
        // setJavaProperties();
        setTomcatClassPath(loader);
        setTomcatSystemProperties();
        setNuxeoSystemProperties();
        configurationGenerator.initLogs();
        try {
            startupClass = loader.getClassLoader().loadClass(
                    TOMCAT_STARTUP_CLASS);
            Object server = startupClass.newInstance();
            @SuppressWarnings("unchecked")
            Method method = startupClass.getMethod("main", String[].class);
            method.invoke(server, new Object[] { new String[] { "start" } });
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find Tomcat startup class", e);
        } catch (InstantiationException e) {
            throw new RuntimeException(
                    "Could not instanciate Tomcat startup class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    // private void setJavaProperties() {
    // String javaOpts = configurationGenerator.getUserConfig().getProperty(
    // "JAVA_OPTS");
    // }

    private void setNuxeoSystemProperties() {
        System.setProperty("nuxeo.home",
                configurationGenerator.getNuxeoHome().getPath());
        System.setProperty("nuxeo.conf",
                configurationGenerator.getNuxeoConf().getPath());
        setNuxeoSystemProperty(Environment.NUXEO_LOG_DIR);
        setNuxeoSystemProperty(Environment.NUXEO_DATA_DIR);
        // setNuxeoSystemProperty(Environment.NUXEO_TMP_DIR);
    }

    private void setNuxeoSystemProperty(String property) {
        log.debug("Set system " + property + ": "
                + configurationGenerator.getUserConfig().getProperty(property));
        System.setProperty(property,
                configurationGenerator.getUserConfig().getProperty(property));
    }

    private void setTomcatSystemProperties() {
        System.setProperty("java.util.logging.manager",
                "org.apache.juli.ClassLoaderLogManager");
        System.setProperty("catalina.base",
                configurationGenerator.getNuxeoHome().getPath());
        System.setProperty("catalina.home",
                configurationGenerator.getNuxeoHome().getPath());
    }

    private void setTomcatClassPath(MutableClassLoader loader) {
        try {
            addToClassPath(loader, "nxserver" + File.separator + "lib");
            addToClassPath(loader, "bin" + File.separator + "bootstrap.jar");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void addToClassPath(MutableClassLoader loader, String filename)
            throws MalformedURLException {
        File classPathEntry = new File(configurationGenerator.getNuxeoHome(),
                filename);
        if (!classPathEntry.exists()) {
            throw new RuntimeException(
                    "Tried to add inexistant classpath entry: "
                            + classPathEntry);
        }
        loader.addURL(classPathEntry.toURI().toURL());
    }

}
