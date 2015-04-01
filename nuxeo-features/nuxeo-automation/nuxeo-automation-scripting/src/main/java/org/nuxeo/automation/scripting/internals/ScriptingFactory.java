/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin <slacoin@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * This factory configures the Nashorn engine and register it following the JVM version.
 *
 * @since 7.3
 */
public class ScriptingFactory {

    private static final Log log = LogFactory.getLog(ScriptingFactory.class);

    protected ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    protected void install() {
        scriptEngineManager.registerEngineName(AutomationScriptingConstants.NX_NASHORN, newFactory());
    }

    protected ScriptEngineFactory newFactory() {
        String version = Framework.getProperty("java.version", "1.8");
        // Check if jdk8
        if (version.contains(AutomationScriptingConstants.NASHORN_JAVA_VERSION)) {
            // Check if version < jdk8u25 -> no cache.
            if (version.compareTo(AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CACHE) < 0) {
                log.warn(AutomationScriptingConstants.NASHORN_WARN_CACHE);
                return new ScriptingCache(false);
                // Check if jdk8u25 <= version < jdk8u40 -> only cache.
            } else if (version.compareTo(AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CACHE) >= 0
                    && version.compareTo(AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CLASS_FILTER) < 0) {
                if (Boolean.valueOf(Framework.getProperty(AutomationScriptingConstants.AUTOMATION_SCRIPTING_PRECOMPILE,
                        AutomationScriptingConstants.DEFAULT_PRECOMPILE_STATUS))) {
                    log.warn(AutomationScriptingConstants.NASHORN_WARN_CLASS_FILTER);
                    return new ScriptingCache(true);
                } else {
                    log.warn(AutomationScriptingConstants.NASHORN_WARN_CLASS_FILTER);
                    return new ScriptingCache(false);
                }
                // Check if version >= jdk8u40 -> cache + class filter
            } else if (version.compareTo(AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CLASS_FILTER) >= 0) {
                try {
                    if (Boolean.valueOf(Framework.getProperty(
                            AutomationScriptingConstants.AUTOMATION_SCRIPTING_PRECOMPILE,
                            AutomationScriptingConstants.DEFAULT_PRECOMPILE_STATUS))) {
                        return new ScriptingCacheClassFilter(true);
                    } else {
                        return new ScriptingCacheClassFilter(false);
                    }
                } catch (NoClassDefFoundError cause) {
                    log.warn(AutomationScriptingConstants.NASHORN_WARN_CLASS_FILTER);
                    return new ScriptingCache(true);
                }
            }
        }
        log.warn(AutomationScriptingConstants.NASHORN_WARN_VERSION);
        throw new UnsupportedOperationException();
    }
}
