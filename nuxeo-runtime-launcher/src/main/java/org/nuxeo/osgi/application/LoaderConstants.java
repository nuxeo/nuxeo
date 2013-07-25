/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.osgi.application;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface LoaderConstants {

    static final String HOST_NAME = "org.nuxeo.app.host.name";
    static final String HOST_VERSION = "org.nuxeo.app.host.version";
    static final String HOME_DIR = "org.nuxeo.app.home";
    static final String LOG_DIR = "org.nuxeo.app.log";
    static final String DATA_DIR = "org.nuxeo.app.data";
    static final String TMP_DIR = "org.nuxeo.app.tmp";
    static final String WEB_DIR = "org.nuxeo.app.web";
    static final String CONFIG_DIR = "org.nuxeo.app.config";
    static final String LIBS = "org.nuxeo.app.libs"; // class path
    static final String BUNDLES = "org.nuxeo.app.bundles"; // class path
    static final String DEVMODE = "org.nuxeo.app.devmode";
    static final String PREPROCESSING = "org.nuxeo.app.preprocessing";
    static final String SCAN_FOR_NESTED_JARS = "org.nuxeo.app.scanForNestedJars";
    static final String INSTALL_RELOAD_TIMER = "org.nuxeo.app.installReloadTimer";
    static final String FLUSH_CACHE = "org.nuxeo.app.flushCache";
    static final String ARGS = "org.nuxeo.app.args";

}
