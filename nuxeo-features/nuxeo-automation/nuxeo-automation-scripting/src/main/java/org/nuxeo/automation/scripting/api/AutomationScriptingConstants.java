/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.api;

/**
 * @since 7.2
 */
public class AutomationScriptingConstants {

    public static final String NASHORN_ENGINE = "Nashorn";

    public static final String AUTOMATION_MAPPER_KEY = "automation";

    public static final String AUTOMATION_CTX_KEY = "ctx";

    public static final String AUTOMATION_SCRIPTING_MONITOR = "automation.scripting.monitor.enable";

    public static final String AUTOMATION_SCRIPTING_PRECOMPILE = "automation.scripting.precompile.enable";

    public static final String DEFAULT_PRECOMPILE_STATUS = "true";

    public static final String XP_OPERATION = "operation";

    public static final String NX_NASHORN = "nx-nashorn";

    public static final String NASHORN_JAVA_VERSION = "1.8.0_1";

    public static final String COMPLIANT_JAVA_VERSION_CACHE = "1.8.0_25";

    public static final String COMPLIANT_JAVA_VERSION_CLASS_FILTER = "1.8.0_40";

    public static final String NASHORN_WARN_CLASS_FILTER = "Class Filter is not available. jdk8u40 is required to activate Automation Javascript imports security.";

    public static final String NASHORN_WARN_CACHE = "Nashorn cache is not available. jdk8u25 is required to optimize Automation Javascript performances.";

    public static final String NASHORN_WARN_VERSION = "Cannot use Nashorn. jdk8 is required to activate Automation Javascript.";

}
