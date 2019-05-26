/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Delays configuration until runtime is initialized to handle correctly dev mode.
 *
 * @since 6.0
 */
public class JSFConfigureListener implements ServletContextListener, RuntimeServiceListener {

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
