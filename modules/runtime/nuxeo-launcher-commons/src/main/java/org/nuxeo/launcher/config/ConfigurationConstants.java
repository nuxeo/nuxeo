/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.launcher.config;

import java.nio.file.Path;

/**
 * @since 11.5
 */
public final class ConfigurationConstants {

    public static final String ENV_NUXEO_ENVIRONMENT = "NUXEO_ENVIRONMENT";

    public static final String ENV_NUXEO_PROFILES = "NUXEO_PROFILES";

    public static final String FILE_CONFIGURATION_PROPERTIES = "configuration.properties";

    public static final String FILE_DISTRIBUTION_PROPS = "distribution.properties";

    public static final String FILE_NUXEO_CONF = "nuxeo.conf";

    public static final String FILE_NUXEO_DEFAULTS = "nuxeo.defaults";

    public static final Path FILE_TEMPLATE_DISTRIBUTION_PROPS = Path.of("common", "config", FILE_DISTRIBUTION_PROPS);

    public static final String PARAM_BIND_ADDRESS = "nuxeo.bind.address";

    public static final String PARAM_CONTEXT_PATH = "org.nuxeo.ecm.contextPath";

    public static final String PARAM_FORCE_GENERATION = "nuxeo.force.generation";

    public static final String PARAM_NUXEO_CONF = "nuxeo.conf";

    public static final String PARAM_NUXEO_DEV = "org.nuxeo.dev";

    public static final String PARAM_NUXEO_URL = "nuxeo.url";

    public static final String PARAM_HTTP_PORT = "nuxeo.server.http.port";

    public static final String PARAM_HTTP_TOMCAT_ADMIN_PORT = "nuxeo.server.tomcat_admin.port";

    public static final String PARAM_LOOPBACK_URL = "nuxeo.loopback.url";

    /** @since 2021.9 */
    public static final String PARAM_STARTUP_CLEAN_TMP_DIRECTORY = "nuxeo.startup.clean.tmp.dir";

    public static final String PARAM_TEMPLATE_DBTYPE = "nuxeo.db.type";

    public static final String PARAM_TEMPLATES_FREEMARKER_EXTENSIONS = "nuxeo.freemarker_parsing_extensions";

    /**
     * Absolute or relative PATH to the user chosen templates (comma separated list)
     */
    public static final String PARAM_TEMPLATES_NAME = "nuxeo.templates";

    public static final String PARAM_TEMPLATES_PARSING_EXTENSIONS = "nuxeo.plaintext_parsing_extensions";

    public static final String TOMCAT_HOME = "tomcat.home";

    public static final String TOMCAT_STARTUP_CLASS = "org.apache.catalina.startup.Bootstrap";

    private ConfigurationConstants() {
        // empty
    }
}
