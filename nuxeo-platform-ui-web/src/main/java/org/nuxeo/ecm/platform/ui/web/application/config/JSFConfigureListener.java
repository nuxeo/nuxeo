/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.application.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.config.ConfigureListener;

/**
 * Handles JSF configuration at startup.
 * <p>
 * Delays configuration until runtime is initialized to handle correctly dev
 * mode.
 *
 * @since 5.9.4-JSF2
 */
public class JSFConfigureListener implements ServletContextListener,
        RuntimeServiceListener {

    private static final Log log = LogFactory.getLog(JSFConfigureListener.class);

    protected ConfigureListener confListener;

    protected ServletContextEvent origEvent;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        origEvent = sce;
        confListener = new ConfigureListener();
        confListener.contextInitialized(sce);
        Framework.addListener(this);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        confListener.contextDestroyed(sce);
        Framework.removeListener(this);
    }

    @Override
    public void handleEvent(RuntimeServiceEvent event) {
        if (event.id == RuntimeServiceEvent.RUNTIME_STARTED) {
            // reload app to make sure dev mode is taken into account by
            // facelets cache factory
            reload();
            Framework.removeListener(this);
        }
    }

    protected void reload() {
        if (log.isDebugEnabled()) {
            log.debug("Reloading JSF configuration");
        }
        if (Framework.isDevModeSet() && confListener != null) {
            confListener.contextDestroyed(origEvent);
            confListener.contextInitialized(origEvent);
        }
    }

}