/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
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

import java.io.File;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Seam;
import org.jboss.seam.core.Init;
import org.jboss.seam.exception.Exceptions;
import org.jboss.seam.init.Initialization;
import org.jboss.seam.navigation.Pages;

/**
 * Helper class to manage Seam Hot Reload
 *
 * Most of the code comes from Jboss Seam 2.0.3-RC1 Debug package (HotDeployFilter)
 */
public class SeamHotReloadHelper {

    private static final Log log = LogFactory.getLog(SeamHotReloadHelper.class);

    public static final String SEAM_HOT_RELOAD_SYSTEM_PROP = "org.nuxeo.seam.debug";


    public static boolean isHotReloadEnabled() {
        String sysProp = System.getProperty(SEAM_HOT_RELOAD_SYSTEM_PROP, "false");
        return "true".equalsIgnoreCase(sysProp);
    }

    public static Set<String> reloadSeamComponents(HttpServletRequest httpRequest) {

        ServletContext servletContext = httpRequest.getSession().getServletContext();

        boolean reloadDone = false;

        Init init = (Init) servletContext.getAttribute(Seam.getComponentName(Init.class));
        if (init != null && init.hasHotDeployableComponents()) {
            for (File file : init.getHotDeployPaths()) {
                if (scan(init, file)) {
                    Seam.clearComponentNameCache();
                    try {
                        new Initialization(servletContext).redeploy(httpRequest);
                    } catch (InterruptedException e) {
                        log.error("Error during hot redeploy", e);
                    }
                    reloadDone=true;
                    break;
                }
            }
        }

        servletContext.removeAttribute(Seam.getComponentName(Pages.class));
        servletContext.removeAttribute(Seam.getComponentName(Exceptions.class));

        if (reloadDone) {
            return init.getHotDeployableComponents();
        } else {
            return null;
        }
    }

    public static Set<String> getHotDeployableComponents(HttpServletRequest httpRequest) {

        ServletContext servletContext = httpRequest.getSession().getServletContext();
        Init init = (Init) servletContext.getAttribute(Seam.getComponentName(Init.class));
        return init.getHotDeployableComponents();

    }

    protected static boolean scan(Init init, File file) {
        if (file.isFile()) {
            if (!file.exists() || file.lastModified() > init.getTimestamp()) {
                if (log.isDebugEnabled()) {
                    log.debug("file updated: " + file.getName());
                }
                return true;
            }
        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (scan(init, f)) {
                    return true;
                }
            }
        }
        return false;
    }

}
