/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.script;

import java.io.IOException;
import java.io.Reader;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Simulates a compiled script for scripts that don't support compilation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FakeCompiledScript extends CompiledScript {

    protected final ScriptEngine engine;

    protected final Script script;

    public FakeCompiledScript(ScriptEngine engine, Script script) {
        this.script = script;
        this.engine = engine;
    }

    @Override
    public Object eval(ScriptContext arg0) throws ScriptException {
        try {
            try (Reader reader = script.getReader()) {
                return engine.eval(reader, arg0);
            }
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }

}
