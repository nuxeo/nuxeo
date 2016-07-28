/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
