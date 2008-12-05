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
 *
 * $Id$
 */

package org.nuxeo.osgi.application.loader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Constants {

    public static final String HOME_DIR = "org.nuxeo.app.home";

    // the web root
    public static final String WEB_DIR = "org.nuxeo.app.web";

    // the config dir
    public static final String CONFIG_DIR = "org.nuxeo.app.config";

    // the data dir
    public static final String DATA_DIR = "org.nuxeo.app.data";

    // the log dir
    public static final String LOG_DIR = "org.nuxeo.app.log";

    // the tmp dir
    public static final String TMP_DIR = "org.nuxeo.app.tmp";

    public static final String CLASS_PATH = "org.nuxeo.app.classpath";

    public static final String SYSTEM_BUNDLE = "org.nuxeo.app.system_bundle";

    public static final String BUNDLES = "org.nuxeo.app.bundles";

    public static final String HOST_NAME = "org.nuxeo.app.host";

    public static final String HOST_VERSION = "org.nuxeo.app.host.version";

    public static final String COMMAND_LINE_ARGS = "org.nuxeo.app.args";

    public static final String OPT_SCAN_NESTED_JARS = "org.nuxeo.app.options.scanNestedJARs";

    public static final String OPT_CLEAR_CACHE = "org.nuxeo.app.options.clearCache";

    // Constant utility class.
    private Constants() {
    }

}
