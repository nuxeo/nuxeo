/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.runtime.tomcat;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.core.ContainerBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.application.FrameworkBootstrap;
import org.nuxeo.osgi.application.MutableClassLoader;
import org.nuxeo.runtime.tomcat.dev.DevFrameworkBootstrap;
import org.nuxeo.runtime.tomcat.dev.NuxeoDevWebappClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class NuxeoLauncher implements LifecycleListener {

    public static final String DEV_BUNDLES_NAME = "org.nuxeo:type=sdk,name=dev-bundles";

    public static final String WEB_RESOURCES_NAME = "org.nuxeo:type=sdk,name=web-resources";

    static final Log log = LogFactory.getLog(NuxeoLauncher.class);

    protected boolean shared; // TODO

    protected String home = "nxserver";

    protected boolean automaticReload = true;

    protected FrameworkBootstrap bootstrap;

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public boolean isShared() {
        return shared;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public String getHome() {
        return home;
    }

    public void setAutomaticReload(boolean value) {
        this.automaticReload = value;
    }

    public boolean getAutomaticReload() {
        return this.automaticReload;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        Lifecycle lf = event.getLifecycle();
        if (lf instanceof ContainerBase) {
            Loader loader = ((Container) lf).getLoader();
            if (loader instanceof NuxeoWebappLoader) {
                handleEvent((NuxeoWebappLoader) loader, event);
            }
        }
    }

    protected void handleEvent(NuxeoWebappLoader loader, LifecycleEvent event) {
        try {
            MutableClassLoader cl = (MutableClassLoader)loader.getClassLoader();
            boolean devMode = cl instanceof NuxeoDevWebappClassLoader;
            String type = event.getType();
            if (type == Lifecycle.START_EVENT) {
                File homeDir = resolveHomeDirectory(loader);
                if (devMode) {
                    bootstrap = new DevFrameworkBootstrap(
                            cl,
                            homeDir);
                    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                    server.registerMBean(bootstrap, new ObjectName(DEV_BUNDLES_NAME));
                    server.registerMBean(cl, new ObjectName(WEB_RESOURCES_NAME));
                    ((NuxeoDevWebappClassLoader) cl).setBootstrap((DevFrameworkBootstrap)bootstrap);
                } else {
                    bootstrap = new FrameworkBootstrap(
                            cl,
                            homeDir);
                }
                bootstrap.setHostName("Tomcat");
                bootstrap.setHostVersion("6.0.20");
                bootstrap.initialize();
            } else if (type == Lifecycle.AFTER_START_EVENT) {
                bootstrap.start();
            } else if (type == Lifecycle.STOP_EVENT) {
                bootstrap.stop();
                if (devMode) {
                    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                    server.unregisterMBean(new ObjectName(DEV_BUNDLES_NAME));
                    server.unregisterMBean(new ObjectName(WEB_RESOURCES_NAME));                   
                }
            }
        } catch (Throwable e) {
            log.error("Failed to handle event", e);
        }
    }

    protected File resolveHomeDirectory(NuxeoWebappLoader loader) {
        String path = null;
        if (home.startsWith("/") || home.startsWith("\\")
                || home.contains(":/") || home.contains(":\\")) {
            // absolute
            path = home;
        } else if (home.startsWith("${catalina.base}")) {
            path = getTomcatHome()
                    + home.substring("${catalina.base}".length());
        } else {
            try {
                File baseDir = loader.getBaseDir();
                return new File(baseDir, home);
            } catch (Throwable t) {
                return null;
            }
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
