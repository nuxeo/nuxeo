/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.servlet;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.tools.jmx.Manager;

public class GuiceContextListener implements ServletContextListener {
    public static final String INJECTOR_ATTRIBUTE = "guice-injector";

    public static final String MODULES_ATTRIBUTE = "guice-modules";

    private boolean jmxInitialized = false;

    private static final Log log = LogFactory.getLog(GuiceContextListener.class);

    public static Injector guiceInjector = null;

    public void contextInitialized(ServletContextEvent event) {
        log.info("GuiceContextListener contextInitialized");
        ServletContext context = event.getServletContext();

        List<Module> modules = getModuleList(context.getInitParameter(MODULES_ATTRIBUTE));
        log.info("GuiceContextListener getModuleList");
        Injector injector = null;
        try {

            log.info("GuiceContextListener createInjector");
            injector = Guice.createInjector(Stage.PRODUCTION, modules);
            OpenSocialService service = Framework.getService(OpenSocialService.class);
            if (service != null) {
                service.setInjector(injector);
            } else {
                guiceInjector = injector;
            }
            context.setAttribute(INJECTOR_ATTRIBUTE, injector);

        } catch (Exception e) {
            log.error("GuiceContextListener caught"
                    + " exception during injection process", e);
            throw new RuntimeException(e);
        }

        try {
            if (!jmxInitialized) {
                Manager.manage("ShindigGuiceContext", injector);
                jmxInitialized = true;
            }
        } catch (Exception e) {
            log.error("GuiceContextListener caught exception "
                    + "trying to init shindig guice context (JMX):", e);
        }
    }

    private Module getModuleInstance(String moduleName)
            throws InstantiationException {
        try {
            return (Module) Class.forName(moduleName).newInstance();
        } catch (IllegalAccessException e) {
            InstantiationException ie = new InstantiationException(
                    "IllegalAccessException: " + e.getMessage());
            ie.setStackTrace(e.getStackTrace());
            throw ie;
        } catch (ClassNotFoundException e) {
            InstantiationException ie = new InstantiationException(
                    "ClassNotFoundException: " + e.getMessage());
            ie.setStackTrace(e.getStackTrace());
            throw ie;
        }
    }

    private List<Module> getModuleList(String moduleNames) {
        List<Module> modules = new LinkedList<Module>();
        if (moduleNames != null) {
            for (String moduleName : moduleNames.split(":")) {
                try {
                    moduleName = moduleName.trim();
                    if (moduleName.length() > 0) {
                        Module moduleInstance = getModuleInstance(moduleName);
                        modules.add(moduleInstance);
                    }
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return modules;
    }

    public void contextDestroyed(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        context.removeAttribute(INJECTOR_ATTRIBUTE);
    }
}
