/*
 * (C) Copyright 2006, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
 *     JBoss and Seam dev team
 *     Thierry Delprat
 */
package org.nuxeo.ecm.webapp.seam;

import java.lang.reflect.Field;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Seam;
import org.jboss.seam.core.Init;
import org.jboss.seam.init.Initialization;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ServerLocator;

/**
 * Helper class to manage Seam Hot Reload Most of the code comes from Jboss Seam 2.0.3-RC1 Debug package
 * (HotDeployFilter)
 */
public class SeamHotReloadHelper {

    private static final Log log = LogFactory.getLog(SeamHotReloadHelper.class);

    @Deprecated
    public static final String SEAM_HOT_RELOAD_SYSTEM_PROP = ConfigurationGenerator.SEAM_DEBUG_SYSTEM_PROP;

    public static boolean isHotReloadEnabled() {
        return Framework.isBooleanPropertyTrue(ConfigurationGenerator.SEAM_DEBUG_SYSTEM_PROP);
    }

    public static void flush() {
        Seam.clearComponentNameCache();
        try {
            Field f = Seam.class.getDeclaredField("CLASSLOADERS_LOADED");
            f.setAccessible(true);
            ((Set<?>) f.get(null)).clear();
        } catch (ReflectiveOperationException e) {
            log.warn("Can't flush seam class loader cache", e);
        }
        try {
            // TODO: check if this would be needed for Studio hot reload (?)
            flushWebResources();
        } catch (ReflectiveOperationException | JMException e) {
            log.error("Cannot flush web resources, did you start with the sdk profile active ?", e);
        }
    }

    public static Set<String> reloadSeamComponents(HttpServletRequest httpRequest) {

        ServletContext servletContext = httpRequest.getSession().getServletContext();

        Init init = (Init) servletContext.getAttribute(Seam.getComponentName(Init.class));
        if (init != null && init.hasHotDeployableComponents()) {
            try {
                new Initialization(servletContext).redeploy(httpRequest);
            } catch (InterruptedException e) {
                log.error("Error during hot redeploy", e);
            }
        }

        // re-initialized by re-deployment, why removing ?
        // servletContext.removeAttribute(Seam.getComponentName(Pages.class));
        return init.getHotDeployableComponents();
    }

    public static Set<String> getHotDeployableComponents(HttpServletRequest httpRequest) {
        ServletContext servletContext = httpRequest.getSession().getServletContext();
        Init init = (Init) servletContext.getAttribute(Seam.getComponentName(Init.class));
        return init.getHotDeployableComponents();

    }

    /**
     * Flushes resources for tomcat cache
     *
     * @since 5.5
     */
    protected static void flushWebResources() throws ReflectiveOperationException, JMException {
        ObjectName on = new ObjectName("org.nuxeo:type=sdk,name=web-resources");
        MBeanServer mbs = Framework.getService(ServerLocator.class).lookupServer();
        if (mbs.isRegistered(on)) {
            // only in tomcat container
            mbs.invoke(on, "flushWebResources", new Object[0], new String[0]);
        }
    }

}
