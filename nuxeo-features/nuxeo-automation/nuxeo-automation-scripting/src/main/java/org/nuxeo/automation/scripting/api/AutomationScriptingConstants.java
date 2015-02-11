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

    public static final String AUTOMATION_SCRIPTING_MONITOR = "automation.scripting.monitor.enable";

    public static final String AUTOMATION_SCRIPTING_PRECOMPILE = "automation.scripting.precompile.enable";

    public static final String DEFAULT_PRECOMPILE_STATUS = "true";

    public static final String[] NASHORN_OPTIONS = new String[] { "-strict", "--persistent-code-cache",
            "--class-cache-size=50" };
}
