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
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.TomcatConfigurator;

/**
 * Main Nuxeo server thread
 *
 * @author jcarsique
 * @since 5.4.1
 */
public class NuxeoTomcatThread extends NuxeoThread {

    /**
     * @param configurationGenerator
     */
    public NuxeoTomcatThread(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    static final Log log = LogFactory.getLog(NuxeoTomcatThread.class);

    private static final String JBOSS_STARTUP_CLASS = "org.jboss.Main";

    private static final String JETTY_STARTUP_CLASS = "org.nuxeo.osgi.application.Main";

    protected void startServer(MutableClassLoader loader)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, SecurityException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {
        startupClass = loader.loadClass(TomcatConfigurator.STARTUP_CLASS);
        Object server = startupClass.newInstance();
        @SuppressWarnings("unchecked")
        Method method = startupClass.getMethod("main", String[].class);
        method.invoke(server, new Object[] { new String[] { "start" } });
    }

    protected void setSystemProperties() {
        System.setProperty("java.util.logging.manager",
                "org.apache.juli.ClassLoaderLogManager");
        System.setProperty("catalina.base",
                configurationGenerator.getNuxeoHome().getPath());
        System.setProperty("catalina.home",
                configurationGenerator.getNuxeoHome().getPath());
    }

    protected void setClassPath(MutableClassLoader loader) {
        try {
            addToClassPath(loader, "nxserver" + File.separator + "lib");
            addToClassPath(loader, "bin" + File.separator + "bootstrap.jar");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
