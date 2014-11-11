/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.webapp.seam;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Listener for Seam cache flush requests
 *
 * @since 5.5
 */
public class NuxeoSeamWebGate implements ServletContextListener {

    protected static NuxeoSeamWebGate instance;

    protected boolean initialized;

    public NuxeoSeamWebGate() {
        instance = this;
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        initialized = false;
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        initialized = true;
    }

    public static boolean isInitialized() {
        return instance != null && instance.initialized;
    }

}
