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

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import org.apache.commons.io.IOUtils;
import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.automation.scripting.api.AutomationScriptingException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
public class ScriptRunner {

    protected final ThreadLocal<ScriptEngine> engines = new ThreadLocal<ScriptEngine>(){
        @Override
        protected ScriptEngine initialValue() {
            if (Boolean.valueOf(Framework.getProperty(AutomationScriptingConstants.AUTOMATION_SCRIPTING_PRECOMPILE,
                    AutomationScriptingConstants.DEFAULT_PRECOMPILE_STATUS))) {
                return new NashornScriptEngineFactory().getScriptEngine(AutomationScriptingConstants.NASHORN_OPTIONS);
            } else {
                return new NashornScriptEngineFactory().getScriptEngine();
            }
        }
    };

    public ScriptRunner(String jsBinding) {
        initialize(jsBinding);
    }

    protected void initialize(String jsBinding) {
        try {
            engines.get().eval(jsBinding);
        } catch (ScriptException e) {
            throw new AutomationScriptingException(e);
        }
    }

    public void run(InputStream in, CoreSession session) throws Exception {
        run("(function(){" + IOUtils.toString(in, "UTF-8") + "})();", session);
    }

    public void run(String script, CoreSession session) throws ScriptException {
        engines.get().put(AutomationScriptingConstants.AUTOMATION_MAPPER_KEY,
                new AutomationMapper(session));
        engines.get().eval(script);
    }

    public <T> T getInterface(Class<T> scriptingOperationInterface, String script, CoreSession session) throws Exception {
        run(script, session);
        Invocable inv = (Invocable) engines.get();
        return inv.getInterface(scriptingOperationInterface);
    }

}
