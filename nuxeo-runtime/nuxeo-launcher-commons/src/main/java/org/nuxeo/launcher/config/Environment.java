/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu, jcarsique
 *
 * $Id$
 */

package org.nuxeo.launcher.config;

/**
 * Constants duplicated from {@link org.nuxeo.common.Environment}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated Since 5.6. Use org.nuxeo.common.Environment
 */
@Deprecated
public class Environment {

    private Environment() {
        // Constants class
    }

    /**
     * Constants that identifies possible hosts for the framework.
     */
    public static final String JBOSS_HOST = "JBoss";

    // Jetty or GF3 embedded
    public static final String NXSERVER_HOST = "NXServer";

    public static final String TOMCAT_HOST = "Tomcat";

    public static final String NUXEO_HOME_DIR = "nuxeo.home.dir";

    public static final String NUXEO_RUNTIME_HOME = "nuxeo.runtime.home";

    public static final String NUXEO_DATA_DIR = "nuxeo.data.dir";

    public static final String NUXEO_LOG_DIR = "nuxeo.log.dir";

    public static final String NUXEO_TMP_DIR = "nuxeo.tmp.dir";

    public static final String NUXEO_CONFIG_DIR = "nuxeo.config.dir";

    public static final String NUXEO_WEB_DIR = "nuxeo.web.dir";

    public static final String NUXEO_PID_DIR = "nuxeo.pid.dir";

    public static final String NUXEO_APP_HOME = "org.nuxeo.app.home";

    public static final String NUXEO_LOOPBACK_URL = "nuxeo.loopback.url";

    // OpenSocial
    public static final String OPENSOCIAL_GADGETS_EMBEDDED_SERVER = "opensocial.gadgets.embeddedServer";

    public static final String OPENSOCIAL_GADGETS_HOST = "opensocial.gadgets.host";

    public static final String OPENSOCIAL_GADGETS_PORT = "opensocial.gadgets.port";

    public static final String OPENSOCIAL_GADGETS_PATH = "opensocial.gadgets.path";

    // proxy
    public static final String NUXEO_HTTP_PROXY_HOST = "nuxeo.http.proxy.host";

    public static final String NUXEO_HTTP_PROXY_PORT = "nuxeo.http.proxy.port";

    public static final String NUXEO_HTTP_PROXY_LOGIN = "nuxeo.http.proxy.login";

    public static final String NUXEO_HTTP_PROXY_PASSWORD = "nuxeo.http.proxy.password";

}
