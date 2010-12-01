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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.runtime.tomcat;

import java.io.File;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.ContainerBase;
import org.nuxeo.osgi.application.FrameworkBootstrap;
import org.nuxeo.osgi.application.loader.FrameworkLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoLauncher2 implements LifecycleListener {

    protected String home = "nxserver";

    protected FrameworkBootstrap bootstrap;

    public static final String NUXEO_DATA_DIR = "nuxeo.data.dir";

    public static final String NUXEO_LOG_DIR = "nuxeo.log.dir";

    public static final String NUXEO_TMP_DIR = "nuxeo.tmp.dir";

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
            ContainerBase container = (ContainerBase)lf;
            handleEvent(container, event);
        }
    }

    protected void handleEvent(ContainerBase container, LifecycleEvent event) {
        try {
            ClassLoader cl = container.getParentClassLoader();
            String type = event.getType();
            if (type == Lifecycle.BEFORE_START_EVENT) {

                File homeDir = resolveHomeDirectory();
                bootstrap = new FrameworkBootstrap(cl, homeDir);
                bootstrap.env().put(FrameworkLoader.DATA_DIR, System.getProperty(NUXEO_DATA_DIR));
                bootstrap.env().put(FrameworkLoader.LOG_DIR, System.getProperty(NUXEO_LOG_DIR));
                bootstrap.env().put(FrameworkLoader.TMP_DIR, System.getProperty(NUXEO_TMP_DIR));
                bootstrap.setHostName("Tomcat");
                bootstrap.setHostVersion("6.0.20");
                bootstrap.initialize();
                bootstrap.start();
            } else if (type == Lifecycle.STOP_EVENT) {
                bootstrap.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle event", e);
        }
    }

    protected File resolveHomeDirectory() {
        String path = null;
        if (home.startsWith("/")) {
            path = home;
        } else {
            path = getTomcatHome()+"/"+home;
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
