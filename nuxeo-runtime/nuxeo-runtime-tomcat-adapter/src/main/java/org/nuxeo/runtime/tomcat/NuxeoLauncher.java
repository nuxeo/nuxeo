/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.tomcat;

import java.io.File;
import java.io.IOException;
import javax.management.JMException;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.util.ServerInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.application.FrameworkBootstrap;
import org.nuxeo.osgi.application.MutableClassLoader;
import org.nuxeo.runtime.tomcat.dev.DevFrameworkBootstrap;
import org.nuxeo.runtime.tomcat.dev.NuxeoDevWebappClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoLauncher implements LifecycleListener {

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
        automaticReload = value;
    }

    public boolean getAutomaticReload() {
        return automaticReload;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        Lifecycle lf = event.getLifecycle();
        if (lf instanceof Context) {
            Loader loader = ((Context) lf).getLoader();
            if (loader instanceof NuxeoWebappLoader) {
                handleEvent((NuxeoWebappLoader) loader, event);
            }
        }
    }

    protected void handleEvent(NuxeoWebappLoader loader, LifecycleEvent event) {
        String type = event.getType();
        try {
            MutableClassLoader cl = (MutableClassLoader) loader.getClassLoader();
            boolean devMode = cl instanceof NuxeoDevWebappClassLoader;
            if (type == Lifecycle.CONFIGURE_START_EVENT) {
                File homeDir = resolveHomeDirectory(loader);
                if (devMode) {
                    bootstrap = new DevFrameworkBootstrap(cl, homeDir);
                    ((NuxeoDevWebappClassLoader) cl).setBootstrap((DevFrameworkBootstrap) bootstrap);
                } else {
                    bootstrap = new FrameworkBootstrap(cl, homeDir);
                }
                bootstrap.setHostName("Tomcat");
                bootstrap.setHostVersion(ServerInfo.getServerNumber());
                bootstrap.initialize();
            } else if (type == Lifecycle.START_EVENT) {
                bootstrap.start(cl);
            } else if (type == Lifecycle.STOP_EVENT) {
                bootstrap.stop(cl);
            }
        } catch (IOException | JMException | ReflectiveOperationException e) {
            log.error("Failed to handle event: " + type, e);
        }
    }

    protected File resolveHomeDirectory(NuxeoWebappLoader loader) {
        String path = null;
        if (home.startsWith("/") || home.startsWith("\\") || home.contains(":/") || home.contains(":\\")) {
            // absolute
            path = home;
        } else if (home.startsWith("${catalina.base}")) {
            path = getTomcatHome() + home.substring("${catalina.base}".length());
        } else {
            try {
                File baseDir = loader.getBaseDir();
                return new File(baseDir, home);
            } catch (ReflectiveOperationException e) {
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
