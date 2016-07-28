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
 *     Stephane Lacoin <slacoin@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.AUTOMATION_SCRIPTING_PRECOMPILE;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CACHE;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CLASS_FILTER;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.DEFAULT_PRECOMPILE_STATUS;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.NASHORN_JAVA_VERSION;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.NASHORN_WARN_CACHE;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.NASHORN_WARN_CLASS_FILTER;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.NASHORN_WARN_VERSION;
import static org.nuxeo.launcher.config.ConfigurationGenerator.checkJavaVersion;

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
        String version = Framework.getProperty("java.version");
        // Check if jdk8
        if (!checkJavaVersion(version, NASHORN_JAVA_VERSION)) {
            log.warn(NASHORN_WARN_VERSION);
            throw new UnsupportedOperationException();
        }
        // Check if version < jdk8u25 -> no cache.
        if (!checkJavaVersion(version, COMPLIANT_JAVA_VERSION_CACHE)) {
            log.warn(NASHORN_WARN_CACHE);
            return new ScriptingCache(false);
        }
        // Check if jdk8u25 <= version < jdk8u40 -> only cache.
        if (!checkJavaVersion(version, COMPLIANT_JAVA_VERSION_CLASS_FILTER)) {
            if (Boolean.parseBoolean(
                    Framework.getProperty(AUTOMATION_SCRIPTING_PRECOMPILE, DEFAULT_PRECOMPILE_STATUS))) {
                log.warn(NASHORN_WARN_CLASS_FILTER);
                return new ScriptingCache(true);
            } else {
                log.warn(NASHORN_WARN_CLASS_FILTER);
                return new ScriptingCache(false);
            }
        }
        // Else if version >= jdk8u40 -> cache + class filter
        try {
            if (Boolean.parseBoolean(
                    Framework.getProperty(AUTOMATION_SCRIPTING_PRECOMPILE, DEFAULT_PRECOMPILE_STATUS))) {
                return new ScriptingCacheClassFilter(true);
            } else {
                return new ScriptingCacheClassFilter(false);
            }
        } catch (NoClassDefFoundError cause) {
            log.warn(NASHORN_WARN_CLASS_FILTER);
            return new ScriptingCache(true);
        }
    }

}
