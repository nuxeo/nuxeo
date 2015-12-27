/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.osgi.application;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
