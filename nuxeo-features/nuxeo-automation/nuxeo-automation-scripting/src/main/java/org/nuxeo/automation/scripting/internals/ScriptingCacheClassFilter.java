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

import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * This script factory configure the Nashorn engine with cache (following
 * {@link org.nuxeo.automation.scripting.internals.ScriptingCache#enabled}) and with Class Filter.
 *
 * @since 7.3
 */
public class ScriptingCacheClassFilter implements ScriptEngineFactory {

    protected final NashornScriptEngineFactory nashornScriptEngineFactory;

    protected final boolean cacheEnable;

    public ScriptingCacheClassFilter(boolean cacheEnable) {
        this.nashornScriptEngineFactory = new NashornScriptEngineFactory();
        this.cacheEnable = cacheEnable;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        if (cacheEnable) {
            return nashornScriptEngineFactory.getScriptEngine(new String[] { "-strict", "--persistent-code-cache",
                    "--class-cache-size=50" }, null, new AutomationScriptingClassFilter());
        } else {
            return nashornScriptEngineFactory.getScriptEngine(new AutomationScriptingClassFilter());
        }
    }

    @Override
    public String getProgram(String... statements) {
        return nashornScriptEngineFactory.getProgram(statements);
    }

    @Override
    public Object getParameter(String key) {
        return nashornScriptEngineFactory.getParameter(key);
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return nashornScriptEngineFactory.getOutputStatement(toDisplay);
    }

    @Override
    public List<String> getNames() {
        return nashornScriptEngineFactory.getNames();
    }

    @Override
    public List<String> getMimeTypes() {
        return nashornScriptEngineFactory.getMimeTypes();
    }

    @Override
    public String getMethodCallSyntax(String obj, String method, String... args) {
        return nashornScriptEngineFactory.getMethodCallSyntax(obj, method, args);
    }

    @Override
    public String getLanguageVersion() {
        return nashornScriptEngineFactory.getLanguageVersion();
    }

    @Override
    public String getLanguageName() {
        return nashornScriptEngineFactory.getLanguageName();
    }

    @Override
    public List<String> getExtensions() {
        return nashornScriptEngineFactory.getExtensions();
    }

    @Override
    public String getEngineVersion() {
        return nashornScriptEngineFactory.getEngineVersion();
    }

    @Override
    public String getEngineName() {
        return nashornScriptEngineFactory.getEngineName();
    }
}
