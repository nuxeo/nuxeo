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
 *
 * $Id$
 */

package org.nuxeo.launcher.config;


/**
 * Constants duplicated from {@link org.nuxeo.commons.Environment}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Environment {

    /**
     * Constants that identifies possible hosts for the framework.
     */
    public static final String JBOSS_HOST = "JBoss";

    // Jetty or GF3 embedded
    public static final String NXSERVER_HOST = "NXServer";

    public static final String TOMCAT_HOST = "Tomcat";

    public static final String NUXEO_HOME_DIR = "nuxeo.home.dir";

    public static final String NUXEO_DATA_DIR = "nuxeo.data.dir";

    public static final String NUXEO_LOG_DIR = "nuxeo.log.dir";

    public static final String NUXEO_TMP_DIR = "nuxeo.tmp.dir";

    public static final String NUXEO_CONFIG_DIR = "nuxeo.config.dir";

    public static final String NUXEO_WEB_DIR = "nuxeo.web.dir";

    public static final String NUXEO_PID_DIR = "nuxeo.pid.dir";

}
