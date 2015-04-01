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

import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * This script factory configure the Nashorn engine with cache (following
 * {@link org.nuxeo.automation.scripting.internals.ScriptingCache#enabled}).
 *
 * @since 7.3
 */
public class ScriptingCache implements ScriptEngineFactory {

    protected final NashornScriptEngineFactory nashornScriptEngineFactory;

    protected final boolean enabled;

    public ScriptingCache(boolean enabled) {
        this.nashornScriptEngineFactory = new NashornScriptEngineFactory();
        this.enabled = enabled;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return enabled ? nashornScriptEngineFactory.getScriptEngine("-strict", "--persistent-code-cache",
                "--class-cache-size=50") : nashornScriptEngineFactory.getScriptEngine();
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
