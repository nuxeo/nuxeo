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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import java.io.InputStream;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;
import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @since 7.2
 */
public class ScriptRunner {

    protected final ScriptEngineManager engineManager;

    protected final ScriptEngine engine;

    protected String jsBinding;

    protected CompiledScript compiledJSWrapper;

    protected boolean initDone = false;

    protected CoreSession session;

    /**
     * @return JS binding script loaded in Nashorn.
     */
    public String getJsBinding() {
        return jsBinding;
    }

    public ScriptRunner(ScriptEngineManager engineManager, String jsBinding) {
        this.engineManager = engineManager;
        engine = engineManager.getEngineByName(AutomationScriptingConstants.NASHORN_ENGINE);
        this.jsBinding = jsBinding;

    }

    public ScriptRunner(ScriptEngineManager engineManager, CompiledScript jsBinding) {
        this.engineManager = engineManager;
        engine = engineManager.getEngineByName(AutomationScriptingConstants.NASHORN_ENGINE);
        this.compiledJSWrapper = jsBinding;
    }

    public long initialize() throws ScriptException {
        if (!initDone) {
            long t0 = System.currentTimeMillis();
            if (compiledJSWrapper != null) {
                compiledJSWrapper.eval(engine.getContext());
            } else {
                engine.eval(jsBinding);
            }
            initDone = true;
            return System.currentTimeMillis() - t0;
        } else {
            return 0;
        }
    }

    public void run(InputStream in) throws Exception {
        run(IOUtils.toString(in, "UTF-8"));
    }

    public void run(String script) throws ScriptException {
        initialize();
        engine.put("automation", new AutomationMapper(session));
        StringBuffer nameSpacedJS = new StringBuffer();
        nameSpacedJS.append("(function(){");
        nameSpacedJS.append(script);
        nameSpacedJS.append("})();");
        engine.eval(nameSpacedJS.toString());
    }

    public void setCoreSession(CoreSession session) {
        this.session = session;
    }

    public <T> T getInterface(Class<T> scriptingOperationInterface, String script) throws Exception {
        initialize();
        engine.put(AutomationScriptingConstants.AUTOMATION_MAPPER_KEY, new AutomationMapper(session));
        engine.eval(script);
        Invocable inv = (Invocable) engine;
        return inv.getInterface(scriptingOperationInterface);
    }

    public Invocable getInvocable() {
        return (Invocable) engine;
    }

    public Compilable getCompilable() {
        return (Compilable) engine;
    }

}
