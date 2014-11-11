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
package org.nuxeo.runtime.tomcat;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.ContainerBase;
import org.nuxeo.osgi.application.FrameworkBootstrap;

import sun.misc.ClassLoaderUtil;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoDeployer implements LifecycleListener {

    protected String home = "nxserver";

    protected FrameworkBootstrap bootstrap;

    public void setHome(String home) {
        this.home = home;
    }

    public String getHome() {
        return home;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        Lifecycle lf = event.getLifecycle();
        if (lf instanceof ContainerBase) {
            ContainerBase container = (ContainerBase) lf;
            handleEvent(container, event);
        }
    }

    protected void handleEvent(ContainerBase container, LifecycleEvent event) {
        try {
            ClassLoader parentCl = container.getParentClassLoader();
            String type = event.getType();
            if (type == Lifecycle.BEFORE_START_EVENT) {
                File homeDir = resolveHomeDirectory();
                File bundles = new File(homeDir, "bundles");
                File lib = new File(homeDir, "lib");
                File deployerJar = FrameworkBootstrap.findFileStartingWidth(
                        bundles, "nuxeo-runtime-deploy");
                File commonJar = FrameworkBootstrap.findFileStartingWidth(
                        bundles, "nuxeo-common");
                // File xercesJar =
                // FrameworkBootstrap.findFileStartingWidth(lib, "xerces");
                if (deployerJar == null || commonJar == null) {
                    System.out.println("Deployer and/or common JAR (nuxeo-runtime-deploy* | nuxeo-common*) not found in "
                            + bundles);
                    return;
                }
                ArrayList<URL> urls = new ArrayList<URL>();
                File[] files = lib.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getPath().endsWith(".jar")) {
                            urls.add(f.toURI().toURL());
                        }
                    }
                }
                files = bundles.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getPath().endsWith(".jar")) {
                            urls.add(f.toURI().toURL());
                        }
                    }
                }
                urls.add(homeDir.toURI().toURL());
                urls.add(new File(homeDir, "config").toURI().toURL());
                URLClassLoader cl = new URLClassLoader(
                        urls.toArray(new URL[urls.size()]), parentCl);
                // URLClassLoader cl = new URLClassLoader(new URL[]
                // {deployerJar.toURI().toURL(),
                // commonJar.toURI().toURL(),
                // xercesJar.toURI().toURL(),
                // new File(homeDir, "config").toURI().toURL() // for log4j
                // config
                // }, parentCl);
                System.out.println("# Running Nuxeo Preprocessor ...");
                Class<?> klass = cl.loadClass("org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor");
                Method main = klass.getMethod("main", String[].class);
                main.invoke(
                        null,
                        new Object[] { new String[] { homeDir.getAbsolutePath() } });
                System.out.println("# Preprocessing done.");
                ClassLoaderUtil.releaseLoader(cl);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle event", e);
        }
    }

    protected File resolveHomeDirectory() {
        String path;
        if (home.startsWith("/")) {
            path = home;
        } else {
            path = getTomcatHome() + "/" + home;
        }
        return new File(path);
    }

    public String getTomcatHome() {
        String tomcatHome = System.getProperty("catalina.base");
        if (tomcatHome == null) {
            tomcatHome = System.getProperty("catalina.home");
        }
        return tomcatHome;
    }

}
