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

    String HOST_NAME = "org.nuxeo.app.host.name";

    String HOST_VERSION = "org.nuxeo.app.host.version";

    String HOME_DIR = "org.nuxeo.app.home";

    String LOG_DIR = "org.nuxeo.app.log";

    String DATA_DIR = "org.nuxeo.app.data";

    String TMP_DIR = "org.nuxeo.app.tmp";

    String WEB_DIR = "org.nuxeo.app.web";

    String CONFIG_DIR = "org.nuxeo.app.config";

    String LIBS = "org.nuxeo.app.libs"; // class path

    String BUNDLES = "org.nuxeo.app.bundles"; // class path

    String DEVMODE = "org.nuxeo.app.devmode";

    String PREPROCESSING = "org.nuxeo.app.preprocessing";

    String SCAN_FOR_NESTED_JARS = "org.nuxeo.app.scanForNestedJars";

    String INSTALL_RELOAD_TIMER = "org.nuxeo.app.installReloadTimer";

    String FLUSH_CACHE = "org.nuxeo.app.flushCache";

    String ARGS = "org.nuxeo.app.args";

}
